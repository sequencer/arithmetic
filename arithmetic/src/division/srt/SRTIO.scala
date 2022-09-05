package division.srt

import chisel3._
import chisel3.util.log2Ceil
// SRTIO
class SRTInput(dividendWidth: Int, dividerWidth: Int, n: Int) extends Bundle {
  val dividend = UInt(dividendWidth.W) //.***********
  val divider = UInt(dividerWidth.W) //.1**********
  val counter = UInt(log2Ceil(n).W) //the width of quotient.
}

class SRTOutput(reminderWidth: Int, quotientWidth: Int) extends Bundle {
  val reminder = UInt(reminderWidth.W)
  val quotient = UInt(quotientWidth.W)
}

//OTFIO
class OTFInput(qWidth: Int, ohWidth: Int) extends Bundle {
  val quotient = UInt(qWidth.W)
  val quotientMinusOne = UInt(qWidth.W)
  val selectedQuotientOH = UInt(ohWidth.W)
}

class OTFOutput(qWidth: Int) extends Bundle {
  val quotient = UInt(qWidth.W)
  val quotientMinusOne = UInt(qWidth.W)
}

// QDSIO
class QDSInput(rWidth: Int, partialDividerWidth: Int) extends Bundle {
  val partialReminderCarry: UInt = UInt(rWidth.W)
  val partialReminderSum:   UInt = UInt(rWidth.W)
  val partialDivider:       UInt = UInt(partialDividerWidth.W)
}

class QDSOutput(ohWidth: Int) extends Bundle {
  val selectedQuotientOH: UInt = UInt(ohWidth.W)
}
