package division.srt.srt8

import chisel3._
import chiseltest._
import utest._

import scala.util.Random

object SRT8Test extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT8 should pass") {
      def testcase(width: Int): Unit = {
        // parameters
        val radixLog2: Int = 3
        val n:         Int = width
        val m:         Int = n - 1
        val p:         Int = Random.nextInt(m - radixLog2 + 1) //order to offer guardwidth
        val q:         Int = Random.nextInt(m - radixLog2 + 1)
        val dividend:  BigInt = BigInt(p, Random)
        val divider:   BigInt = BigInt(q, Random)
//        val dividend: BigInt = 0x7fffffff
//        val divider: BigInt = 8
        def zeroCheck(x: BigInt): Int = {
          var flag = false
          var a: Int = m
          while (!flag && (a >= -1)) {
            flag = ((BigInt(1) << a) & x) != 0
            a = a - 1
          }
          a + 1
        }
        val zeroHeadDividend:  Int = m - zeroCheck(dividend)
        val zeroHeadDivider:   Int = m - zeroCheck(divider)
        val needComputerWidth: Int = zeroHeadDivider - zeroHeadDividend + 1 + radixLog2 - 1
        val noguard:           Boolean = needComputerWidth % radixLog2 == 0
        val guardWidth:        Int = if (noguard) 0 else 3 - needComputerWidth % 3
        val counter:           Int = (needComputerWidth + guardWidth) / radixLog2
        if ((divider == 0) || (divider > dividend) || (needComputerWidth <= 0))
          return
        val quotient:               BigInt = dividend / divider
        val remainder:              BigInt = dividend % divider
        val leftShiftWidthDividend: Int = zeroHeadDividend - guardWidth
        val leftShiftWidthDivider:  Int = zeroHeadDivider

//        println("leftShiftWidthDividend  = %d ".format(leftShiftWidthDividend))
        testCircuit(
          new SRT8(n, n, n),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SRT8 =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke((dividend << leftShiftWidthDividend).U)
          dut.input.bits.divider.poke((divider << leftShiftWidthDivider).U)
          dut.input.bits.counter.poke(counter.U)
          dut.fixValue.poke((dividend % 4).U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (a <- 1 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
//              println(dut.output.bits.quotient.peek().litValue)
//              println(dut.output.bits.reminder.peek().litValue)
              utest.assert(dut.output.bits.quotient.peek().litValue == quotient)
              utest.assert(dut.output.bits.reminder.peek().litValue >> zeroHeadDivider == remainder)
            }
            dut.clock.step()
          }
          utest.assert(flag)
          dut.clock.step(scala.util.Random.nextInt(10))
        }
      }

      //     testcase(64)
      for (i <- 1 to 30) {
        testcase(64)
      }
    }
  }
}
