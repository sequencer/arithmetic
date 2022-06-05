package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import chisel3._
import chisel3.util.{log2Ceil, DecoupledIO, Fill, Mux1H, RegEnable, ValidIO}
import utils.leftShift

/** SRT4
  * 1/2 <= d < 1, 1/2 < rho <=1, 0 < q  < 2
  * radix = 4
  * a = 2, {-2, -1, 0, 1, -2},
  * t = 4
  * y^（xxx.xxxx）, d^（0.1xxx）
  * -44/16 < y^ < 42/16
  */

class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W) //.***********
  val divider = UInt(dividerWidth.W) //.1**********
  val counter = UInt(log2Ceil(n).W) //the width of quotient.
}

class SRTOutput(reminderWidth: Int, quotientWidth: Int) extends Bundle {
  val reminder = UInt(reminderWidth.W)
  val quotient = UInt(quotientWidth.W)
}

// only SRT4 currently
class SRT4(
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

  val partialReminderCarryNext, partialReminderSumNext = Wire(UInt(wLen.W))
  val quotientNext, quotientMinusOneNext = Wire(UInt(n.W))
  val dividerNext = Wire(UInt(dividerWidth.W))
  val counterNext = Wire(UInt(log2Ceil(n).W))

  // Control
  // sign of select quotient, true -> negative, false -> positive
  // sign of Cycle, true -> (counter === 0.U)
  val qdsSign, isLastCycle, enable: Bool = Wire(Bool())

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = RegEnable(partialReminderCarryNext, 0.U(wLen.W), enable)
  val partialReminderSum = RegEnable(partialReminderSumNext, 0.U(wLen.W), enable)
  val divider = RegEnable(dividerNext, 0.U(dividerWidth.W), enable)
  val quotient = RegEnable(quotientNext, 0.U(n.W), enable)
  val quotientMinusOne = RegEnable(quotientMinusOneNext, 0.U(n.W), enable)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(n).W), enable)

  //  Datapath
  //  according two adders
  isLastCycle := !counter.orR
  output.valid := isLastCycle
  input.ready := isLastCycle
  enable := input.fire || !isLastCycle

  val remainderNoCorrect: UInt = partialReminderSum + partialReminderCarry
  val remainderCorrect: UInt =
    partialReminderSum + partialReminderCarry + (divider << 2)
  val needCorrect: Bool = remainderNoCorrect(wLen - 3).asBool
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)(wLen - 4, radixLog2)
  output.bits.quotient := Mux(needCorrect, quotientMinusOne, quotient)

  // qds
  val rWidth: Int = 1 + radixLog2 + rTruncateWidth
  val selectedQuotientOH: UInt =
    QDS(rWidth, ohWidth, dTruncateWidth - 1)(
      leftShift(partialReminderSum, radixLog2).head(rWidth),
      leftShift(partialReminderCarry, radixLog2).head(rWidth),
      dividerNext.head(dTruncateWidth)(dTruncateWidth - 2, 0) //.1********* -> 1*** -> ***
    )
  qdsSign := selectedQuotientOH(ohWidth - 1, ohWidth / 2 + 1).orR

  // csa for SRT4 -> CSA32, SRT8 -> CSA32+CSA32, SRT16 -> CSA53+CSA32, SRT16 <- SRT4 + SRT4*5
  val csa = Module(new CarrySaveAdder(CSACompressor3_2, xLen))
  csa.in(0) := leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2)
  csa.in(1) := leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qdsSign
  csa.in(2) :=
    Mux1H(
      selectedQuotientOH,
      //this is for SRT4, for SRT8 or SRT16, this should be changed
      VecInit((-2 to 2).map {
        case -2 => divider << 1
        case -1 => divider
        case 0  => 0.U
        case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider
        case 2  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1)
      })
    )

  // On-The-Fly conversion
  val otf = OTF(radixLog2, n, ohWidth)(quotient, quotientMinusOne, selectedQuotientOH)

  dividerNext := Mux(input.fire, input.bits.divider, divider)
  counterNext := Mux(input.fire, input.bits.counter, counter - 1.U)
  quotientNext := Mux(input.fire, 0.U, otf(0))
  quotientMinusOneNext := Mux(input.fire, 0.U, otf(1))
  partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa.out(1) << radixLog2)
  partialReminderCarryNext := Mux(input.fire, 0.U, csa.out(0) << 1 + radixLog2)
}
