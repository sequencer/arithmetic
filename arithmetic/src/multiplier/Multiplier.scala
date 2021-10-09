package multiplier

import chisel3._

trait Multiplier extends Module {
  val width: Int
  // TODO: add backpressure for in order signals?
  val stage: Int
  require(width > 0)
  val a: UInt = IO(Input(UInt(width.W)))
  val b: UInt = IO(Input(UInt(width.W)))
  val z: UInt = IO(Output(UInt((2 * width).W)))
}
