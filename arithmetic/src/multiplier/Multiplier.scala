package multiplier

import chisel3._
import chisel3.experimental.FixedPoint

trait Multiplier[T] extends Module {
  val aWidth: Int
  val bWidth: Int
  require(aWidth > 0)
  require(bWidth > 0)
  val a: T
  val b: T
  val z: T
}

trait SignedMultiplier extends Multiplier[SInt] {
  val a: SInt = IO(Input(SInt(aWidth.W)))
  val b: SInt = IO(Input(SInt(bWidth.W)))
  val z: SInt = IO(Output(SInt((aWidth + bWidth).W)))
  assert(a * b === z)
}

trait UnsignedMultiplier extends Multiplier[UInt] {
  val a: UInt = IO(Input(UInt(aWidth.W)))
  val b: UInt = IO(Input(UInt(bWidth.W)))
  val z: UInt = IO(Output(UInt((aWidth + bWidth).W)))
  assert(a * b === z)
}

trait FixedPointMultiplier extends Multiplier[FixedPoint] {

  val aBPWidth: Int
  
  val bBPWidth: Int

  require(aBPWidth > 0)
  require(bBPWidth > 0)
  val a: FixedPoint = IO(Input(FixedPoint( aWidth.W, aBPWidth.BP )))
  val b: FixedPoint = IO(Input(FixedPoint( bWidth.W, bBPWidth.BP )))
  val z: FixedPoint = IO(Output(FixedPoint((aWidth + bWidth).W, (aBPWidth + bBPWidth).BP)))
  assert(a * b === z)
}