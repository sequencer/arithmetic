package division.srt
import chisel3._
import chisel3.util.{RegEnable, Valid, log2Ceil}

class QDSInput extends Bundle {
  val partialReminderCarry: UInt = ???
  val partialReminderSum:   UInt = ???
}

class QDSOutput extends Bundle {
  val selectedQuotient: UInt = UInt((log2Ceil(n)+1).W)
  val selectedQuotientOH: UInt = ???
}

/**
  */
class QDS(table: String) extends Module {
  // IO
  val input = IO(Input(new QDSInput))
  val output = IO(Output(new QDSOutput))
  // used to select a column of SRT Table

  val partialDivider = IO(Flipped(Valid(UInt()))) 
  val partialDividerReg = RegEnable(partialDivider.bits, partialDivider.valid) 

  val partialDivider = IO(Flipped(Valid(UInt())))

  // State
  val partialDividerReg = RegEnable(partialDivider.bits, partialDivider.valid)
  // for the first cycle: use partialDivider on the IO
  // for the reset of cycles: use partialDividerReg
  // for synthesis: the constraint should be IO -> Output is a multi-cycle design
  //                                         Reg -> Output is single-cycle
  // to avoid glitch, valid should be larger than raise time of partialDividerReg
  val partialDividerLatch = Mux(partialDivider.valid, partialDivider.bits, partialDividerReg)

  // Datapath
  val columnSelect = partialDividerLatch
  val rowSelect = input.partialReminderCarry + input.partialReminderSum
  val selectRom: Vec[Vec[UInt]] = ???
  val mkVec = selectRom(columnSelect)
  val selectPoints = mkVec.map{mk =>
    // get the select point
    input.partialReminderCarry + input.partialReminderSum - mk
  }
  // decoder or findFirstOne here, prefer decoder

}
