package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import utils.extend
import chisel3._
import chisel3.util.{log2Ceil, Counter, DecoupledIO, Fill, Mux1H, RegEnable, ValidIO}

import scala.math.ceil

/** SRT4
  * 1/2 <= d < 1, 1/2 < rho <=1, 0 < q  < 2
  * radix = 4
  * a = 2, {-2, -1, 0, 1, -2},
  * t = 4
  * y^（xxx.xxxx）, d^（0.1xxx）
  * -44/16 < y^ < 42/16
  */

class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W) //.1**********
  val divider = UInt(dividerWidth.W) //.1**********
  val counter = UInt(log2Ceil(n).W) //the width of quotient.
}

class SRTOutput(reminderWidth: Int, quotientWidth: Int) extends Bundle {
  val reminder = UInt(reminderWidth.W)
  val quotient = UInt(quotientWidth.W)
}

// only SRT4 currently
class SRT(
  dividendWidth:  Int,
  dividerWidth:   Int,
  n:              Int, // the longest width
  radixLog2:      Int = 2,
  a:              Int = 2,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {

  val xLen:    Int = dividendWidth + radixLog2 + 1
  val wLen:    Int = xLen + radixLog2
  val ohWidth: Int = 2 * a + 1

  // IO
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))

  val partialReminderCarryNext = Wire(UInt(wLen.W))
  val partialReminderSumNext = Wire(UInt(wLen.W))
  val dividerNext = Wire(UInt(dividerWidth.W))
  val counterNext = Wire(UInt(n.W))
  val quotientNext = Wire(UInt(n.W))
  val quotientMinusOneNext = Wire(UInt(log2Ceil(n).W))

  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())
  // sign of Cycle, true -> (counter === 0.U)
  val isLastCycle: Bool = Wire(Bool())

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = RegEnable(partialReminderCarryNext, 0.U(wLen.W), input.fire || !isLastCycle)
  val partialReminderSum = RegEnable(partialReminderSumNext, 0.U(wLen.W), input.fire || !isLastCycle)
  val divider = RegEnable(dividerNext, 0.U(dividerWidth.W), input.fire || !isLastCycle)
  val quotient = RegEnable(quotientNext, 0.U(n.W), input.fire || !isLastCycle)
  val quotientMinusOne = RegEnable(quotientMinusOneNext, 0.U(n.W), input.fire || !isLastCycle)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(n).W), input.fire || !isLastCycle)

  //  Datapath
  //  according two adders
  isLastCycle := !counter.orR
  output.valid := isLastCycle
  input.ready := isLastCycle

  // only mux is in last Cycle, adder is in every Cycle
  val remainderNoCorrect: UInt = partialReminderSum(wLen - 3, radixLog2) + partialReminderCarry(wLen - 3, radixLog2)
  val remainderCorrect: UInt =
    partialReminderSum(wLen - 3, radixLog2) + partialReminderCarry(wLen - 3, radixLog2) + divider
  val needCorrect: Bool = remainderNoCorrect.head(1).asBool
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)
  output.bits.quotient := quotient - needCorrect.asUInt

  // qds
  val rWidth: Int = 1 + radixLog2 + rTruncateWidth
  val qds = Module(new QDS(rWidth, ohWidth, dTruncateWidth - 1))
  qds.input.partialReminderSum := (partialReminderSum << radixLog2)(wLen - 1, wLen - rWidth)
  qds.input.partialReminderCarry := (partialReminderCarry << radixLog2)(wLen - 1, wLen - rWidth)
  qds.partialDivider.valid := input.fire
  qds.partialDivider.bits := input.bits.divider
    .head(dTruncateWidth)(dTruncateWidth - 1, 0) //.1********** -> .1*** -> ***
  qdsSign := qds.output.selectedQuotientOH(ohWidth - 1, ohWidth / 2).orR

  // for SRT4 -> CSA32
  // for SRT8 -> CSA32+CSA32
  // for SRT16 -> CSA53+CSA32
  // SRT16 <- SRT4 + SRT4*5
  val csa = Module(new CarrySaveAdder(CSACompressor3_2, xLen))
  csa.in(0) := (partialReminderSum << radixLog2)(wLen - 1, radixLog2)
  csa.in(1) := (partialReminderCarry << radixLog2)(wLen - 1, radixLog2 + 1) ## qdsSign
  csa.in(2) :=
    Mux1H(
      qds.output.selectedQuotientOH,
      //this is for SRT4, for SRT8 or SRT16, this should be changed
      VecInit((-2 to 2).map {
        case -2 => divider << 1
        case -1 => divider
        case 0  => 0.U
        case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider
        case 2  => Fill(radixLog2, 1.U(1.W)) ## (~divider << 1)
      })
    )

  // On-The-Fly conversion
  val otf = Module(new OTF(1 << radixLog2, n, ohWidth))
  otf.input.quotient := quotient
  otf.input.quotientMinusOne := quotientMinusOne
  otf.input.selectedQuotientOH := qds.output.selectedQuotientOH

  dividerNext := Mux(input.fire, input.bits.divider, divider)
  counterNext := Mux(input.fire, input.bits.counter, counter - 1.U)

  quotientNext := Mux(input.fire, 0.U, otf.output.quotient)
  quotientMinusOneNext := Mux(input.fire, 0.U, otf.output.quotientMinusOne)

  partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa.out(1) << radixLog2)
  partialReminderCarryNext := Mux(input.fire, 0.U, csa.out(0) << 1 + radixLog2)
}
