package addition

import addition.prefixadder.common.{BrentKungSum, KoggeStoneSum, RippleCarrySum}
import chisel3._
import utils.{extend, sIntToBitPat}

package object prefixadder {

  def apply(prefixSum: PrefixSum, width: Option[Int] = None)(a: UInt, b: UInt, cin: Boolean = false.B) = {
    val WIDTH = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max)
    val m = Module(new UnsignedPrefixAdder(WIDTH, prefixSum))
    m.a := a
    m.b := b
    m.cin := cin
    (m.cout, m.z)
  }

  def apply(prefixSum: PrefixSum, width: Option[Int] = None)(a: SInt, b: SInt) = {
    val WIDTH = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max)
    val m = Module(new SignedPrefixAdder(WIDTH, prefixSum))
    m.a := extend(a, WIDTH, true)
    m.b := extend(b, WIDTH, true)
    m.z
  }

  def brentKun[T <: DATA](a: T, b: T, cin: Boolean = false.B, width: Option[Int] = None) = apply(BrentKungSum, width)(a, b, cin)

  def koggeStone[T <: DATA](a: T, b: T, cin: Boolean = false.B, width: Option[Int] = None) = apply(KoggeStoneSum, width)(a, b, cin)

  def rippleCarry[T <: DATA](a: T, b: T, cin: Boolean = false.B, width: Option[Int] = None) = apply(RippleCarrySum, width)(a, b, cin)
}
