package sqrt

import chisel3.{util, _}
import chisel3.util._
import division.srt.SRTTable
import division.srt.srt4.{OTF, QDS}
import utils.leftShift

/** SquareRoot
  *
  * {{{
  * oprand = 0.1xxxxx > 1/2 , input.bits.oprand  = 1xxxx
  * result = 0.1xxxxx > 1/2 , output.bits.result = 1xxxxx
  * }}}
  *
  *
  * @param outputWidth decide width for result , true result is .xxxxxx, need to be inputwidth + 2
  */
class SquareRoot(
  radixLog2:   Int,
  a:           Int,
  inputWidth:  Int,
  outputWidth: Int)
    extends Module {
  val input = IO(Flipped(DecoupledIO(new SquareRootInput(inputWidth: Int, outputWidth: Int))))
  val output = IO(DecoupledIO(new SquareRootOutput(outputWidth)))

  /** width for partial result and csa */
  val wlen = inputWidth + 2

  /** W[j] = xx.xxxxxxxx
    *
    * width = 2 + inputwidth
    */
  val partialResultCarryNext, partialResultSumNext = Wire(UInt(wlen.W))
  /** S[j] = .xxxxxxxx
    *
    * effective bits number depends on counter, 2n+1
    *
    * effective length grows from LSB and depends on j
    * */
  val resultOriginNext, resultMinusOneNext = Wire(UInt((outputWidth).W))
  val counterNext = Wire(UInt(log2Ceil(outputWidth).W))

  // Control
  // sign of Cycle, true -> (counter === 0.U)
  val isLastCycle, enable: Bool = Wire(Bool())
  val occupiedNext = Wire(Bool())
  val occupied = RegNext(occupiedNext, false.B)
  occupiedNext := input.fire || (!isLastCycle && occupied)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(outputWidth).W), enable)


  /** Data REG */
  val resultOrigin       = RegEnable(resultOriginNext,       0.U((outputWidth).W), enable)
  val resultMinusOne     = RegEnable(resultMinusOneNext,     0.U((outputWidth).W), enable)
  val partialResultCarry = RegEnable(partialResultCarryNext, 0.U(wlen.W),          enable)
  val partialResultSum   = RegEnable(partialResultSumNext,   0.U(wlen.W),          enable)



  /** todo :  later don't fix it ? */
  isLastCycle := counter === (outputWidth/2).U
  output.valid := occupied && isLastCycle
  input.ready := !occupied
  enable := input.fire || !isLastCycle

  /** rW[j] = xxxx.xxxxxxxx
    *
    * first 7 bits for QDS
    *
    */
  val shiftSum, shiftCarry = Wire(UInt((inputWidth + 4).W))
  shiftSum   := partialResultSum   << 2
  shiftCarry := partialResultCarry << 2

  /** todo later parameterize it */
  val rtzYWidth = 7
  val rtzSWidth = 4
  val ohWidth = 5

  val firstIter = counter === 0.U

  /** S[j] = x.xxxxxxxx
    *
    * For constructing resultForQDS
    * shift effective bits's MSB to MSB
    *
    * width = outwidth + 1
    */
  val resultOriginRestore = (resultOrigin << (outputWidth.U - (counter << 1).asUInt))(outputWidth, 0)

  /** todo: later opt it with p341
    *
    * seems resultOriginRestore(outputWidth) can't be 1?
    * */
  val resultForQDS = Mux(
    firstIter,
    "b101".U,
    Mux(resultOriginRestore(outputWidth), "b111".U, resultOriginRestore(outputWidth - 2, outputWidth - 4))
  )

  /** todo later param it */
  val tables: Seq[Seq[Int]] = SRTTable(1 << radixLog2, a, 4, 4).tablesToQDS

  val selectedQuotientOH: UInt =
    QDS(rtzYWidth, ohWidth, rtzSWidth - 1, tables, a)(
      shiftSum.head(rtzYWidth),
      shiftCarry.head(rtzYWidth),
      resultForQDS //.1********* -> 1*** -> ***
    )

  // On-The-Fly conversion
  val otf = OTF(radixLog2, outputWidth + 1, ohWidth, a)(resultOrigin, resultMinusOne, selectedQuotientOH)

  /** p339 */
  val formationForIter = Mux1H(
    Seq(
      selectedQuotientOH(0) -> (resultMinusOne << 4 | "b1100".U),
      selectedQuotientOH(1) -> (resultMinusOne << 3 | "b111".U),
      selectedQuotientOH(2) -> 0.U,
      selectedQuotientOH(3) -> (~resultOrigin << 3 | "b111".U),
      selectedQuotientOH(4) -> (~resultOrigin << 4 | "b1100".U)
    )
  )

  val formationFinal = Wire(UInt((inputWidth + 3).W))
  formationFinal := formationForIter << (inputWidth - 2) >> (counter << 1)

  /** csa width : inputwidth + 2 */
  val csa: Vec[UInt] = addition.csa.c32(
    VecInit(
      shiftSum(inputWidth + 1, 0),
      shiftCarry(inputWidth + 1, 0),
      formationFinal(inputWidth + 1, 0)
    )
  )

  val remainderFinal = partialResultSum + partialResultCarry
  val needCorrect: Bool = remainderFinal(outputWidth-1).asBool

  /** w[0] = oprand - 1.U, oprand > 1/2 */
  val initSum = Cat("b11".U, input.bits.operand)

  /** init S[0] = 1 */
  resultOriginNext       := Mux(input.fire, 1.U, otf(0))
  resultMinusOneNext     := Mux(input.fire, 0.U, otf(1))
  partialResultSumNext   := Mux(input.fire, initSum, csa(1))
  partialResultCarryNext := Mux(input.fire, 0.U, csa(0) << 1)
  counterNext := Mux(input.fire, 0.U, counter + 1.U)

  output.bits.result := Mux(needCorrect, resultMinusOne, resultOrigin)

}
