package multiplier

import chisel3._

trait Multiplier extends Module {
  val width: Int
  // TODO: add backpressure for in order signals?
  val stage: Int
  require(width > 0)
  val a: SInt = IO(Input(SInt(width.W)))
  val b: SInt = IO(Input(SInt(width.W)))
  val z: SInt = IO(Output(SInt((2 * width).W)))
}
