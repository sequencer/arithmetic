package multiplier

import chisel3._
import chisel3.util.experimental.decode.{TruthTable, decoder}
import chisel3.util.{BitPat, Cat, Fill, isPow2, log2Ceil}
import utils.sIntToBitPat

class Booth(width: Int)(radixLog2: Int) extends Module {
  val input = IO(Input(UInt(width.W)))
  val output = IO(Output(Vec(
    (width - 1) / radixLog2 + 1, // = ceil(width / radixLog2)
    SInt((radixLog2 + 1).W)
  )))

  val paddingLeftWidth = radixLog2 - 1 - (width - 1) % radixLog2
  val signBit = input(width - 1)
  val paddedInput = if (paddingLeftWidth == 0)
    Cat(input, 0.U(1.W))
  else {
    Cat(Fill(paddingLeftWidth, signBit), input, 0.U(1.W))
  }

  val boothEncodingCoeff = Seq.tabulate(radixLog2 + 1) {
    case i if i == radixLog2 => -(1 << (radixLog2 - 1))
    case i if i == 0 => 1
    case i => 1 << (i - 1)
  }

  val boothEncodingTable = TruthTable(
    Seq
      .tabulate(1 << (radixLog2 + 1)) { i =>
        Seq
          .tabulate(radixLog2 + 1)((bit: Int) => if (BigInt(i).testBit(bit)) 1 else 0)
          .zip(boothEncodingCoeff)
          .map {
            case (a, b) => a * b
          }
          .sum
      }
      .zipWithIndex
      .map {
        case (o, i) =>
          val w = radixLog2 + 1
          (sIntToBitPat(i, w), sIntToBitPat(o, w))
      },
    BitPat.dontCare(radixLog2 + 1)
  )

  output := Seq.tabulate(output.size) { i =>
    decoder(paddedInput(radixLog2 * (i + 1), radixLog2 * i), boothEncodingTable)
  }.map(_.asSInt)
}

object Booth {
  def recode(width: Int)(radix: Int)(x: UInt): Vec[SInt] = {
    val recoder = Module(new Booth(width)(radix))
    recoder.input := x
    recoder.output
  }
}
