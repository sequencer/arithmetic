package division.srt

import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import chisel3._
import chisel3.util.{log2Ceil, Decoupled, DecoupledIO, Mux1H}

class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W)
  val divider = UInt(dividerWidth.W)
  val counter = UInt(log2Ceil(???).W)
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
  val input:  DecoupledIO[SRTInput] = Flipped(Decoupled(new SRTInput(dividendWidth, dividerWidth, n)))
  val output: DecoupledIO[SRTOutput] = Decoupled(new SRTOutput(dividerWidth, dividendWidth))

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = Reg(UInt())
  val partialReminderSum = Reg(UInt())
  val divider = Reg(UInt())

  val quotient = Reg(UInt())
  val quotientMinusOne = Reg(UInt())

  val state = Reg(UInt())
  val counter = Reg(UInt())

  // Control
  // sign of select quotient, true -> negative, false -> positive
  val qdsSign: Bool = Wire(Bool())

  // Datapath
  val qds = new QDS("???")
  // TODO: bit select here
  qds.input.partialReminderSum := partialReminderSum
  qds.input.partialReminderCarry := partialReminderCarry

  // for SRT4 -> CSA32
  // for SRT8 -> CSA32+CSA32
  // for SRT16 -> CSA53+CSA32
  // SRT16 <- SRT4 + SRT4*5
  // {
    val csa = new CarrySaveAdder(CSACompressor3_2, ???)
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
}
