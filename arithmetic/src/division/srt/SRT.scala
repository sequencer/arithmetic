package division.srt

import division.srt.srt4._
import division.srt.srt8._
import division.srt.srt16._
import chisel3._
import chisel3.util.{DecoupledIO, ValidIO}

class SRT(
  dividendWidth:  Int,
  dividerWidth:   Int,
  n:              Int, // the longest width
  radixLog2:      Int = 2,
  a:              Int = 2,
  dTruncateWidth: Int = 4,
  rTruncateWidth: Int = 4)
    extends Module {
  val input = IO(Flipped(DecoupledIO(new SRTInput(dividendWidth, dividerWidth, n))))
  val output = IO(ValidIO(new SRTOutput(dividerWidth, dividendWidth)))
  // select radix
  if (radixLog2 == 2) { // SRT4
    val srt = Module(new SRT4(dividendWidth, dividerWidth, n, radixLog2, a, dTruncateWidth, rTruncateWidth))
    srt.input <> input
    output <> srt.output
  } else if (radixLog2 == 3) { // SRT8
    val srt = Module(new SRT8(dividendWidth, dividerWidth, n, radixLog2, a, dTruncateWidth, rTruncateWidth))
    srt.input <> input
    output <> srt.output
  } else if (radixLog2 == 4) { //SRT16
    val srt = Module(new SRT16(dividendWidth, dividerWidth, n, radixLog2 >> 1, a, dTruncateWidth, rTruncateWidth))
    srt.input <> input
    output <> srt.output
  }
}
