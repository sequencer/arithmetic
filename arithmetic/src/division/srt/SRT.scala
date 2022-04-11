package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import chisel3._
import chisel3.util.{log2Ceil, DecoupledIO, Mux1H, ValidIO}


// TODO: width
class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int, radix: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W) //0.1**********
  val divider = UInt(dividerWidth.W) //0.1**********
  val counter = UInt((log2Ceil(n / log2Ceil(radix))).W) // n为需要计算的二进制位数
}

class SRTOutput(reminderWidth: Int, quotientWidth: Int) extends Bundle {
  val reminder = UInt(reminderWidth.W)
  val quotient = UInt(quotientWidth.W)
}

// only SRT4 currently
class SRT(
  dividendWidth:  Int,
  dividerWidth:   Int,
  n:              Int,
  radix:          Int = 4,
  a:              Int = 2, 
  dTruncateWidth: Int = 4, 
  xTruncateWidth: Int = 3
  )
    extends Module {
  // IO
  val input = Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n)))
  val output = ValidIO(new SRTOutput(dividerWidth, dividendWidth))

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = Reg(UInt((dividendWidth + log2Ceil(radix)).W))
  val partialReminderSum = Reg(UInt((dividendWidth + log2Ceil(radix)).W))

  // dMultiplier
  val divider = RegInit(input.divider)

  val quotient = Reg(UInt(n.W)) //?
  val quotientMinusOne = Reg(UInt(n.W)) //?

  // counter = 0 quotientToFix = 0 ->
  val counter = Reg(UInt((log2Ceil(n / log2Ceil(radix))).W))

  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())

  // Datapath
  // from software get quotient select Constant tables,how to convert double to UInt and  store  in ROM
  val table: Seq[(Int, Int)] = SRTTable(radix, a, dTruncateWidth, xTruncateWidth).qdsTables
  val rTruncateWidth: Int = ???
  val selectedQuotientOHWidth: Int = ???
  val qds = Module(new QDS(table, rTruncateWidth, selectedQuotientOHWidth))
  // TODO: bit select here
  qds.input.partialReminderSum := partialReminderSum.head(rTruncateWidth)
  qds.input.partialReminderCarry := partialReminderCarry.head(rTruncateWidth)

  counter := counter - 1.U

  val sz = Module(new SZ(dividendWidth))
  sz.input.partialReminderSum := partialReminderSum
  sz.input.partialReminderCarry := partialReminderCarry

  // the output of srt
  output.remainder := remainder
  output.quotient := quotient

  // if counter === 0.U && sz.output.sign, correct the quotient and remainder. valid = 1
  // TODO: correct
  quotient      := Mux(counter === 0.U && sz.output.sign, ???, quotient)
  remainder     := Mux(counter === 0.U && sz.output.sign, ???, remainder)
  output.valid  := Mux(counter === 0.U, true.B, false.B)

  // for SRT4 -> CSA32
  // for SRT8 -> CSA32+CSA32
  // for SRT16 -> CSA53+CSA32
  // SRT16 <- SRT4 + SRT4*5
  // {
  val csa = Module(new CarrySaveAdder(CSACompressor3_2, ???))
  csa.in(0) := partialReminderSum
  csa.in(1) := (partialReminderCarry ## !qdsSign)
  csa.in(2) := Mux1H(
    qds.output.selectedQuotientOH,
    // TODO: this is for SRT4, for SRT8 or SRT16, this should be changed
    VecInit((-2 to 2).map {
      case -2 => divider << 1
      case -1 => divider
      case 0  => 0.U
      case 1  => ~divider
      case 2  => (~divider) << 1
    })
  )
  // }
  partialReminderSum := Mux1H(
    Map(
      (counter === n / log2Ceil(radix)) -> input.bits.dividend,
      (counter > 0.U) -> (csa.out(0) << log2Ceil(n)),
      (counter === 0.U) -> partialReminderSum
    )
  )
  partialReminderCarry := Mux1H(
    Map(
      (counter === n / log2Ceil(radix)) -> 0.U,
      (counter > 0.U) -> (csa.out(1) << log2Ceil(n) - 1),
      (counter === 0.U) -> partialReminderCarry
    )
  )

  // On-The-Fly conversion
  val otf = Module(new OTF(radix, quotient.getWidth, qds.output.selectedQuotientOH.getWidth))
  otf.input.quotient := quotient
  otf.input.quotientMinusOne := quotientMinusOne
  otf.input.selectedQuotientOH := qds.output.selectedQuotientOH

  quotient := otf.output.quotient
  quotientMinusOne := otf.output.quotientMinusOne
  output.bits.quotient := quotient
}
