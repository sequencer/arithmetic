package sqrt

import chisel3._
import chisel3.util._

class SquareRootInput(inputWidth: Int, outputWidth: Int) extends Bundle{
  val operand = UInt(inputWidth.W)
}

/** 0.1**** = 0.resultOrigin */
class SquareRootOutput(outputWidth: Int) extends Bundle{
  val result = UInt((outputWidth).W)
}

class QDSInput(rWidth: Int, partialDividerWidth: Int) extends Bundle {
  val partialReminderCarry: UInt = UInt(rWidth.W)
  val partialReminderSum:   UInt = UInt(rWidth.W)
  /** truncated divisor without the most significant bit  */
  val partialDivider: UInt = UInt(partialDividerWidth.W)
}

class QDSOutput(ohWidth: Int) extends Bundle {
  val selectedQuotientOH: UInt = UInt(ohWidth.W)
}
