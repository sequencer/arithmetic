package square

import chisel3.{util, _}
import chisel3.util._
import division.srt.SRTTable
import division.srt.srt4.{OTF, QDS}
import utils.leftShift

/** Squre
  *
  * @param outputWidth decide width for result , true result is .xxxxxx
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

  /** todo: verify it, switch to csa */
  val resultZero = input.bits.operand - 1.U

  /** W[j] = xx.xxxxxxxx
    *
    * width = 2 + inputwidth
    */
  val partialResultCarryNext, partialResultSumNext = Wire(UInt(wlen.W))

  /** S[j] = .xxxxxxxx
    *
    * point position depends on j
    *
    * grow from LSB
    */
  val resultOriginNext, resultMinusOneNext = Wire(UInt((outputWidth).W))
  val counterNext = Wire(UInt(log2Ceil(outputWidth).W))

  // Control
  // sign of Cycle, true -> (counter === 0.U)
  val isLastCycle, enable: Bool = Wire(Bool())
  val occupiedNext = Wire(Bool())
  val occupied = RegNext(occupiedNext, false.B)
  occupiedNext := input.fire || (!isLastCycle && occupied)

  // State
  // because we need a CSA to minimize the critical path
  val partialResultCarry = RegEnable(partialResultCarryNext, 0.U(wlen.W), enable)
  val partialResultSum = RegEnable(partialResultSumNext, 0.U(wlen.W), enable)
  val resultOrigin = RegEnable(resultOriginNext, 0.U((outputWidth).W), enable)
  val resultMinusOne = RegEnable(resultMinusOneNext, 0.U((outputWidth).W), enable)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(outputWidth).W), enable)

  //  Datapath
  //  according two adders
  /** todo :  store counter */
  isLastCycle := counter === 5.U
  output.valid := occupied && isLastCycle
  input.ready := !occupied
  enable := input.fire || !isLastCycle

  /** rW[j]
    *
    * xxxx.xxxxxxxx
    */
  val shiftSum, shiftCarry = Wire(UInt((inputWidth + 4).W))
  shiftSum := partialResultSum << 2
  shiftCarry := partialResultCarry << 2

  /** todo parameterize it */
  val rtzYWidth = 7
  val rtzSWidth = 4
  val ohWidth = 5

  val firstIter = counter === 0.U

  /** S[j]
    *
    * x.xxxxxxxx
    *
    * width = outwidth + 1
    */
  val resultOriginRestore = (resultOrigin << (outputWidth.U - (counter << 1).asUInt))(outputWidth, 0)

  /** todo: opt it with p342 */
  val resultForQDS = Mux(
    firstIter,
    "b101".U,
    Mux(resultOriginRestore(outputWidth), "b111".U, resultOriginRestore(outputWidth - 2, outputWidth - 4))
  )

  /** todo param it */
  val tables: Seq[Seq[Int]] = SRTTable(1 << radixLog2, a, 4, 4).tablesToQDS

  /** todo make sure resultOrigin has setup right? */
  val selectedQuotientOH: UInt =
    QDS(rtzYWidth, ohWidth, rtzSWidth - 1, tables, a)(
      shiftSum.head(rtzYWidth),
      shiftCarry.head(rtzYWidth),
      resultForQDS //.1********* -> 1*** -> ***
    )

  // On-The-Fly conversion
  val otf = OTF(radixLog2, outputWidth + 1, ohWidth, a)(resultOrigin, resultMinusOne, selectedQuotientOH)

  val formationForIter = Mux1H(
    Seq(
      selectedQuotientOH(0) -> (resultMinusOne << 4 | "b1100".U),
      selectedQuotientOH(1) -> (resultMinusOne << 3 | "b111".U),
      selectedQuotientOH(2) -> 0.U,
      selectedQuotientOH(3) -> (~resultOrigin << 3 | "b111".U),
      selectedQuotientOH(4) -> (~resultOrigin << 4 | "b1100".U)
    )
  )

  /** csa need width : inputwidth + 2 */
  val formationFinal = Wire(UInt((inputWidth + 3).W))
  formationFinal := formationForIter << (inputWidth - 2) >> (counter << 1)

  val csa: Vec[UInt] = addition.csa.c32(
    VecInit(
      shiftSum(inputWidth + 1, 0),
      shiftCarry(inputWidth + 1, 0),
      formationFinal(inputWidth + 1, 0)
    )
  )

  val remainderFinal = partialResultSumNext + partialResultCarryNext
  val needCorrect: Bool = remainderFinal(outputWidth+1).asBool

  /** init S[0] = 1 */
  resultOriginNext := Mux(input.fire, 1.U, otf(0))
  resultMinusOneNext := Mux(input.fire, 0.U, otf(1))
  partialResultSumNext := Mux(input.fire, "b1110110111".U, csa(1))
  partialResultCarryNext := Mux(input.fire, 0.U, csa(0) << 1)
  counterNext := Mux(input.fire, 0.U, counter + 1.U)

  output.bits.result := Mux(needCorrect, resultOrigin, resultMinusOne)

}
