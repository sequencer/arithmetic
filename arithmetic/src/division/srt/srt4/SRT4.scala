package division.srt.srt4

import division.srt._
import addition.csa.CarrySaveAdder
import addition.csa.common.CSACompressor3_2
import chisel3._
import chisel3.util._
import spire.math
import utils.leftShift

/** SRT4
  * 1/2 <= d < 1, 1/2 < rho <=1, 0 < q  < 2
  * radix = 4
  * a = 2, {-2, -1, 0, 1, -2},
  * dTruncateWidth = 4, rTruncateWidth = 8
  * y^（xxx.xxxx）, d^（0.1xxx）
  * -44/16 < y^ < 42/16
  * floor((-r*rho - 2^-t)_t) <= y^ <= floor((r*rho - ulp)_t)
  */

class SRT4(
  dividendWidth:  Int,
  dividerWidth:   Int,
  n:              Int, // the longest width
  radixLog2:      Int = 2,
  a:              Int = 2,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {
  val xLen: Int = dividendWidth + radixLog2 + 1
  val wLen: Int = xLen + radixLog2
  // IO
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))

  val partialReminderCarryNext, partialReminderSumNext = Wire(UInt(wLen.W))
  val quotientNext, quotientMinusOneNext = Wire(UInt(n.W))
  val dividerNext = Wire(UInt(dividerWidth.W))
  val counterNext = Wire(UInt(log2Ceil(n).W))

  // Control
  // sign of Cycle, true -> (counter === 0.U)
  val isLastCycle, enable: Bool = Wire(Bool())

  // State
  // because we need a CSA to minimize the critical path
  val partialReminderCarry = RegEnable(partialReminderCarryNext, 0.U(wLen.W), enable)
  val partialReminderSum = RegEnable(partialReminderSumNext, 0.U(wLen.W), enable)
  val divider = RegEnable(dividerNext, 0.U(dividerWidth.W), enable)
  val quotient = RegEnable(quotientNext, 0.U(n.W), enable)
  val quotientMinusOne = RegEnable(quotientMinusOneNext, 0.U(n.W), enable)
  val counter = RegEnable(counterNext, 0.U(log2Ceil(n).W), enable)

  //  Datapath
  //  according two adders
  isLastCycle := !counter.orR
  output.valid := isLastCycle
  input.ready := isLastCycle
  enable := input.fire || !isLastCycle

  val remainderNoCorrect: UInt = partialReminderSum + partialReminderCarry
  val remainderCorrect: UInt =
    partialReminderSum + partialReminderCarry + (divider << radixLog2)
  val needCorrect: Bool = remainderNoCorrect(wLen - 3).asBool
  output.bits.reminder := Mux(needCorrect, remainderCorrect, remainderNoCorrect)(wLen - 4, radixLog2)
  output.bits.quotient := Mux(needCorrect, quotientMinusOne, quotient)

  val rWidth: Int = 1 + radixLog2 + rTruncateWidth
  val tables: Seq[Seq[Int]] = SRTTable(1 << radixLog2, a, dTruncateWidth, rTruncateWidth).tablesToQDS
  val ohWidth: Int = a match {
    case 2 => 2 * a + 1
    case 3 => 6
  }
  //qds
  val selectedQuotientOH: UInt =
    QDS(rWidth, ohWidth, dTruncateWidth - 1, tables, a)(
      leftShift(partialReminderSum, radixLog2).head(rWidth),
      leftShift(partialReminderCarry, radixLog2).head(rWidth),
      dividerNext.head(dTruncateWidth)(dTruncateWidth - 2, 0) //.1********* -> 1*** -> ***
    )
  // On-The-Fly conversion
  val otf = OTF(radixLog2, n, ohWidth, a)(quotient, quotientMinusOne, selectedQuotientOH)

  val csa: Vec[UInt] =
    if (a == 2) { // a == 2
      //csa
      val dividerMap = VecInit((-2 to 2).map {
        case -2 => divider << 1
        case -1 => divider
        case 0  => 0.U
        case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider
        case 2  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1)
      })
      val qdsSign = selectedQuotientOH(ohWidth - 1, ohWidth / 2 + 1).orR
      addition.csa.c32(
        VecInit(
          leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
          leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qdsSign,
          Mux1H(selectedQuotientOH, dividerMap)
        )
      )
    } else { // a==3
      val qHigh = selectedQuotientOH(5, 3)
      val qLow = selectedQuotientOH(2, 0)
      val qds0Sign = qHigh.head(1)
      val qds1Sign = qLow.head(1)

      // csa
      val dividerHMap = VecInit((-1 to 1).map {
        case -1 => divider << 1 // -2
        case 0  => 0.U //  0
        case 1  => Fill(radixLog2, 1.U(1.W)) ## ~(divider << 1) // 2
      })
      val dividerLMap = VecInit((-1 to 1).map {
        case -1 => divider // -1
        case 0  => 0.U //  0
        case 1  => Fill(1 + radixLog2, 1.U(1.W)) ## ~divider // 1
      })
      val csa0 = addition.csa.c32(
        VecInit(
          leftShift(partialReminderSum, radixLog2).head(wLen - radixLog2),
          leftShift(partialReminderCarry, radixLog2).head(wLen - radixLog2 - 1) ## qds0Sign,
          Mux1H(qHigh, dividerHMap)
        )
      )
      addition.csa.c32(
        VecInit(
          csa0(1).head(wLen - radixLog2),
          leftShift(csa0(0), 1).head(wLen - radixLog2 - 1) ## qds1Sign,
          Mux1H(qLow, dividerLMap)
        )
      )
    }

  dividerNext := Mux(input.fire, input.bits.divider, divider)
  counterNext := Mux(input.fire, input.bits.counter, counter - 1.U)
  quotientNext := Mux(input.fire, 0.U, otf(0))
  quotientMinusOneNext := Mux(input.fire, 0.U, otf(1))
  partialReminderSumNext := Mux(input.fire, input.bits.dividend, csa(1) << radixLog2)
  partialReminderCarryNext := Mux(input.fire, 0.U, csa(0) << 1 + radixLog2)
}
