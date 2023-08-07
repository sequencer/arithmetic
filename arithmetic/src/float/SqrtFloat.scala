package float

import chisel3._
import chisel3.util._
import sqrt._

class SqrtFloat(expWidth: Int, sigWidth: Int) extends Module{
  val input = IO(Flipped(DecoupledIO(new FloatSqrtInput(expWidth, sigWidth))))
  val output = IO(DecoupledIO(new FloatSqrtOutput(expWidth, sigWidth)))
  val debug = IO(Output(new Bundle() {
    val fractIn = UInt(26.W)
  }))

  /** Data path */
  val rawFloatIn = rawFloatFromFN(expWidth,sigWidth,input.bits.oprand)
  val adjustedExp = Cat(rawFloatIn.sExp(expWidth-1), rawFloatIn.sExp(expWidth-1, 0))

  /** {{{
    * expLSB   rawExpLSB    Sig             SigIn     expOut
    *      0           1    1.xxxx>>2<<1    1xxxx0    rawExp/2 +1 + bias
    *      1           0    1.xxxx>>2       01xxxx    rawExp/2 +1 + bias
    *}}}
    */
  val expOutNext = Wire(UInt(expWidth.W))
  expOutNext := adjustedExp(expWidth,1) + 127.U
  val expOut = RegEnable(expOutNext, 0.U(expWidth.W), input.fire)
  val fractIn = Mux(input.bits.oprand(sigWidth-1), Cat("b0".U(1.W),rawFloatIn.sig(sigWidth-1, 0),0.U(1.W)),
    Cat(rawFloatIn.sig(sigWidth-1, 0),0.U(2.W)))

  val SqrtModule = Module(new SquareRoot(2, 2, 26, 26))
  SqrtModule.input.valid := input.valid
  SqrtModule.input.bits.operand := fractIn
  SqrtModule.output.ready := output.ready

  val rbits = SqrtModule.output.bits.result(1,0) ## (!SqrtModule.output.bits.zeroRemainder)
  val sigRound = SqrtModule.output.bits.result(24,2)

  input.ready := SqrtModule.input.ready
  output.bits.result := RoundingUnit(input.bits.oprand(expWidth + sigWidth-1) ,expOut,sigRound,rbits,consts.round_near_even)
  output.bits.sig := SqrtModule.output.bits.result
  output.bits.exp := expOut
  output.valid := SqrtModule.output.valid

  debug.fractIn := fractIn

}

class FloatSqrtInput(expWidth: Int, sigWidth: Int) extends Bundle() {
  val oprand = UInt((expWidth + sigWidth).W)
}

/** add 2 for rounding*/
class FloatSqrtOutput(expWidth: Int, sigWidth: Int) extends Bundle() {
  val result = UInt((expWidth + sigWidth).W)
  val sig = UInt((sigWidth+2).W)
  val exp = UInt(expWidth.W)

//  val exceptionFlags = UInt(5.W)
}

