package division.srt
import chisel3._
import chisel3.util.{RegEnable, Valid, log2Ceil}

class QDSInput extends Bundle {
  val partialReminderCarry: UInt = ???
  val partialReminderSum: UInt = ???
}

class QDSOutput extends Bundle {
  val selectedQuotient: UInt = UInt((log2Ceil(n)+1).W)
}

class QDS extends Module {
  val input = IO(Input(new QDSInput))
  val output = IO(Output(new QDSOutput))
  // used to select a column of SRT Table
  val partialDivider = IO(Flipped(Valid(UInt())))  //它表达的是什么意思？
  val partialDividerReg = RegEnable(partialDivider.bits, partialDivider.valid) 
  // for the first cycle: use partialDivider on the IO
  // for the reset of cycles: use partialDividerReg
  // for synthesis: the constraint should be IO -> Output is a multi-cycle design
  //                                         Reg -> Output is single-cycle
  // to avoid glitch, valid should be larger than raise time of partialDividerReg
  val partialDividerLatch = Mux(partialDivider.valid, partialDivider.bits, partialDividerReg)

}
