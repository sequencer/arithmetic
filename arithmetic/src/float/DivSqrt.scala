package float

import chisel3._
import chisel3.util._
import division.srt.srt16._
import sqrt._
import chisel3.dontTouch

class DivSqrt(expWidth: Int, sigWidth: Int) extends Module{
  val fpWidth = expWidth + sigWidth
  val calWidth = 28
  val input = IO(Flipped(DecoupledIO(new DivSqrtInput(expWidth, sigWidth))))
  val output = IO(ValidIO(new DivSqrtOutput(expWidth, sigWidth)))

  val opSqrtReg = RegEnable(input.bits.sqrt, input.fire)
  val roundingModeReg = RegEnable(input.bits.roundingMode, input.fire)

  val rawA_S = rawFloatFromFN(expWidth, sigWidth, input.bits.a)
  val rawB_S = rawFloatFromFN(expWidth, sigWidth, input.bits.b)

  // Exceptions

  /** inf/inf and 0/0  => NaN out */
  val notSigNaNIn_invalidExc_S_div =
    (rawA_S.isZero && rawB_S.isZero) || (rawA_S.isInf && rawB_S.isInf)
  /** -Inf + -normal => NaN out */
  val notSigNaNIn_invalidExc_S_sqrt =
    !rawA_S.isNaN && !rawA_S.isZero && rawA_S.sign
  /** isSigNaNRawFloat detect signaling NaN */
  val majorExc_S =
    Mux(input.bits.sqrt,
      isSigNaNRawFloat(rawA_S) || notSigNaNIn_invalidExc_S_sqrt,
      isSigNaNRawFloat(rawA_S) || isSigNaNRawFloat(rawB_S) ||
        notSigNaNIn_invalidExc_S_div ||
        (!rawA_S.isNaN && !rawA_S.isInf && rawB_S.isZero)
    )

  /** all cases result in NaN output*/
  val isNaN_S =
    Mux(input.bits.sqrt,
      rawA_S.isNaN || notSigNaNIn_invalidExc_S_sqrt,
      rawA_S.isNaN || rawB_S.isNaN || notSigNaNIn_invalidExc_S_div
    )
  val isInf_S = Mux(input.bits.sqrt, rawA_S.isInf, rawA_S.isInf || rawB_S.isZero)
  val isZero_S = Mux(input.bits.sqrt, rawA_S.isZero, rawA_S.isZero || rawB_S.isInf)

  val majorExc_Z = RegEnable(majorExc_S,false.B,input.fire)
  val isNaN_Z    = RegEnable(isNaN_S,false.B,input.fire)
  val isInf_Z    = RegEnable(isInf_S,false.B,input.fire)
  val isZero_Z   = RegEnable(isZero_S,false.B,input.fire)

  /** invalid operation flag */
  val invalidExec = majorExc_Z &&  isNaN_Z

  /** DivideByZero flag */
  val infinitExec = majorExc_Z && !isNaN_Z

  val specialCaseA_S = rawA_S.isNaN || rawA_S.isInf || rawA_S.isZero
  val specialCaseB_S = rawB_S.isNaN || rawB_S.isInf || rawB_S.isZero
  val normalCase_S_div = !specialCaseA_S && !specialCaseB_S
  val normalCase_S_sqrt = !specialCaseA_S && !rawA_S.sign
  val normalCase_S = Mux(input.bits.sqrt, normalCase_S_sqrt, normalCase_S_div)
  val specialCase_S = !normalCase_S

  val fastValid = RegInit(false.B)
  fastValid := specialCase_S && input.fire

  // needNorm for div
  /** when B_sig > A_sig neednorm*/
  val needNormNext:Bool = (rawA_S.sig + (-rawB_S.sig))(24)
  val needNorm = RegEnable(needNormNext, input.fire)

  // sign
  val signNext = Mux(input.bits.sqrt, Mux(rawA_S.isZero, rawA_S.sign, false.B), rawA_S.sign ^ rawB_S.sign)
  val signReg = RegEnable(signNext, input.fire)

  // sqrt

