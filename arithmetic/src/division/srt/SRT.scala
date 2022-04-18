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

// TODO: width
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
  n:              Int, // the longest width
  radixLog2:      Int = 2,
  a:              Int = 2,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {
  val ohWidth: Int = 2 * a + 1

  // IO
  val input = Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n)))
  val output = ValidIO(new SRTOutput(dividerWidth, dividendWidth))

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = Reg(UInt((dividendWidth + radixLog2).W))
  val partialReminderSum = Reg(UInt((dividendWidth + radixLog2).W))
  val divider = RegInit(input.bits.divider)
  val quotient = Reg(UInt(n.W))
  val quotientMinusOne = Reg(UInt(n.W))
  val counter = RegInit(input.bits.counter)
  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())
  qdsSign := qds.output.selectedQuotientOH(ohWidth - 1, ohWidth / 2).orR

  // Datapath
  val qds = Module(new QDS(rTruncateWidth, ohWidth))
  qds.input.partialReminderSum := partialReminderSum.head(1 + radixLog2 + rTruncateWidth)
  qds.input.partialReminderCarry := partialReminderCarry.head(1 + radixLog2 + rTruncateWidth)
  qds.partialDivider.bits := input.bits.divider.head(1 + radixLog2 + rTruncateWidth)(dTruncateWidth - 2, 0)

  counter := counter - radixLog2.U
  // if counter === 0.U && sz.output.sign, correct the quotient and reminder. valid = 1
  // the output of srt
  val sz = Module(new SZ(dividendWidth - 2))
  sz.input.partialReminderSum := partialReminderSum(partialReminderSum.getWidth-3, 0)
  sz.input.partialReminderCarry := partialReminderCarry(partialReminderSum.getWidth-3, 0)
  output.valid := Mux(counter === 0.U, true.B, false.B)

  // correcting maybe have problem
  quotient := Mux(counter === 0.U && sz.output.sign, quotient - 1.U, quotient)
  output.bits.reminder := Mux1H(
    Map(
      (counter === 0.U && sz.output.zero) -> 0.U,
      (counter === 0.U && sz.output.sign) -> (sz.output.remainder + 1.U + divider),
      (counter === 0.U && !sz.output.sign) -> (sz.output.remainder + 1.U)
    )
  )
  output.bits.quotient := quotient

  // for SRT4 -> CSA32
  // for SRT8 -> CSA32+CSA32
  // for SRT16 -> CSA53+CSA32
  // SRT16 <- SRT4 + SRT4*5
  val csa = Module(new CarrySaveAdder(CSACompressor3_2, dividendWidth + radixLog2))
  csa.in(0) := partialReminderSum
  csa.in(1) := (partialReminderCarry ## !qdsSign)
  csa.in(2) := Mux1H(
    qds.output.selectedQuotientOH,
    // TODO: this is for SRT4, for SRT8 or SRT16, this should be changed
    VecInit((-2 to 2).map {
      case -2 => divider << 1
      case -1 => divider
      case 0  => 0.U
      case 1  => extend(~divider, dividendWidth + radixLog2)
      case 2  => extend((~divider) << 1, dividendWidth + radixLog2)
    })
  )

  partialReminderSum := Mux1H(
    Map(
      (counter === input.bits.counter) -> input.bits.dividend,
      (counter > 0.U) -> (csa.out(0) << radixLog2),
      (counter === 0.U) -> partialReminderSum
    )
  )

  partialReminderCarry := Mux1H(
    Map(
      (counter === input.bits.counter) -> 0.U,
      (counter > 0.U) -> (csa.out(1) << (radixLog2 - 1)),
      (counter === 0.U) -> partialReminderCarry
    )
  )

  // On-The-Fly conversion
  val otf = Module(new OTF((1 << radixLog2), n, ohWidth))
  otf.input.quotient := quotient
  otf.input.quotientMinusOne := quotientMinusOne
  otf.input.selectedQuotientOH := qds.output.selectedQuotientOH

  quotient := otf.output.quotient
  quotientMinusOne := otf.output.quotientMinusOne
  output.bits.quotient := quotient
}
