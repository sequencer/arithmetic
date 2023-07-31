package sqrt

import chisel3._
import chisel3.util._

class SquareRootInput(inputWidth: Int, outputWidth: Int) extends Bundle{
  val operand = UInt(inputWidth.W)
  val counter = UInt(log2Ceil(outputWidth).W)
}

/** 0.1**** = 0.resultOrigin */
class SquareRootOutput(outputWidth: Int) extends Bundle{
  val result = UInt((outputWidth).W)
}
