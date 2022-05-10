package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import utils.{extend}
import chisel3._
import chisel3.util.{log2Ceil, Counter, DecoupledIO, Fill, Mux1H, ValidIO}

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
  val ohWidth: Int = 2 * a + 1

  // IO
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = Reg(UInt(xLen.W))
  val partialReminderSum = Reg(UInt(xLen.W))
  val divider = Reg(UInt(dividerWidth.W))
  val quotient = Reg(UInt(n.W))
  val quotientMinusOne = Reg(UInt(n.W))
  val counter = RegInit(0.U(log2Ceil(n).W))

  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())

  //  Datapath
  //  according two adders
  val isLastCycle: Bool = !counter.orR
  output.valid := isLastCycle
  input.ready := isLastCycle

  // lastCycle-> correct-> output
  // only mux is in lastCycle, adder is not inlastCycle
  val remainderNoCorrect: UInt = partialReminderSum(xLen - 3, 0) + partialReminderCarry(xLen - 3, 0)
  val remainderCorrect:   UInt = partialReminderSum(xLen - 3, 0) + partialReminderCarry(xLen - 3, 0) + divider
  val needCorrect:        Bool = remainderNoCorrect.head(1).asBool
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)
  output.bits.quotient := quotient - needCorrect.asUInt

  // qds
  val rWidth: Int = 1 + radixLog2 + rTruncateWidth
  val qds = Module(new QDS(rWidth, ohWidth, dTruncateWidth - 1))
  qds.input.partialReminderSum := partialReminderSum.head(rWidth)
  qds.input.partialReminderCarry := partialReminderCarry.head(rWidth)
  qds.partialDivider.valid := input.valid && input.ready
  qds.partialDivider.bits := input.bits.divider
    .head(dTruncateWidth)(dTruncateWidth - 1, 0) //.1********** -> .1*** -> ***
  qdsSign := qds.output.selectedQuotientOH(ohWidth - 1, ohWidth / 2).orR

  // for SRT4 -> CSA32
  // for SRT8 -> CSA32+CSA32
  // for SRT16 -> CSA53+CSA32
  // SRT16 <- SRT4 + SRT4*5
  val csa = Module(new CarrySaveAdder(CSACompressor3_2, xLen))
  csa.in(0) := partialReminderSum
  csa.in(1) := (partialReminderCarry(xLen - 1, 1) ## qdsSign)
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

  divider := Mux(input.valid && input.ready, input.bits.divider, divider)
  counter := Mux(input.valid && input.ready, input.bits.counter, counter - 1.U)

  quotient := Mux(isLastCycle, 0.U, otf.output.quotient)
  quotientMinusOne := Mux(isLastCycle, 0.U, otf.output.quotientMinusOne)
//  //shiftleft before csa
//  partialReminderSum := Mux(isLastCycle, input.bits.dividend >> radixLog2, csa.out(1))
//  partialReminderCarry := Mux(isLastCycle, 0.U, csa.out(0) << 1)
//  val csa = Module(new CarrySaveAdder(CSACompressor3_2, xLen))
//  //csa.in(0) := Mux(counter === input.bits.counter, input.bits.dividend, partialReminderSum )
//  csa.in(0) := partialReminderSum << radixLog2
//  csa.in(1) := ((partialReminderCarry << radixLog2)(xLen - 1, 1) ## qdsSign)
//  csa.in(2) :=
//    Mux1H(
//      qds.output.selectedQuotientOH,
//      //this is for SRT4, for SRT8 or SRT16, this should be changed
//      VecInit((-2 to 2).map {
//        case -2 => divider << 1
//        case -1 => divider
//        case 0  => 0.U
//        case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider
//        case 2  => Fill(radixLog2, 1.U(1.W)) ## (~divider << 1)
//      })
//    )

  partialReminderSum := Mux1H(
    Map(
      isLastCycle -> input.bits.dividend,
      (counter > 1.U) -> (csa.out(1) << radixLog2)(xLen - 1, 0),
      (counter === 1.U) -> csa.out(1)(xLen - 1, 0)
    )
  )
  partialReminderCarry := Mux1H(
    Map(
      isLastCycle -> 0.U,
      (counter > 1.U) -> (csa.out(0) << radixLog2 + 1)(xLen - 1, 0),
      (counter === 1.U) -> (csa.out(0) << 1)(xLen - 1, 0)
    )
  )
}
