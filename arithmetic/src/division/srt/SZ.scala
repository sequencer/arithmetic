package division.srt

import chisel3._
import addition.prefixadder._
import addition.prefixadder.common.{BrentKungSum}

class SZInput(rWidth: Int) extends Bundle {
  val partialReminderCarry: UInt = UInt(rWidth.W)
  val partialReminderSum:   UInt = UInt(rWidth.W)
}

class SZOutput(rWidth: Int) extends Bundle {
  val sign:      Bool = Bool()
  val zero:      Bool = Bool()
  val remainder: UInt = UInt((rWidth).W)
}

class SZ(rWidth: Int, prefixSum: PrefixSum = BrentKungSum) extends Module {
  val input = IO(Input(new SZInput(rWidth)))
  val output = IO(Output(new SZOutput(rWidth)))
  //controlpath

  //datapath
  // csa(ws,wc,-2^-b) => Seq[(Bool,Bool)]
  // drop signed bits
  // prefixtree by group
  val ws = input.partialReminderCarry.asBools
  val wc = input.partialReminderSum.asBools
  val psc: Seq[(Bool, Bool)] = ws.zip(wc).map { case (s, c) => (!(s ^ c), (s | c)) }

  // call the prefixtree to associativeOp and compute last remainder
  val pairs: Seq[(Bool, Bool)] = prefixSum.zeroLayer(psc.map(_._1) :+ false.B, false.B +: psc.map(_._2))
  val pgs:   Vector[(Bool, Bool)] = prefixSum(pairs)
  val ps:    Vector[Bool] = pgs.map(_._1)
  val gs:    Vector[Bool] = pgs.map(_._2)

  val a:   Vector[Bool] = false.B +: gs
  val b:   Seq[Bool] = pairs.map(_._1) :+ false.B
  val sum: Seq[Bool] = a.zip(b).map { case (p, c) => p ^ c }

  // maybe have a problem.
  output.zero := VecInit(ps).asUInt.head(1)
  output.sign := (pairs(pairs.length - 1)._1 ^ gs(gs.length - 2)) & (!output.zero)
  output.remainder := VecInit(sum).asUInt
}
