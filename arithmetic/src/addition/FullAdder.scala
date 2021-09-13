package addition

import chisel3._

/** Top-level Module of the full adder.
 * Two input ports whose width are defined by [[width]]
 * The output is one bit wider than the input.
 * Carry-in and carry-out are not defined.
 */
trait FullAdder extends Module {
  val width: Int
  require(width > 0)
  val a: UInt = IO(Input(UInt(width.W)))
  val b: UInt = IO(Input(UInt(width.W)))
  val z: UInt = IO(Output(UInt((width + 1).W)))
  chisel3.experimental.verification.assert(a +& b === z)
}
