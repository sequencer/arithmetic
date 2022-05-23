package division.srt

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object SRT4Test extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT4 should pass") {
      // parameters
      val n: Int = 16
      val m: Int = n - 1
//      val dividend: Int = scala.util.Random.nextInt(scala.math.pow(2,n -2 ).toInt)
//      val divider: Int = scala.util.Random.nextInt(scala.math.pow(2, n - 8).toInt)
      val dividend: Int = 65
      val divider: Int = 1

      def zeroCheck(x: Int): Int = {
        var flag = false
        var a: Int = m
        while (!flag && (a >= -1)) {
          flag = ((1 << a) & x) != 0
          a = a - 1
        }
        a + 1
      }
      val zeroHeadDividend: Int = m - zeroCheck(dividend)
      val zeroHeadDivider: Int = m - zeroCheck(divider)
      val needComputerWidth: Int = zeroHeadDivider - zeroHeadDividend + 1 + 1
      val noguard: Boolean = needComputerWidth % 2 == 0

      val counter: Int = (needComputerWidth + 1) / 2
      val quotient: Int = dividend / divider
      val remainder: Int = dividend % divider
      val leftShiftWidthDividend: Int = zeroHeadDividend - (if (noguard) 0 else 1)
      val leftShiftWidthDivider: Int = zeroHeadDivider

      println("dividend = %8x, dividend = %d ".format(dividend, dividend))
      println("divider  = %8x, divider  = %d".format(divider, divider))
      println("zeroHeadDividend  = %d,  dividend << zeroHeadDividend = %d".format(zeroHeadDividend, dividend << leftShiftWidthDividend))
      println("zeroHeadDivider   = %d,  divider << zeroHeadDivider  = %d".format(zeroHeadDivider, divider << leftShiftWidthDivider))
      println("quotient   = %d,  remainder  = %d".format(quotient, remainder))
      println("counter   = %d, needComputerWidth = %d".format(counter, needComputerWidth))
      // test
      //println(chisel3.stage.ChiselStage.emitVerilog(new SRT(dividendWidth, dividerWidth, n)))
      testCircuit(new SRT(n, n, n),
        Seq(chiseltest.internal.NoThreadingAnnotation,
          chiseltest.simulator.WriteVcdAnnotation)) {
        dut: SRT =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke((dividend << leftShiftWidthDividend).U)
          dut.input.bits.divider.poke((divider << leftShiftWidthDivider).U)
          dut.input.bits.counter.poke(counter.U)
//          dut.input.bits.dividend.poke("b11_1111_1111".U)
//          dut.input.bits.divider.poke( "b10_0000_0000".U)
//          dut.input.bits.counter.poke(6.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (a <- 1 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
              println(dut.output.bits.quotient.peek().litValue)
              println(dut.output.bits.reminder.peek().litValue)
//              println(dut.qds.partialDivider.peek().litValue)
              utest.assert(dut.output.bits.quotient.peek().litValue == 61)
              utest.assert(dut.output.bits.reminder.peek().litValue >> zeroHeadDivider == remainder)
//              utest.assert(dut.output.bits.quotient.peek().litValue == 31)
//              utest.assert(dut.output.bits.reminder.peek().litValue  == 0)
            }
            dut.clock.step()
          }
          utest.assert(flag)
          dut.clock.step(scala.util.Random.nextInt(10))
      }
    }
  }
}