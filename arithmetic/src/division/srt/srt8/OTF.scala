package division.srt.srt8

import division.srt._
import chisel3._
import chisel3.util.Mux1H

class OTF(radixLog2: Int, qWidth: Int, ohWidth: Int) extends Module {
  val input = IO(Input(new OTFInput(qWidth, ohWidth)))
  val output = IO(Output(new OTFOutput(qWidth)))

  val radix: Int = 1 << radixLog2
  // datapath
  // q_j+1 in this circle, only for srt8(a = 7)
  val qNext: UInt = Mux1H(
    Seq(
      input.selectedQuotientOH(0) -> "b11110".U, // -2
      input.selectedQuotientOH(1) -> "b11111".U, // -1
      input.selectedQuotientOH(2) -> "b00000".U, // 0
      input.selectedQuotientOH(3) -> "b00001".U, // 1
      input.selectedQuotientOH(4) -> "b00010".U // 2
    )
  ) + Mux1H(
    Seq(
      input.selectedQuotientOH(5) -> "b11000".U, // -8
      input.selectedQuotientOH(6) -> "b11100".U, // -4
      input.selectedQuotientOH(7) -> "b00000".U, // 0
      input.selectedQuotientOH(8) -> "b00100".U, // 4
      input.selectedQuotientOH(9) -> "b01000".U // 8
    )
  )

  // val cShiftQ:  Bool = qNext >= 0.U
  // val cShiftQM: Bool = qNext <=  0.U
  val cShiftQ: Bool = input.selectedQuotientOH(9, 8).orR ||
    (input.selectedQuotientOH(7) && input.selectedQuotientOH(4, 2).orR)
  val cShiftQM: Bool = input.selectedQuotientOH(6, 5).orR ||
    (input.selectedQuotientOH(7) && input.selectedQuotientOH(2, 0).orR)

  val qIn:  UInt = Mux(cShiftQ, qNext, radix.U + qNext)(radixLog2 - 1, 0)
  val qmIn: UInt = Mux(!cShiftQM, qNext - 1.U, (radix - 1).U + qNext)(radixLog2 - 1, 0)

  output.quotient := Mux(cShiftQ, input.quotient, input.quotientMinusOne)(qWidth - radixLog2, 0) ## qIn
  output.quotientMinusOne := Mux(!cShiftQM, input.quotient, input.quotientMinusOne)(qWidth - radixLog2, 0) ## qmIn
}

object OTF {
  def apply(
    radixLog2:          Int,
    qWidth:             Int,
    ohWidth:            Int
  )(quotient:           UInt,
    quotientMinusOne:   UInt,
    selectedQuotientOH: UInt
  ): Seq[UInt] = {
    val m = Module(new OTF(radixLog2, qWidth, ohWidth))
    m.input.quotient := quotient
    m.input.quotientMinusOne := quotientMinusOne
    m.input.selectedQuotientOH := selectedQuotientOH
    Seq(m.output.quotient, m.output.quotientMinusOne)
  }
}
