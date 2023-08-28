package float

import chisel3._
import chisel3.util._


/**
  * exp: 10bits SInt, MSB is sign
  * sig: 23bits
  */
class RoundingUnit extends Module{
  val input = IO(Input(new Bundle{
    val invalidExc = Bool() // overrides 'infiniteExc' and 'in'
    val infiniteExc = Bool() // overrides 'in' except for 'in.sign'
    val isInf  = Bool()
    val isZero = Bool()
    val isNaN  = Bool()
    val sig = UInt(23.W)
    val exp = SInt(10.W)
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

  val sigIncr = Wire(Bool())
  val expBiasedOut = Wire(UInt(8.W))
  val sig_afterInc = Wire(UInt(27.W))
  val sub_sigOut, common_subnormSigOut = Wire(UInt(23.W))
  val expInc = Wire(UInt(8.W))

  expBiasedOut := ((input.exp + 127.S)(7,0) + expInc).asUInt

  // control logic
  // set to 126 according to softfloat
  val exp_ForSub = (input.exp + 126.S(10.W))
  // for non subnormal case, Dist = 0
  val subnormDist = Mux(common_underflow, -exp_ForSub, 0.S(10.W))
  // todo why we have this case? IN IEEE754 or definded in Hardfloat?
  val common_totalUnderflow = subnormDist > 235.S

  /** subnormal logic
    *
    * roundMask is 26bits mask selecting all bits will be rounded, considering subnormal case
    *
    */
  val distGT32 = subnormDist(9,5).orR
  val allMask = ((-1).S(31.W) << 31 >> subnormDist(5,0))
  val distIn24And31 = allMask(6,0).orR
  val distGT24 = (distGT32 || distIn24And31) && common_underflow
  val roundMask = Mux(!distGT24, Reverse(allMask(30,7)) ## 3.U(2.W), 0.U(26.W))

  val shiftedRoundMask = Mux(!distGT24, 0.U(1.W) ## roundMask >> 1 , BigInt(-1).S(26.W).asUInt)
  /** select the guard bit need to be  rounded */
  val roundPosMask = ~shiftedRoundMask & roundMask

  val adjustedSig = Cat(1.U(1.W), input.sig, input.rBits)
  val roundPosBit = (adjustedSig & roundPosMask).orR
  /** Any bits is one after guard bit  => sticky bit */
  val anyRoundExtra = (adjustedSig & shiftedRoundMask).orR
  /** Any bits is one containing guard bit */
  val anyRound = roundPosBit || anyRoundExtra

  val lastBitMask = (roundPosMask<<1.U)(25,0)
  val lastBit = (adjustedSig & lastBitMask ).orR

  val distEQ24 = roundPosMask(25) && !roundPosMask(24,0).orR

  val rbits : UInt= Cat(roundPosBit, anyRoundExtra)

  sigIncr := (roundingMode_near_even && (rbits.andR || (lastBit && rbits==="b10".U))) ||
    (roundingMode_min && input.sign && rbits.orR) ||
    (roundingMode_max && !input.sign && rbits.orR) ||
    (roundingMode_near_maxMag && rbits(1))

  /** sig_afterInc doesn;t cover distEQ24 */
  sub_sigOut := Mux(distGT24 || distEQ24 ,Mux(sigIncr,1.U(26.W), 0.U(26.W)),(sig_afterInc >> subnormDist(4,0))(24,2))
  /** when subnormDist===1.S, there may be expInc */
  expInc := sig_afterInc(26)  && (!common_underflow || subnormDist === 1.S )
  common_subnormSigOut := Mux(common_totalUnderflow, 0.U ,sub_sigOut )

  val sigIncrement = Mux(sigIncr,lastBitMask, 0.U(26.W))
  sig_afterInc := adjustedSig +& sigIncrement

  /** Exceptions output */
  val isNaNOut = input.invalidExc || input.isNaN
  val notNaN_isSpecialInfOut = (input.infiniteExc || input.isInf) && (!input.invalidExc) && (!input.isNaN)
  val notNaN_isZero = input.isZero && !isNaNOut
  val commonCase = !isNaNOut && !notNaN_isSpecialInfOut && !input.isZero

  val overflow  = commonCase && common_overflow
  val underflow = commonCase && (common_underflow && rbits.orR)
  val inexact   = overflow || (commonCase && common_inexact)

  val isZero = input.isZero && underflow

  val overflowSele = roundingMode_min ## roundingMode_max ## roundingMode_toZero ## (roundingMode_near_even || roundingMode_near_maxMag)

  val common_infiniteOut = Mux1H(
    Seq(
      overflowSele(0) -> Cat(input.sign, "h7F800000".U(31.W)),
      overflowSele(1) -> Cat(input.sign, "h7F7FFFFF".U(31.W)),
      overflowSele(2) -> Mux(input.sign, "hFF7FFFFF".U(32.W), "h7F800000".U(32.W)),
      overflowSele(3) -> Mux(input.sign, "hFF800000".U(32.W), "h7F7FFFFF".U(32.W)),
    )
  )

  /** qNaN in Spike */
  val quietNaN = "h7FC00000".U

  val infiniteOut = Cat(input.sign, "h7F800000".U)
  val zeroOut = Cat(input.sign, 0.U(31.W))
  val outSele1H = commonCase ## notNaN_isSpecialInfOut ## isNaNOut ## notNaN_isZero

  /** common_overflow = input.exp > 127.S
    *
    * @todo opt it using hardfloat methods?
    */
  common_overflow  := input.exp(8,7).orR && !input.exp(9)
  common_underflow := exp_ForSub(9)
  common_inexact   := anyRound

  val common_sigOut = sig_afterInc(24,2)
  val common_expOut = expBiasedOut


  val common_out = Mux(common_overflow, common_infiniteOut,
    Mux(common_underflow, input.sign ## expInc ## common_subnormSigOut,
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
  def apply(sign: Bool, exp: SInt, sig: UInt, rbits: UInt, rmode: UInt, invalidExc: Bool, infiniteExc: Bool, isNaN: Bool, isInf: Bool, isZero: Bool): Vec[UInt] = {

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


