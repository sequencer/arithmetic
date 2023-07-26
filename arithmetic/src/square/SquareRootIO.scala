package square

import chisel3._
import chisel3.util._

//class OTFInput(qWidth: Int, ohWidth: Int) extends Bundle {
//  val resultOrigin = UInt(qWidth.W)
//  val resultMinusOne = UInt(qWidth.W)
//  val selectedQuotientOH = UInt(ohWidth.W)
//}
//class OTFOutput(qWidth: Int) extends Bundle {
//  val resultOrigin = UInt(qWidth.W)
//  val resultMinusOne = UInt(qWidth.W)
//}


class SquareRootInput(inputWidth: Int, outputWidth: Int) extends Bundle{
  val operand = UInt(inputWidth.W)
  val counter = UInt(log2Ceil(outputWidth).W)
}

/** 0.1**** = 0.resultOrigin */
class SquareRootOutput(outputWidth: Int) extends Bundle{
  val result = UInt((outputWidth).W)
}
