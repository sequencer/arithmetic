package crypto.aes

import chisel3._

class SBox(matrix: ConstantMatrix, portSize: Int) extends Module {
  val address = Seq.fill(portSize) { IO(Input(UInt(8.W))) }
  val data = Seq.fill(portSize) { IO(Output(UInt(8.W))) }
  // TODO: need rom compiler?
}
