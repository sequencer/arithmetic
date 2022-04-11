package division.srt
import chisel3._
import chisel3.util.{log2Ceil, RegEnable, Valid}
import chisle3.util.experimental.decode

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
class QDS(table: Seq[(Int, Int)], rWidth: Int, ohWidth: Int) extends Module {
  // IO
  val input = IO(Input(new QDSInput(rWidth)))
  val output = IO(Output(new QDSOutput(ohWidth)))

  // used to select a column of SRT Table
  val partialDivider = IO(Flipped(Valid(UInt(???)))) //这个从哪里来

  // State, in order to keep divider's value
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

  // // use the table from XiangShan
  // // from XiangShan: /16
  // val qSelTable = Array(
  //   Array(12, 4, -4, -13),
  //   Array(14, 4, -6, -15),
  //   Array(15, 4, -6, -16),
  //   Array(16, 4, -6, -18),
  //   Array(18, 6, -8, -20),
  //   Array(20, 6, -8, -20),
  //   Array(20, 8, -8, -22),
  //   Array(24, 8, -8, -24)
  // )

  // TODO: complete select_table algorithm
  val selectRom: Vec[(UInt, SInt)] = table.map{case (d, x) => (d.U, x.S)}
  val mkVec = selectRom.filter{ case (d, x) => d === columnSelect }.map{ case (d, x) => x }

  val selectPoints = mkVec.map { mk =>
    // get the select point
    // TODO: find the sign
    (input.partialReminderCarry + input.partialReminderSum - mk).head(1)
  }.flatten.asUInt

  // decoder or findFirstOne here, prefer decoder
  // the decoder only for srt4
  io.output := chisel3.util.experimental.decode.decoder(
    selectPoints,
    TruthTable(
      Seq(
        BitPat("b1???") -> BitPat("b10000"),
        BitPat("b01??") -> BitPat("b01000"),
        BitPat("b001?") -> BitPat("b00100"),
        BitPat("b0001") -> BitPat("b00010"),
      ),
      BitPat("b00001")
    )
  )
}
