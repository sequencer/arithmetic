package multiplier

import addition.prefixadder.PrefixSum
import addition.prefixadder.common.BrentKungSum
import chisel3._
import chisel3.util._
import utils.signExt

class WallaceMultiplier(
  val width:  Int
)(sumUpAdder: PrefixSum = BrentKungSum,
  // TODO: add additional stage for final adder?
  pipeAt: Seq[Int] = Nil
  // TODO: making addOneColumn to be configurable to add more CSA to make this circuit more configurable?
) extends Multiplier {

  // TODO: use chisel type here?
  def addOneColumn(col: Seq[Bool]): (Seq[Bool], Seq[Bool], Seq[Bool]) =
    col.size match {
      case 1 => // do nothing
        (col, Seq.empty[Bool], Seq.empty[Bool])
      case 2 =>
        val c22 = addition.csa.c22(VecInit(col)).map(_.asBool).reverse
        (Seq(c22(0)), Seq.empty[Bool], Seq(c22(1)))
      case 3 =>
        val c32 = addition.csa.c32(VecInit(col)).map(_.asBool).reverse
        (Seq(c32(0)), Seq.empty[Bool], Seq(c32(1)))
      case 4 =>
        val c53 = addition.csa.c53(VecInit(col :+ false.B)).map(_.asBool).reverse
        (Seq(c53(0)), Seq(c53(1)), Seq(c53(2)))
      case 5 =>
        val c53 = addition.csa.c53(VecInit(col)).map(_.asBool).reverse
        (Seq(c53(0)), Seq(c53(1)), Seq(c53(2)))
      case _ =>
        val (s_1, c_1_1, c_1_2) = addOneColumn(col.take(5))
        val (s_2, c_2_1, c_2_2) = addOneColumn(col.drop(5))
        (s_1 ++ s_2, c_1_1 ++ c_2_1, c_1_2 ++ c_2_2)
    }

  def addAll(cols: Array[_ <: Seq[Bool]], depth: Int): (UInt, UInt) = {
    if (cols.map(_.size).max <= 2) {
      val sum = Cat(cols.map(_.head).reverse)
      val carry = Cat(cols.map(col => if (col.length > 1) col(1) else 0.B).reverse)
      (sum, carry)
    } else {
      val columns_next = Array.fill(2 * width)(Seq[Bool]())
      var cout1, cout2 = Seq[Bool]()
      for (i <- cols.indices) {
        val (s, c1, c2) = addOneColumn(cols(i) ++ cout1)
        columns_next(i) = s ++ cout2
        cout1 = c1
        cout2 = c2
      }

      val needReg = stages.contains(depth)
      val toNextLayer =
        if (needReg)
          // TODO: use 'RegEnable' instead
          columns_next.map(_.map(x => RegNext(x)))
        else
          columns_next

      addAll(toNextLayer, depth + 1)
    }
  }

  val stage:  Int = pipeAt.size
  val stages: Seq[Int] = pipeAt.sorted

  val b_sext = signExt(b.asUInt, width + 1)
  val bx2 = (b_sext << 1)(width, 0)
  val neg_b = (~b_sext).asUInt
  val neg_bx2 = (neg_b << 1)(width, 0)

  def makePartialProducts(i: Int, x: SInt): Seq[(Int, Bool)] = {
    val bb = MuxLookup(
      x.asUInt,
      0.U,
      Seq(
        1.S(3.W).asUInt -> b_sext,
        2.S(3.W).asUInt -> bx2,
        -1.S(3.W).asUInt -> neg_b,
        -2.S(3.W).asUInt -> neg_bx2,
      )
    )
    val plus_1 = MuxLookup(
      x.asUInt,
      0.U(2.W),
      Seq(
        -1.S(3.W).asUInt -> 1.U(2.W),
        -2.S(3.W).asUInt -> 2.U(2.W),
      )
    )
    val s = bb(width)
    val pp = i match {
      case 0 =>
        Cat(~s, s, s, bb)
      case n if (n == width - 1) || (n == width - 2) =>
        Cat(~s, bb)
      case _ =>
        Cat(1.U(1.W), ~s, bb)
    }
    Seq.tabulate(pp.getWidth) {j => (i + j, pp(j)) } ++ Seq.tabulate(2) {j => (i + j, plus_1(j))}
  }

  val columns_map = Booth.recode(width)(4)(a.asUInt)
    .zipWithIndex
    .flatMap{case (x, i) => makePartialProducts(2 * i, x)}
    .groupBy{_._1}

  val columns = Array.tabulate(2 * width) {i => columns_map(i).map(_._2)}

  val (sum, carry) = addAll(cols = columns, depth = 0)
  z := addition.prefixadder.apply(sumUpAdder)(sum, carry)(2 * width - 1, 0).asSInt
}
