package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import chisel3._
import chisel3.util.{DecoupledIO, Mux1H, ValidIO, log2Ceil}

class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W)
  val divider = UInt(dividerWidth.W)
  val counter = UInt(log2Ceil(n).W)
}

class SRTOutput(reminderWidth: Int, quotientWidth: Int) extends Bundle {
  val reminder = UInt(reminderWidth.W)
  val quotient = UInt(quotientWidth.W)
}

// only SRT4 currently
class SRT(
  dividendWidth: Int,
  dividerWidth:  Int,
  n:             Int)
    extends Module {
  // IO
  val input = Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n)))
  val output = ValidIO(new SRTOutput(dividerWidth, dividendWidth))

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = Reg(UInt())
  val partialReminderSum = Reg(UInt())

  // dMultiplier
  val divider = Reg(UInt())

  val quotient = Reg(UInt())
  val quotientMinusOne = Reg(UInt())

  // counter = 0 quotientToFix = 0 ->
  val counter = Reg(UInt())

  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())

  // Datapath
  val qds = Module(new QDS("???"))
  // TODO: bit select here
  qds.input.partialReminderSum := partialReminderSum
  qds.input.partialReminderCarry := partialReminderCarry

  counter := counter - 1.U

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
      ??? -> input.bits.dividend,
      ??? -> (csa.out(0) << log2Ceil(n)),
      ??? -> partialReminderSum
    )
  )
  partialReminderCarry := Mux1H(
    Map(
      ??? -> 0.U,
      ??? -> (csa.out(1) << log2Ceil(n)),
      ??? -> partialReminderCarry
    )
  )

  val csa = new CarrySaveAdder(CSACompressor3_2, ???)
  csa.in(0) := partialReminderSum
  csa.in(1) := (partialReminderCarry ## !qdsSign) //？这里有点问题
  csa.in(2) := Mux1H(Map(
    ??? -> ,
    ??? ->
  ))
  partialReminderSum := Mux1H(Map(
    ??? -> input.bits.dividend,
    ??? -> (csa.out(0) << log2Ceil(n)),
    ??? -> partialReminderSum
  ))
  partialReminderCarry := Mux1H(Map(
    ??? -> 0.U, 
    ??? -> (csa.out(1) << log2Ceil(n)),
    ??? -> partialReminderCarry
  ))

  // On-The-Fly conversion
  val otf = Module(new OTF)
  otf.input.quotient := quotient
  otf.input.quotientMinusOne := quotientMinusOne
  otf.input.selectedQuotientOH := qds.output.selectedQuotientOH
  quotient := otf.output.quotient
  quotientMinusOne := otf.output.quotientMinusOne
  output.bits.quotient := quotient

}
