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
  def addOneColumn(col: Seq[Bool], cin: Seq[Bool]): (Seq[Bool], Seq[Bool], Seq[Bool]) =
    col.size match {
      case 1 => // do nothing
        (col ++ cin, Seq.empty[Bool], Seq.empty[Bool])
      case 2 =>
        val c22 = addition.csa.c22(VecInit(col)).map(_.asBool)
        (c22(0) +: cin, Seq.empty[Bool], Seq(c22(1)))
      case 3 =>
        val c32 = addition.csa.c32(VecInit(col)).map(_.asBool)
        (c32(0) +: cin, Seq.empty[Bool], Seq(c32(1)))
      case 4 =>
        val c53 = addition.csa.c53(VecInit(col :+ cin.headOption.getOrElse(false.B))).map(_.asBool)
        (Seq(c53(0)) ++ (if (cin.nonEmpty) cin.drop(1) else Nil), Seq(c53(1)), Seq(c53(2)))
      case _ =>
        val cin_1 = if (cin.nonEmpty) Seq(cin.head) else Nil
        val cin_2 = if (cin.nonEmpty) cin.drop(1) else Nil
        val (s_1, c_1_1, c_1_2) = addOneColumn(col.take(4), cin_1)
        val (s_2, c_2_1, c_2_2) = addOneColumn(col.drop(4), cin_2)
        (s_1 ++ s_2, c_1_1 ++ c_2_1, c_1_2 ++ c_2_2)
    }

  def addAll(cols: Array[Seq[Bool]], depth: Int): (UInt, UInt) = {
    if (cols.map(_.size).max <= 2) {
      val sum = Cat(cols.map(_.head).reverse)
      // TODO: remove var.
      var k = 0
      while (cols(k).size == 1) k = k + 1
      val carry = Cat(cols.drop(k).map(_(1)).reverse)
      (sum, Cat(carry, 0.U(k.W)))
    } else {
      val columns_next = Array.fill(2 * width)(Seq[Bool]())
      var cout1, cout2 = Seq[Bool]()
      for (i <- cols.indices) {
        val (s, c1, c2) = addOneColumn(cols(i), cout1)
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

  // TODO: UInt/SInt configurable?

  // TODO: why a and b is asymmetric?
  // TODO: should we switch to SInt?
  val b_sext = signExt(b, width + 1)
  val bx2 = (b_sext << 1)(width, 0)
  val neg_b = (~b_sext).asUInt
  val neg_bx2 = (neg_b << 1)(width, 0)

  // TODO: use Seq.tabulate()().scan() to get rid of mutable here.
  val columns: Array[Seq[Bool]] = Array.fill(2 * width)(Seq())
  // TODO remove var
  var last_x = WireInit(0.U(3.W))
  for (i <- Range(0, width, 2)) {
    val x = if (i == 0)
              Cat(a(1, 0), 0.U(1.W))
            else if (i + 1 == width)
              signExt(a(i, i - 1), 3)
            else
              a(i + 1, i - 1)
    val pp_temp = MuxLookup(
      x,
      0.U,
      Seq(
        1.U -> b_sext,
        2.U -> b_sext,
        3.U -> bx2,
        4.U -> neg_bx2,
        5.U -> neg_b,
        6.U -> neg_b
      )
    )
    val s = pp_temp(width)
    val t = MuxLookup(
      last_x,
      0.U(2.W),
      Seq(
        4.U -> 2.U(2.W),
        5.U -> 1.U(2.W),
        6.U -> 1.U(2.W)
      )
    )
    last_x = x
    val (pp, weight) = i match {
      case 0 =>
        (Cat(~s, s, s, pp_temp), 0)
      case n if (n == width - 1) || (n == width - 2) =>
        (Cat(~s, pp_temp, t), i - 2)
      case _ =>
        (Cat(1.U(1.W), ~s, pp_temp, t), i - 2)
    }
    println(pp_temp, pp)
    for (j <- columns.indices) {
      if (j >= weight && j < (weight + pp.getWidth)) {
        columns(j) = columns(j) :+ pp(j - weight)
      }
    }
    println(s"$weight -> ${weight + pp.getWidth}")
  }
  println(columns.map(_.length).mkString("Array(", ", ", ")"))
  val (sum, carry) = addAll(cols = columns, depth = 0)
  z := addition.prefixadder.apply(sumUpAdder)(sum, carry)
}
