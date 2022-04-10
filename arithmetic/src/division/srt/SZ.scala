package division.srt

import chisel3._

class SZInput extends Bundle {
  val partialReminderCarry: UInt = UInt(rWidth.W)
  val partialReminderSum:   UInt = UInt(rWidth.W)
}

class SZOutput extends Bundle {
  // val selectedQuotient: UInt = UInt((log2Ceil(n)+1).W)
  val sign: Bool = Bool()
  val zero: Bool = Bool()
}

class SZ(rWidth: Int) extends Module{
    val input = IO(Input(new SZInput(rWidth)))
    val output= IO(Output(new SZOutput()))

    //controlpath

    //datapath
    val ws = input.partialReminderCarry.asBools
    val wc = input.partialReminderSum.asBools

    val psc: Seq[(Bool, Bool)]= ws.zip(wc).map{case(s,c) =>(~(s ^ c), (s | c))}
    val ps:  Seq[Bool] = psc.map(_._1) +: false.B
    val pc:  Seq[Bool] = false.B +: psc.map(_._2) 
    val p:   Seq[Bool] = ps.zip(pc){case(s, c) => s ^ c}

    output.zero := p.andR
    output.sign := (p.asUInt.head(1) ^ ???) & (~output.zero)

}