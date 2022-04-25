package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import utils.extend
import chisel3._
import chisel3.util.{log2Ceil, Counter, DecoupledIO, Mux1H, ValidIO}

import scala.math.ceil

/** SRT4
  * 1/2<= d < 1, 1/2 < rho <=1, 0 < q  < 2
  * 0, radix = 4
  * a = 2, {-2, -1, 0, 1, -2},
  * t = 4
  * y^（xxx.xxxx）, d^（0.1xxx）
  * -44/16 < y^ < 42/16
  */

// TODO: counter & n
class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W) //0.1**********
  val divider = UInt(dividerWidth.W) //0.1**********
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
  n:              Int, // the longest width,
  radixLog2:      Int = 2,
  a:              Int = 2,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {

  val xLen:    Int = dividendWidth + radixLog2
  val ohWidth: Int = 2 * a + 1

  // IO
  val input = Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n)))
  val output = ValidIO(new SRTOutput(dividerWidth, dividendWidth))

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = Reg(UInt(xLen.W))
  val partialReminderSum = Reg(UInt(xLen.W))
  val divider = RegInit(input.bits.divider)
  val quotient = Reg(UInt(n.W))
  val quotientMinusOne = Reg(UInt(n.W))
  val counter = RegInit(input.bits.counter)
  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())
  qdsSign := qds.output.selectedQuotientOH(ohWidth - 1, ohWidth / 2).orR

  // Datapath
  val rWidth: Int = 1 + radixLog2 + rTruncateWidth
  val qds = Module(new QDS(rWidth, ohWidth, dTruncateWidth - 1))
  qds.input.partialReminderSum := partialReminderSum.head(rWidth)
  qds.input.partialReminderCarry := partialReminderCarry.head(rWidth)
  qds.partialDivider.bits := input.bits.divider
    .head(dTruncateWidth + 1)(dTruncateWidth - 2, 0) //0.1********** -> 0.1*** -> ***

  counter := counter - 1.U
  // if counter === 0.U && sz.output.sign, correct the quotient and reminder. valid = 1
  // the output of srt
//  val sz = Module(new SZ(dividendWidth - 2))
//  sz.input.partialReminderSum := partialReminderSum(partialReminderSum.getWidth-3, 0)
//  sz.input.partialReminderCarry := partialReminderCarry(partialReminderSum.getWidth-3, 0)
//  // correcting maybe have problem
//  quotient             := quotient - Mux(sz.output.sign, 1.U, 0.U)
//  output.bits.reminder := sz.output.remainder + Mux(sz.output.sign, divider, 0.U)
//  output.bits.quotient := quotient

  //  according two adders
  val isLastCycle: Bool = !counter.orR
  output.valid := Mux(isLastCycle, true.B, false.B)
  val remainderNoCorrect: UInt = partialReminderSum(xLen - 3, 0) + partialReminderCarry(xLen - 3, 0)
  val needCorrect:        Bool = Mux(isLastCycle, remainderNoCorrect.head(1).asBool, false.B)
  val remainderCorrect:   UInt = partialReminderSum(xLen - 3, 0) + partialReminderCarry(xLen - 3, 0) + divider

  quotient := quotient - needCorrect.asUInt
  output.bits.reminder := Mux(needCorrect, remainderNoCorrect, remainderCorrect)
  output.bits.quotient := quotient

  // for SRT4 -> CSA32
  // for SRT8 -> CSA32+CSA32
  // for SRT16 -> CSA53+CSA32
  // SRT16 <- SRT4 + SRT4*5
  val csa = Module(new CarrySaveAdder(CSACompressor3_2, xLen))
  csa.in(0) := partialReminderSum
  csa.in(1) := (partialReminderCarry(xLen, 1) ## !qdsSign)
  csa.in(2) := Mux1H(
    qds.output.selectedQuotientOH,
    //this is for SRT4, for SRT8 or SRT16, this should be changed
    VecInit((-2 to 2).map {
      case -2 => divider << 1
      case -1 => divider
      case 0  => 0.U
      case 1  => extend(~divider, xLen)
      case 2  => extend((~divider) << 1, xLen)
    })
  )

  // TODO: sel maybe have a problem
  partialReminderSum := Mux1H(
    Map(
      (counter === input.bits.counter) -> input.bits.dividend,
      counter.orR -> (csa.out(0) << radixLog2)(xLen - 1, 0),
      isLastCycle -> partialReminderSum
    )
  )

  partialReminderCarry := Mux1H(
    Map(
      (counter === input.bits.counter) -> 0.U,
      counter.orR -> (csa.out(1) << radixLog2)(xLen - 1, 0),
      isLastCycle -> partialReminderCarry
    )
  )
  // On-The-Fly conversion
  val otf = Module(new OTF(1 << radixLog2, n, ohWidth))
  otf.input.quotient := quotient
  otf.input.quotientMinusOne := quotientMinusOne
  otf.input.selectedQuotientOH := qds.output.selectedQuotientOH

  quotient := otf.output.quotient
  quotientMinusOne := otf.output.quotientMinusOne
  output.bits.quotient := quotient
}
