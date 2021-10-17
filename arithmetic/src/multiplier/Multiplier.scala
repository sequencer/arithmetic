package multiplier

import chisel3._

trait Multiplier[T] extends Module {
  val width: Int
  require(width > 0)
  val a: T
  val b: T
  val z: T
}

trait SignedMultiplier extends Multiplier[SInt] {
  val a: SInt = IO(Input(SInt(width.W)))
  val b: SInt = IO(Input(SInt(width.W)))
  val z: SInt = IO(Output(SInt((2 * width).W)))
  assert(a * b === z)
}

trait UnsignedMultiplier extends Multiplier[UInt] {
  val a: UInt = IO(Input(UInt(width.W)))
  val b: UInt = IO(Input(UInt(width.W)))
  val z: UInt = IO(Output(UInt((2 * width).W)))
  assert(a * b === z)
}
