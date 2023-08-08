package float

import chisel3._
import chisel3.util._
import division.srt.srt16._


/**
  * input
  * dividend = 0.1f  -> 1f +"00000" right extends to 32
  * divisor  = 0.1f  -> 1f +"00000" right extends to 32
  *
  * output = 0.01f or 0.1f, LSB 28bits effective
  * 0.01f: 28bits=01f f=sig=select(25,3)
  * 0.1f : 28bits=1f  f=sig=select(26,4)
  *
  * */
class DivFloat(expWidth: Int, sigWidth: Int) extends Module{
  val fpWidth = expWidth + sigWidth
  val calWidth = 28
  val input = IO(Flipped(DecoupledIO(new FloatDivInput(8, 24))))
  val output = IO(DecoupledIO(new FloatDivOutput(8, 24)))


  // for div, don't need to calculate rawExp
  val rawFloatDividend = rawFloatFromFN(expWidth,sigWidth,input.bits.dividend)
  val rawFloatDivisor  = rawFloatFromFN(expWidth,sigWidth,input.bits.divisor)


  // Data Path
  val sign = rawFloatDividend.sign ^ rawFloatDivisor.sign

  val dividendIn = Wire(UInt((fpWidth).W))
  val divisorIn = Wire(UInt((fpWidth).W))
  val signReg = RegEnable(sign, input.fire)
  val expRaw = Wire(UInt(expWidth.W))
  val expOutNext = Wire(UInt(expWidth.W))
  val expfinal = Wire(UInt(expWidth.W))



  // divIter logic

  dividendIn := Cat(1.U(1.W), rawFloatDividend.sig(sigWidth-2, 0), 0.U(expWidth.W))
  divisorIn  := Cat(1.U(1.W), rawFloatDivisor.sig(sigWidth-2, 0), 0.U(expWidth.W))

  val divModule = Module(new SRT16(fpWidth,fpWidth,fpWidth))
  divModule.input.bits.dividend := dividendIn
  divModule.input.bits.divider  := divisorIn
  divModule.input.bits.counter  := 8.U

  divModule.input.valid := input.valid
  input.ready  := divModule.input.ready
  output.valid := divModule.output.valid


  val needNormNext = Wire(Bool())
  val needNorm = RegEnable(needNormNext,input.fire)
  needNormNext := input.bits.divisor(sigWidth-2, 0) > input.bits.dividend(sigWidth-2, 0)

  // todo verify it
  expRaw := input.bits.dividend(fpWidth-1, sigWidth-1) - input.bits.divisor(fpWidth-1, sigWidth-1)
  expOutNext := expRaw + 127.U
  val expOutReg = RegEnable(expOutNext, 0.U(expWidth.W), input.fire)
  expfinal := expOutReg - needNorm


  val sigUnRound = Mux(needNorm, divModule.output.bits.quotient(calWidth-3, calWidth-sigWidth-1),divModule.output.bits.quotient(calWidth-2, calWidth-sigWidth))
  val rbits = Mux(needNorm, divModule.output.bits.quotient(calWidth-sigWidth-2)##1.U(1.W), divModule.output.bits.quotient(calWidth-sigWidth-1)##1.U(1.W))


  /** @todo exceptions
    *
    *       >256
    *       subnormal
    * */

  val invalidExec = false.B
  val infinitExec = false.B






  output.bits.result := RoundingUnit(
    signReg,
    expfinal,
    sigUnRound,
    rbits,
    consts.round_near_even,
    invalidExec,
    infinitExec)

  output.bits.sig := output.bits.result(22,0)
  output.bits.exp := expfinal


}
class FloatDivInput(expWidth: Int, sigWidth: Int) extends Bundle() {
  val dividend = UInt((expWidth + sigWidth).W)
  val divisor  = UInt((expWidth + sigWidth).W)
}

/** add 2 for rounding */
class FloatDivOutput(expWidth: Int, sigWidth: Int) extends Bundle() {
  val result = UInt((expWidth + sigWidth).W)
  val sig = UInt((sigWidth-1).W)
  val exp = UInt(expWidth.W)
}