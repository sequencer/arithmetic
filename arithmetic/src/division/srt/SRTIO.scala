package division.srt

import chisel3._
import chisel3.util.log2Ceil

class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W) //.***********
  val divider = UInt(dividerWidth.W) //.1**********
  val counter = UInt(log2Ceil(n).W) //the width of quotient.
}

class SRTOutput(reminderWidth: Int, quotientWidth: Int) extends Bundle {
  val reminder = UInt(reminderWidth.W)
  val quotient = UInt(quotientWidth.W)
}
