package addition

import addition.prefixadder.common.{BrentKungSum, KoggeStoneSum, RippleCarrySum}
import chisel3._

package object prefixadder {
  def apply(prefixSum: PrefixSum, width: Option[Int] = None)(a: UInt, b: UInt) = {
    val m = Module(new PrefixAdder(width.getOrElse(Seq(a, b).flatMap(_.widthOption).max), prefixSum))
    m.a := a
    m.b := b
    m.z
  }

  def brentKun(a: UInt, b: UInt, width: Option[Int] = None) = apply(BrentKungSum, width)(a, b)

  def koggeStone(a: UInt, b: UInt, width: Option[Int] = None) = apply(KoggeStoneSum, width)(a, b)

  def rippleCarry(a: UInt, b: UInt, width: Option[Int] = None) = apply(RippleCarrySum, width)(a, b)
}
