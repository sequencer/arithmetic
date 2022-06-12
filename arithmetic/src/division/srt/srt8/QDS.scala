package division.srt.srt8

import division.srt._
import chisel3._
import chisel3.util.BitPat
import chisel3.util.BitPat.bitPatToUInt
import chisel3.util.experimental.decode.TruthTable
import utils.{extend, sIntToBitPat}

class QDS(rWidth: Int, ohWidth: Int, partialDividerWidth: Int, tables: Seq[Seq[Int]]) extends Module {
  // IO
  val input = IO(Input(new QDSInput(rWidth, partialDividerWidth)))
  val output = IO(Output(new QDSOutput(ohWidth)))

  val columnSelect = input.partialDivider
  // Seq[Seq[Int]] => Vec[Vec[UInt]]
  lazy val selectRom = VecInit(tables.map {
    case x =>
      VecInit(x.map {
        case x => bitPatToUInt(sIntToBitPat(-x, rWidth))
      })
  })

  val adderWidth = rWidth + 1
  val yTruncate: UInt = input.partialReminderCarry + input.partialReminderSum
  val mkVec = selectRom(columnSelect)
  val selectPoints = VecInit(mkVec.map { mk =>
    (extend(yTruncate, adderWidth).asUInt
      + extend(mk, adderWidth).asUInt).head(1)
  }).asUInt

  // decoder or findFirstOne here, prefer decoder, the decoder only for srt8(a = 7)
  output.selectedQuotientOH := chisel3.util.experimental.decode.decoder(
    selectPoints,
    TruthTable(
      Seq( // 8 4 0 -4 -8__2 1 0 -1 -2
        BitPat("b??_????_????_???0") -> BitPat("b10000_00010"), // 7 = +8 + (-1)
        BitPat("b??_????_????_??01") -> BitPat("b01000_10000"), // 6 = +4 + (+2)
        BitPat("b??_????_????_?011") -> BitPat("b01000_01000"), // 5 = +4 + (+1)
        BitPat("b??_????_????_0111") -> BitPat("b01000_00100"), // 4 = +4 + ( 0)
        BitPat("b??_????_???0_1111") -> BitPat("b01000_00010"), // 3 = +4 + (-1)
        BitPat("b??_????_??01_1111") -> BitPat("b00100_10000"), // 2 =  0 + (+2)
        BitPat("b??_????_?011_1111") -> BitPat("b00100_01000"), // 1 =  0 + (+1)
        BitPat("b??_????_0111_1111") -> BitPat("b00100_00100"), // 0 =  0 + ( 0)
        BitPat("b??_???0_1111_1111") -> BitPat("b00100_00010"), //-1 =  0 + (-1)
        BitPat("b??_??01_1111_1111") -> BitPat("b00100_00001"), //-2 =  0 + (-2)
        BitPat("b??_?011_1111_1111") -> BitPat("b00010_01000"), //-3 = -4 + ( 1)
        BitPat("b??_0111_1111_1111") -> BitPat("b00010_00100"), //-4 = -4 + ( 0)
        BitPat("b?0_1111_1111_1111") -> BitPat("b00010_00010"), //-5 = -4 + (-1)
        BitPat("b01_1111_1111_1111") -> BitPat("b00010_00001") //-6 = -4 + (-2)
      ),
      BitPat("b00001_01000") //-7 = -8 + (+1)
    )
  )
}

object QDS {
  def apply(
    rWidth:               Int,
    ohWidth:              Int,
    partialDividerWidth:  Int,
    tables:               Seq[Seq[Int]]
  )(partialReminderSum:   UInt,
    partialReminderCarry: UInt,
    partialDivider:       UInt
  ): UInt = {
    val m = Module(new QDS(rWidth, ohWidth, partialDividerWidth, tables))
    m.input.partialReminderSum := partialReminderSum
    m.input.partialReminderCarry := partialReminderCarry
    m.input.partialDivider := partialDivider
    m.output.selectedQuotientOH
  }
}
