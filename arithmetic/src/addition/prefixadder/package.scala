package addition

import addition.prefixadder.common.{BrentKungSum, KoggeStoneSum, RippleCarrySum}
import chisel3._
import utils.{extend, sIntToBitPat}
import chisel3.experimental.FixedPoint

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


  // // TODO...获取位宽
  //  def apply(prefixSum: PrefixSum, width: Option[Int] = None, BPWidth: Option[Int] = None)(a: FixedPoint, b: FixedPoint) = {
  //   val WIDTH = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max)  // 整数位宽
  //   val BPWIDTH = width.getOrElse(Seq(a, b).flatMap(_.widthOption).max) // 小数位宽
  //   val m = Module(new FixedPointPrefixAdder(WIDTH, BPWIDTH, prefixSum))
  //   m.a := extend(a, WIDTH, true)                                      //扩展位宽
  //   m.b := extend(b, WIDTH, true)
  //   m.z
  // }


  def brentKun[T <: DATA](a: T, b: T, cin: Boolean = false.B, width: Option[Int] = None) = apply(BrentKungSum, width)(a, b, cin)

  def koggeStone[T <: DATA](a: T, b: T, cin: Boolean = false.B, width: Option[Int] = None) = apply(KoggeStoneSum, width)(a, b, cin)

  def rippleCarry[T <: DATA](a: T, b: T, cin: Boolean = false.B, width: Option[Int] = None) = apply(RippleCarrySum, width)(a, b, cin)

  def brentKun[T <: DATA](a: T, b: T, width: Option[Int] = None) = apply(BrentKungSum, width)(a, b)

  def koggeStone[T <: DATA](a: T, b: T, width: Option[Int] = None) = apply(KoggeStoneSum, width)(a, b)

  def rippleCarry[T <: DATA](a: T, b: T, width: Option[Int] = None) = apply(RippleCarrySum, width)(a, b)
}
