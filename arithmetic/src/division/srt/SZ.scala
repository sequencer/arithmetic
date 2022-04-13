package division.srt

import chisel3._
import addition.prefixadder._
import addition.prefixadder.common.{BrentKungSum}

class SZInput(rWidth: Int) extends Bundle {
  val partialReminderCarry: UInt = UInt(rWidth.W)
  val partialReminderSum:   UInt = UInt(rWidth.W)
}

class SZOutput extends Bundle {
  // val selectedQuotient: UInt = UInt((log2Ceil(n)+1).W)
  val sign: Bool = Bool()
  val zero: Bool = Bool()
}

class SZ(rWidth: Int, prefixSum: PrefixSum = BrentKungSum) extends Module {
  val input = IO(Input(new SZInput(rWidth)))
  val output = IO(Output(new SZOutput()))

  //controlpath

  //datapath
  // csa(ws,wc,-2^-b) => Seq[(Bool,Bool)]
  val ws = input.partialReminderCarry.asBools
  val wc = input.partialReminderSum.asBools
  val psc: Seq[(Bool, Bool)] = ws.zip(wc).map { case (s, c) => (~(s ^ c), (s | c)) }

  // call the prefixtree to associativeOp
  val pairs: Seq[(Bool, Bool)] = prefixSum.zeroLayer(psc.map(_._1) :+ false.B, false.B +: psc.map(_._2))
  val pgs:   Vector[(Bool, Bool)] = prefixSum(pairs)
  val gs:    Vector[Bool] = pgs.map(_._2)
  val ps:    Vector[Bool] = pgs.map(_._1)

  // maybe have a problem.
  output.zero := VecInit(ps).asUInt.head(1)
  output.sign := (output.zero ^ VecInit(gs).asUInt.head(1)) & (~output.zero)
}
