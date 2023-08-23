package float

import chisel3._
import chisel3.util._


/**
  * input.rbits = 2bits + sticky bit
  *
  * leave
  *
  * output is subnormal
  *
  * */
class RoundingUnit extends Module{
  val input = IO(Input(new Bundle{
    val invalidExc = Bool() // overrides 'infiniteExc' and 'in'
    val infiniteExc = Bool() // overrides 'in' except for 'in.sign'
    val isInf  = Bool()
    val isZero = Bool()
    val isNaN  = Bool()
    val sig = UInt(23.W)
    val exp = UInt(10.W)
    val rBits = UInt(2.W)
    val sign = Bool()
    val roundingMode = UInt(5.W)
  }))
  val output = IO(Output(new Bundle{
    val data = UInt(32.W)
    val exceptionFlags = Output(Bits(5.W))
  }))

  val roundingMode_near_even   = (input.roundingMode === consts.round_near_even)
  val roundingMode_toZero      = (input.roundingMode === consts.round_minMag)
  val roundingMode_min         = (input.roundingMode === consts.round_min)
  val roundingMode_max         = (input.roundingMode === consts.round_max)
  val roundingMode_near_maxMag = (input.roundingMode === consts.round_near_maxMag)


  val common_overflow = Wire(Bool())
  val common_underflow = Wire(Bool())
  val common_inexact  = Wire(Bool())
  val common_subnorm  = Wire(Bool())



  val sigPlus = Wire(UInt(23.W))
  val expBiasPlus = Wire(UInt(8.W))
  val sigIncr = Wire(Bool())
  val expIncr = Wire(Bool())
  val expBiased = Wire(UInt(8.W))

  /** normal case */

  /** todo later use Mux?*/
  sigIncr := (roundingMode_near_even && input.rBits(1) && input.rBits(0)) ||
    (roundingMode_min &&  input.sign && input.rBits.orR) ||
    (roundingMode_max && !input.sign && input.rBits.orR) ||
    (roundingMode_near_maxMag && input.rBits(1) && input.rBits(0))

  sigPlus := input.sig + sigIncr

  /** for sig = all 1 and sigIncr*/
  expIncr := input.sig.andR && sigIncr
  expBiased := (input.exp.asSInt + 127.S)(7,0).asUInt
  expBiasPlus := (input.exp.asSInt + 128.S)(7,0).asUInt

  val exp_BiasForSub = (input.exp.asSInt + 127.S) + expIncr.asSInt
  val subnormDist = -exp_BiasForSub + 1.S
  common_subnorm := exp_BiasForSub(9)
  val common_subnormSigOut = (Cat(1.U(1.W), input.sig) >> subnormDist.asUInt)(22,0)
  dontTouch(exp_BiasForSub)
  dontTouch(subnormDist)
  dontTouch(common_subnorm)
  dontTouch(common_subnormSigOut)

  // Exceptions
  val isNaNOut = input.invalidExc || input.isNaN
  val notNaN_isSpecialInfOut = input.infiniteExc || input.isInf
  val commonCase = !isNaNOut && !notNaN_isSpecialInfOut && !input.isZero

  val overflow = commonCase && common_overflow
  val underflow = commonCase && common_underflow
  val inexact = overflow || (commonCase && common_inexact)

  val isZero = input.isZero && underflow

//  val overflowSele = UIntToOH(roundingMode_min ## roundingMode_max ## roundingMode_toZero ## (roundingMode_near_even || roundingMode_near_maxMag))
//
//  val infiniteOut = Mux1H(
//    Seq(
//      overflowSele(0) -> Cat(input.sign, "h7F80000".U(31.W)),
//      overflowSele(1) -> Cat(input.sign, "h7F7FFFF".U(31.W)),
//      overflowSele(2) -> Cat(0.U(1.W), "h7F7FFFF".U(31.W)),
//      overflowSele(3) -> Cat(1.U(1.W), "h7F7FFFF".U(31.W)),
//    )
//  )


  // exception data with Spike
  val quietNaN = "h7FC00000".U

  val infiniteOut = Cat(input.sign, "h7F800000".U)
  val zeroOut = Cat(input.sign, 0.U(31.W))
  val outSele1H = commonCase ## notNaN_isSpecialInfOut ## isNaNOut ## input.isZero

  /** @todo opt it */
  common_overflow := exp_BiasForSub > 254.S
  common_underflow := common_subnorm
  common_inexact := input.rBits.orR

  val common_sigOut = Mux(sigIncr, sigPlus, input.sig)
  val common_expOut = Mux(expIncr, expBiasPlus, expBiased)
  dontTouch(common_expOut)
  dontTouch(common_underflow)
  dontTouch(common_overflow)


  val common_out = Mux(common_overflow, infiniteOut,
    Mux(common_subnorm, input.sign ## 0.U(8.W) ## common_subnormSigOut,
      input.sign ## common_expOut ## common_sigOut))

  output.data := Mux1H(Seq(
    outSele1H(0) -> zeroOut,
    outSele1H(1) -> quietNaN,
    outSele1H(2) -> infiniteOut,
    outSele1H(3) -> common_out)
  )


  output.exceptionFlags := input.invalidExc ## input.infiniteExc ## overflow ## underflow ## inexact

}

object RoundingUnit {
  def apply(sign: Bool, exp: UInt, sig: UInt, rbits: UInt, rmode: UInt, invalidExc: Bool, infiniteExc: Bool, isNaN: Bool, isInf: Bool, isZero: Bool): Vec[UInt] = {

    val rounder = Module(new RoundingUnit)
    rounder.input.sign := sign
    rounder.input.sig := sig
    rounder.input.exp := exp
    rounder.input.rBits := rbits
    rounder.input.roundingMode := rmode
    rounder.input.invalidExc := invalidExc
    rounder.input.infiniteExc := infiniteExc
    rounder.input.isInf := isInf
    rounder.input.isZero := isZero
    rounder.input.isNaN := isNaN
    VecInit(rounder.output.data, rounder.output.exceptionFlags)
  }

}