  /** construct expForSqrt
    *
    * sExp first 2 bits
    * 00 -> 10 (subnormal)
    * 01 -> 11 (true exp negative)
    * 10 -> 00 (true exp positive)
    *
    */
  val expfirst2 = UIntToOH(rawA_S.sExp(expWidth, expWidth-1))
  /** @todo expfirst2(3) never happens */
  val expstart  = Mux1H(
    Seq(
      expfirst2(0) -> "b10".U,
      expfirst2(1) -> "b11".U,
      expfirst2(2) -> "b00".U,
      expfirst2(3) -> "b00".U
    )
  )
  /** exp for sqrt never underlow */
  val expForSqrt = Cat(expstart, rawA_S.sExp(expWidth - 2, 0)) >> 1
  val sqrtExpIsOdd = !rawA_S.sExp(0)
  val sqrtFractIn = Mux(sqrtExpIsOdd, Cat("b0".U(1.W), rawA_S.sig(sigWidth - 1, 0), 0.U(1.W)),
    Cat(rawA_S.sig(sigWidth - 1, 0), 0.U(2.W)))

  val SqrtModule = Module(new SquareRoot(2, 2, sigWidth+2, sigWidth+2))
  SqrtModule.input.bits.operand := sqrtFractIn
  SqrtModule.input.valid := input.valid && input.bits.sqrt && normalCase_S_sqrt

  val rbits_sqrt = SqrtModule.output.bits.result(1) ## (!SqrtModule.output.bits.zeroRemainder || SqrtModule.output.bits.result(0))
  val sigToRound_sqrt = SqrtModule.output.bits.result(24, 2)


  // div
  val fractDividendIn = Wire(UInt((fpWidth).W))
  val fractDivisorIn = Wire(UInt((fpWidth).W))
  fractDividendIn := Cat(1.U(1.W), rawA_S.sig(sigWidth - 2, 0), 0.U(expWidth.W))
  fractDivisorIn  := Cat(1.U(1.W), rawB_S.sig(sigWidth - 2, 0), 0.U(expWidth.W))

  val divModule = Module(new SRT16(fpWidth, fpWidth, fpWidth))
  divModule.input.bits.dividend := fractDividendIn
  divModule.input.bits.divider := fractDivisorIn
  divModule.input.bits.counter := 8.U
  divModule.input.valid := input.valid && !input.bits.sqrt && normalCase_S_div

  val sigToRound_div = Mux(needNorm, divModule.output.bits.quotient(calWidth - 3, calWidth - sigWidth - 1),
    divModule.output.bits.quotient(calWidth - 2, calWidth - sigWidth))
  val rbits_div = Mux(needNorm, divModule.output.bits.quotient(calWidth - sigWidth - 2) ## divModule.output.bits.reminder.orR,
    divModule.output.bits.quotient(calWidth - sigWidth - 1) ## divModule.output.bits.reminder.orR)


  // collect sig result
  val sigToRound = Mux(opSqrtReg, sigToRound_sqrt, sigToRound_div)
  val rbitsToRound = Mux(opSqrtReg, rbits_sqrt, rbits_div)

  // exp logic
  val expStoreNext,expToRound = Wire(UInt((expWidth+2).W))
  /** expStore
    *
    * for sqrt
    * expForSqrt(7,0) effective is 8bits, MSB is sign
    * extends 2 sign bit in MSB
    * expStoreNext = 10bits
    *
    * todo define it format, important
    */
  expStoreNext := Mux(input.bits.sqrt,
    Cat(expForSqrt(7),expForSqrt(7),expForSqrt(7,0)),
    (rawA_S.sExp-rawB_S.sExp).asUInt)
  val expStore = RegEnable(expStoreNext, 0.U((expWidth+2).W), input.fire)
  expToRound := Mux(opSqrtReg, expStore, expStore - needNorm)
  dontTouch(expToRound)

  dontTouch(rawA_S)
  dontTouch(rawB_S)
  dontTouch(rbits_div)

  val roundresult = RoundingUnit(
    signReg,
    expToRound,
    sigToRound,
    rbitsToRound,
    roundingModeReg,
    invalidExec,
    infinitExec,
    isNaN_Z,
    isInf_Z,
    isZero_Z)

  output.bits.result := roundresult(0)
  output.bits.exceptionFlags := roundresult(1)

  input.ready := divModule.input.ready && SqrtModule.input.ready
  output.valid := divModule.output.valid || SqrtModule.output.valid || fastValid
}


class DivSqrtInput(expWidth: Int, sigWidth: Int) extends Bundle() {
  val a = UInt((expWidth + sigWidth).W)
  val b = UInt((expWidth + sigWidth).W)
  val sqrt = Bool()
  val roundingMode = UInt(3.W)
}


class DivSqrtOutput(expWidth: Int, sigWidth: Int) extends Bundle() {
  val result = UInt((expWidth + sigWidth).W)
  val exceptionFlags = UInt(5.W)
}
