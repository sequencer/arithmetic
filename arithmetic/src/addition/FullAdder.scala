package arithmetic.addition.prefixadder

import chisel3._

trait FullAdder extends Module {
  val width: Int
  require(width > 0)
  val a: UInt = IO(Input(UInt(width.W)))
  val b: UInt = IO(Input(UInt(width.W)))
  val z: UInt = IO(Output(UInt((width + 1).W)))
  chisel3.experimental.verification.assert(a +& b === z)
}
