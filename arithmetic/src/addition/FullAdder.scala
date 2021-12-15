package addition

import chisel3._
import scala.math.{max}

trait FullAdder[T] extends Module {
  val a: T
  val b: T
  val z: T
}

trait UnsignedFullAdder extends FullAdder[UInt] {
  val width: Int
  require(width > 0)
  val a: UInt = IO(Input(UInt(width.W)))
  val b: UInt = IO(Input(UInt(width.W)))
  val z: UInt = IO(Output(UInt(width.W)))

  val cin: Bool = IO(Input(Bool))
  val cout:Bool = IO(Output(Bool))
  
  assert(a +& b +& cin === Cat(cout,z))
}


trait SignedFullAdder extends FullAdder[SInt] {
  val width: Int
  require(width > 0)
  val a: SInt = IO(Input(SInt(width.W)))
  val b: SInt = IO(Input(SInt(width.W)))
  val z: SInt = IO(Output(SInt((width+1).W)))
  assert(a +& b === z)
}

