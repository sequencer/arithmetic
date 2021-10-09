package multiplier

import chisel3._
import chisel3.util.experimental.decode.{TruthTable, decoder}
import chisel3.util.{Cat, BitPat, isPow2, log2Ceil}

class Booth(width: Int)(radix: Int) extends Module {
  assert(isPow2(radix))
  val radixLog2 = log2Ceil(radix)
  val input = IO(Input(UInt(width.W)))
  val output = IO(Output(Vec(
    (width - 1) / radixLog2 + 1, // = ceil(width / radixLog2)
    UInt((radixLog2 + 1).W)
  )))

  val paddingLeftWidth = radixLog2 - 1 - (width - 1) % radixLog2
  val paddedInput = if (paddingLeftWidth == 0)
    Cat(input, 0.U(1.W))
  else {
    Cat(0.U(paddingLeftWidth.W), input, 0.U(1.W))
  }

  val boothEncodingCoeff = Seq.tabulate(radixLog2 + 1) {
    case i if i == radixLog2 => -(1 << (radixLog2 - 1))
    case i if i == 0 => 1
    case i => 1 << (i - 1)
  }
  println(boothEncodingCoeff)

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
          val w = (radixLog2 + 1).W
          (BitPat(i.U(w)), BitPat((o + (1 << (radixLog2 - 1))).U(w)))
      },
    BitPat.dontCare(radixLog2 + 1)
  )
  println(boothEncodingTable)
  printf("%b", paddedInput)

  output := Seq.tabulate(output.size) { i =>
    decoder(paddedInput(radixLog2 * (i + 1), radixLog2 * i), boothEncodingTable)
  }
}
