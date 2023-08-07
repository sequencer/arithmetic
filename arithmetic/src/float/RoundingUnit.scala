package float

import chisel3._
import chiseltest._
import utest._

import scala.util.Random
import scala.math._

/**
  * input.rbits = 2bits + sticky bit
  *
  * */
class RoundingUnit extends Module{
  val input = IO(Input(new Bundle{
//    val invalidExc = Bool() // overrides 'infiniteExc' and 'in'
//    val infiniteExc = Bool() // overrides 'in' except for 'in.sign'
    val sig = UInt(23.W)
    val exp = UInt(8.W)
    val rBits = UInt(3.W)
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

  val sigPlus = Wire(UInt(23.W))
  val expPlus = Wire(UInt(8.W))
  val sigIncr = Wire(Bool())
  val expIncr = Wire(Bool())

  /** normal case */

  /** todo later use Mux?*/
  sigIncr := (roundingMode_near_even && input.rBits(2) && input.rBits(1,0).orR) ||
    (roundingMode_min &&  input.sign && input.rBits.orR) ||
    (roundingMode_max && !input.sign && input.rBits.orR) ||
    (roundingMode_near_maxMag && input.rBits.orR)

  sigPlus := input.sig + sigIncr

  /** for sig = all 1 and sigIncr*/
  expIncr := input.sig.andR && sigIncr
  expPlus := input.exp + expIncr

  val expOverflow = input.exp.andR && expIncr

  val sigOut = Mux(sigIncr, sigPlus, input.sig)
  val expOut = Mux(expIncr, expPlus, input.exp)

  output.data := input.sign ## expOut ## sigOut
  output.exceptionFlags := 0.U

}

object RoundingUnit {
  def apply(sign: Bool, exp:UInt, sig: UInt, rbits:UInt, rmode: UInt): UInt = {

    val rounder = Module(new RoundingUnit)
    rounder.input.sign := sign
    rounder.input.sig := sig
    rounder.input.exp := exp
    rounder.input.rBits := rbits
    rounder.input.roundingMode := rmode
    rounder.output.data
  }

}

