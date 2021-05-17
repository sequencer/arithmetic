package division.srt

import chisel3._
import chisel3.util.Decoupled

class DividerInput(width: Int) extends Bundle {
  val dividend: UInt = UInt(width.W)
  val divider:  UInt = UInt(width.W)
}
class SRTDivider(width: Int) extends MultiIOModule {
  // dividend and divider should fire in the same time.
  val in = IO(Flipped(Decoupled(new DividerInput(width))))
  val remainder = IO(Decoupled(UInt(width.W)))
  val quotient = IO(Decoupled(UInt(width.W)))
}
