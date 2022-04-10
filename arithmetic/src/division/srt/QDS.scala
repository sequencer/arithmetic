package division.srt
import chisel3._
import chisel3.util.{RegEnable, Valid, log2Ceil}

class QDSInput extends Bundle {
  val partialReminderCarry: UInt = UInt(rWidth.W)
  val partialReminderSum:   UInt = UInt(rWidth.W)
}

class QDSOutput extends Bundle {
  // val selectedQuotient: UInt = UInt((log2Ceil(n)+1).W)
  val selectedQuotientOH: UInt = UInt(ohWidth.W)
}

/**
  */
class QDS(table: String, rWidth: Int, ohWidth: Int) extends Module {
  // IO
  val input = IO(Input(new QDSInput(rWidth)))
  val output = IO(Output(new QDSOutput(ohWidth)))
  
  // used to select a column of SRT Table
  val partialDivider = IO(Flipped(Valid(UInt()))) 
  val partialDividerReg = RegEnable(partialDivider.bits, partialDivider.valid) 

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
