package division.srt.srt16

import Chisel.Cat
import division.srt._
import chisel3._
import chisel3.util.{log2Ceil, DecoupledIO, Fill, Mux1H, RegEnable, ValidIO}
import utils.leftShift

/** RSRT16 with Two SRT4 Overlapped Stages
  * n>=7
  * Reuse parameters, OTF and QDS of srt4
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
  val fixWidth = 3
  val divisorWidthFix = dividerWidth + fixWidth
  val xLen:    Int = dividendWidth + radixLog2 + 1 + fixWidth
  val wLen:    Int = xLen + radixLog2
  val ohWidth: Int = 2 * a + 1
  val rWidth:  Int = 1 + radixLog2 + rTruncateWidth

  // IO
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))
  val fixValue = IO(Input(UInt(fixWidth.W)))

  val partialReminderCarryNext, partialReminderSumNext = Wire(UInt(wLen.W))
  val dividerNext = Wire(UInt(divisorWidthFix.W))
  val counterNext = Wire(UInt(log2Ceil(n).W))
  val quotientNext, quotientMinusOneNext = Wire(UInt(n.W))

  // Control
  val isLastCycle, enable: Bool = Wire(Bool())
  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = RegEnable(partialReminderCarryNext, 0.U(wLen.W), enable)
  val partialReminderSum = RegEnable(partialReminderSumNext, 0.U(wLen.W), enable)
  val divider = RegEnable(dividerNext, 0.U(divisorWidthFix.W), enable)
  val quotient = RegEnable(quotientNext, 0.U(n.W), enable)
  val quotientMinusOne = RegEnable(quotientMinusOneNext, 0.U(n.W), enable)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(n).W), enable)

  val occupiedNext = Wire(Bool())
  val occupied = RegNext(occupiedNext, false.B)
  occupiedNext := Mux(input.fire, true.B, Mux(isLastCycle, false.B, occupied))

  //  Datapath
  //  according two adders
  isLastCycle := !counter.orR
  output.valid := Mux(occupied, isLastCycle, false.B)
  input.ready := !occupied
  enable := input.fire || !isLastCycle

  val remainderNoCorrect: UInt = partialReminderSum + partialReminderCarry
  val remainderCorrect: UInt =
    partialReminderSum + partialReminderCarry + (divider << radixLog2)
  val needCorrect: Bool = remainderNoCorrect(wLen - 3).asBool
  // todo issue here
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)(wLen - 4, radixLog2 + fixWidth)
  output.bits.quotient := Mux(needCorrect, quotientMinusOne, quotient)

  // 5*CSA32  SRT16 <- SRT4 + SRT4*5 /SRT16 -> CSA53+CSA32
  val dividerMap = VecInit((-2 to 2).map {
    case -2 => divider << 1
    case -1 => divider
    case 0  => 0.U
    case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider
    case 2  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1)
  })
  val csa0InWidth = rWidth + radixLog2 + 1
  val csaIn1 = leftShift(partialReminderSum, radixLog2).head(csa0InWidth)
  val csaIn2 = leftShift(partialReminderCarry, radixLog2).head(csa0InWidth)

  val csa1 = addition.csa.c32(VecInit(csaIn1, csaIn2, dividerMap(0).head(csa0InWidth))) // -2  csain 10bit
  val csa2 = addition.csa.c32(VecInit(csaIn1, csaIn2, dividerMap(1).head(csa0InWidth))) // -1
  val csa3 = addition.csa.c32(VecInit(csaIn1, csaIn2, dividerMap(2).head(csa0InWidth))) // 0
  val csa4 = addition.csa.c32(VecInit(csaIn1, csaIn2, dividerMap(3).head(csa0InWidth))) // 1
  val csa5 = addition.csa.c32(VecInit(csaIn1, csaIn2, dividerMap(4).head(csa0InWidth))) // 2

  // qds
  val tables:         Seq[Seq[Int]] = SRTTable(1 << radixLog2, a, dTruncateWidth, rTruncateWidth).tablesToQDS
  val partialDivider: UInt = dividerNext.head(dTruncateWidth)(dTruncateWidth - 2, 0)
  val qdsOH0: UInt =
    QDS(rWidth, ohWidth, dTruncateWidth - 1, tables)(
      leftShift(partialReminderSum, radixLog2).head(rWidth),
      leftShift(partialReminderCarry, radixLog2).head(rWidth),
      partialDivider
    ) // q_j+1 oneHot

  def qds(a: Vec[UInt]): UInt = {
    QDS(rWidth, ohWidth, dTruncateWidth - 1, tables)(
      leftShift(a(1), radixLog2).head(rWidth),
      leftShift(a(0), radixLog2 + 1).head(rWidth),
      partialDivider
    )
  }
  //  q_j+2 oneHot precompute
  val qds1SelectedQuotientOH: UInt = qds(csa1) // -2
  val qds2SelectedQuotientOH: UInt = qds(csa2) // -1
  val qds3SelectedQuotientOH: UInt = qds(csa3) // 0
  val qds4SelectedQuotientOH: UInt = qds(csa4) // 1
  val qds5SelectedQuotientOH: UInt = qds(csa5) // 2

  val qds1SelectedQuotientOHMap = VecInit((-2 to 2).map {
    case -2 => qds1SelectedQuotientOH
    case -1 => qds2SelectedQuotientOH
    case 0  => qds3SelectedQuotientOH
    case 1  => qds4SelectedQuotientOH
    case 2  => qds5SelectedQuotientOH
  })

  val qdsOH1 = Mux1H(qdsOH0, qds1SelectedQuotientOHMap) // q_j+2 oneHot
  val qds0sign = qdsOH0(ohWidth - 1, ohWidth / 2 + 1).orR
  val qds1sign = qdsOH1(ohWidth - 1, ohWidth / 2 + 1).orR

  val csa0Out = addition.csa.c32(
    VecInit(
      leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
      leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qds0sign,
      Mux1H(qdsOH0, dividerMap)
    )
  )
  val csa1Out = addition.csa.c32(
    VecInit(
      leftShift(csa0Out(1), radixLog2).head(wLen - radixLog2),
      leftShift(csa0Out(0), radixLog2 + 1).head(wLen - radixLog2 - 1) ## qds1sign,
      Mux1H(qdsOH1, dividerMap)
    )
  )

  // On-The-Fly conversion
  //  todo?: OTF input: Q, QM1, (q1 << 2 + q2) output: Q,QM1
  val otf0 = OTF(radixLog2, n, ohWidth)(quotient, quotientMinusOne, qdsOH0)
  val otf1 = OTF(radixLog2, n, ohWidth)(otf0(0), otf0(1), qdsOH1)

  dividerNext := Mux(input.fire, Cat(input.bits.divider, 0.U(fixWidth.W)), divider)
  counterNext := Mux(input.fire, input.bits.counter, counter - 1.U)
  quotientNext := Mux(input.fire, 0.U, otf1(0))
  quotientMinusOneNext := Mux(input.fire, 0.U, otf1(1))
  partialReminderSumNext := Mux(input.fire, Cat(input.bits.dividend, fixValue), csa1Out(1) << radixLog2)
  partialReminderCarryNext := Mux(input.fire, 0.U, csa1Out(0) << radixLog2 + 1)
}
