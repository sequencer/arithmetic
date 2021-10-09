package multiplier

import chisel3._
import chisel3.util.experimental.decode.{TruthTable, decoder}
import chisel3.util.{BitPat, isPow2, log2Ceil}

class Booth(width: Int)(radix: Int) extends Module {
  assert(isPow2(radix))
  val radixLog2 = log2Ceil(radix)
  val input = IO(Input(UInt(width.W)))
  val output = IO(Output(Vec(Math.ceil(width.toDouble / radixLog2).toInt, UInt((radixLog2 + 1).W))))

  Seq.tabulate(output.size) {
    case i if i <= radixLog2                 =>
    case width @ i if i >= width - radixLog2 =>
    case i => decoder(input(radixLog2 * (i + 1) - 1, radixLog2 * i - 1), TruthTable(
        Seq
          .tabulate(2 << (radixLog2 + 1)) { i =>
            Seq
              .tabulate(radixLog2 + 1)((bit: Int) => if (BigInt(i).testBit(bit)) 1 else 0)
              .zip(Seq.tabulate(radixLog2 + 1) {
                case i if i == 0         => -(2 << (radixLog2 - i - 1))
                case i if i == radixLog2 => 1
                case _                   => 2 << (radixLog2 - i - 1)
              })
              .map {
                case (a, b) => a * b
              }
              .sum
          }
          .zipWithIndex
          .map {
            case (o, i) =>
              (BitPat(i.U), BitPat(o.U))
          },
        BitPat.dontCare(radixLog2 + 1)
      ))
  }
}
