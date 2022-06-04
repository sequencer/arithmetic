package division.srt

import chisel3._
import chisel3.util.{log2Ceil, DecoupledIO, Fill, Mux1H, RegEnable, ValidIO}
import utils.leftShift

/** RSRT16 with Two SRT4 Overlapped Stages
  */
class SRT16(
  dividendWidth:  Int,
  dividerWidth:   Int,
  n:              Int, // the longest width
  radixLog2:      Int = 2,
  a:              Int = 2,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {

  val xLen:    Int = dividendWidth + radixLog2 + 1
  val wLen:    Int = xLen + radixLog2
  val ohWidth: Int = 2 * a + 1

  // IO
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))

  val partialReminderCarryNext, partialReminderSumNext = Wire(UInt(wLen.W))
  val dividerNext = Wire(UInt(dividerWidth.W))
  val counterNext = Wire(UInt(log2Ceil(n).W))
  val quotientNext, quotientMinusOneNext = Wire(UInt(n.W))
  val ws1, wc1, ws2, wc2: UInt = Wire(UInt(wLen.W))

  // Control
  val qds1Sign, qds2Sign, isLastCycle, enable: Bool = Wire(Bool())
  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = RegEnable(partialReminderCarryNext, 0.U(wLen.W), enable)
  val partialReminderSum = RegEnable(partialReminderSumNext, 0.U(wLen.W), enable)
  val divider = RegEnable(dividerNext, 0.U(dividerWidth.W), enable)
  val quotient = RegEnable(quotientNext, 0.U(n.W), enable)
  val quotientMinusOne = RegEnable(quotientMinusOneNext, 0.U(n.W), enable)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(n).W), enable)

  //  Datapath
  isLastCycle := !counter.orR
  output.valid := isLastCycle
  input.ready := isLastCycle
  enable := input.fire || !isLastCycle

  val remainderNoCorrect: UInt = partialReminderSum + partialReminderCarry
  val remainderCorrect: UInt =
    partialReminderSum + partialReminderCarry + (divider << 2)
  val needCorrect: Bool = remainderNoCorrect(wLen - 3).asBool
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)(wLen - 4, radixLog2)
  output.bits.quotient := Mux(needCorrect, quotientMinusOne, quotient)

  // qds
  val rWidth:         Int = 1 + radixLog2 + rTruncateWidth
  val partialDivider: UInt = dividerNext.head(dTruncateWidth)(dTruncateWidth - 2, 0)
  val qds1SelectedQuotientOH: UInt =
    QDS(rWidth, ohWidth, dTruncateWidth - 1)(
      leftShift(partialReminderSum, radixLog2).head(rWidth),
      leftShift(partialReminderCarry, radixLog2).head(rWidth),
      partialDivider
    )
  val qds2SelectedQuotientOH: UInt =
    QDS(rWidth, ohWidth, dTruncateWidth - 1)(
      leftShift(ws1, radixLog2).head(rWidth),
      leftShift(wc1, radixLog2).head(rWidth),
      partialDivider
    )
  qds1Sign := qds1SelectedQuotientOH(ohWidth - 1, ohWidth / 2 + 1).orR
  qds2Sign := qds2SelectedQuotientOH(ohWidth - 1, ohWidth / 2 + 1).orR

  // CSA32 -> CSA32
  val dividerMap = VecInit((-2 to 2).map {
    case -2 => divider << 1
    case -1 => divider
    case 0  => 0.U
    case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider
    case 2  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1)
  })
  val csa1In = VecInit(
    leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
    leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qds1Sign,
    Mux1H(qds1SelectedQuotientOH, dividerMap)
  )
  val csa2In = VecInit(
    leftShift(ws1, radixLog2).head(wLen - radixLog2),
    leftShift(wc1, radixLog2).head(wLen - radixLog2 - 1) ## qds2Sign,
    Mux1H(qds2SelectedQuotientOH, dividerMap)
  )
  val csa1 = addition.csa.c32(csa1In)
  val csa2 = addition.csa.c32(csa2In)
  ws1 := csa1(1) << radixLog2
  wc1 := csa1(0) << radixLog2 + 1
  ws2 := csa2(1) << radixLog2
  wc2 := csa2(0) << radixLog2 + 1

  // On-The-Fly conversion
  val otf1 = OTF(1 << radixLog2, n, ohWidth)(quotient, quotientMinusOne, qds1SelectedQuotientOH)
  val otf2 = OTF(1 << radixLog2, n, ohWidth)(otf1(0), otf1(1), qds2SelectedQuotientOH)

  dividerNext := Mux(input.fire, input.bits.divider, divider)
  counterNext := Mux(input.fire, input.bits.counter, counter - 1.U)
  quotientNext := Mux(input.fire, 0.U, otf2(0))
  quotientMinusOneNext := Mux(input.fire, 0.U, otf2(1))
  partialReminderSumNext := Mux(input.fire, input.bits.dividend, ws2)
  partialReminderCarryNext := Mux(input.fire, 0.U, wc2)
}
