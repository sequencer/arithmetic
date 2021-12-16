package addition

import addition.prefixadder.common.{BrentKungSum, KoggeStoneSum, RippleCarrySum}
import chisel3._
import chisel3.util.Cat
import utils.{extend, sIntToBitPat}
import javax.xml.crypto.Data

package object prefixadder {

  def apply(prefixSum: PrefixSum, width: Option[Int] = None)(a: UInt, b: UInt, cin: Bool = false.B) = {
    val WIDTH = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max)
    val m = Module(new UnsignedPrefixAdder(WIDTH, prefixSum))
    m.a := a
    m.b := b
    m.cin := cin
    Cat(m.cout, m.z)
  }

  def apply(prefixSum: PrefixSum, width: Option[Int] = None)(a: SInt, b: SInt) = {
    val WIDTH = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max)
    val m = Module(new SignedPrefixAdder(WIDTH, prefixSum))
    m.a := extend(a, WIDTH, true)
    m.b := extend(b, WIDTH, true)
    m.z
  }

  def brentKun(a: UInt, b: UInt, cin: Bool = false.B, width: Option[Int] = None) = apply(BrentKungSum, width)(a, b, cin)

  def koggeStone(a: UInt, b: UInt, cin: Bool = false.B, width: Option[Int] = None) = apply(KoggeStoneSum, width)(a, b, cin)

  def rippleCarry(a: UInt, b: UInt, cin: Bool = false.B, width: Option[Int] = None) = apply(RippleCarrySum, width)(a, b, cin)
}
