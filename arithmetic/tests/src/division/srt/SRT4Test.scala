package division.srt.srt4

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._
import scala.util.{Random}

object SRT4Test extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT4 should pass") {
      def testcase(width: Int): Unit ={
        // parameters
        val radixLog2: Int = 2
        val n: Int = width
        val m: Int = n - 1
        val p: Int = Random.nextInt(m)
        val q: Int = Random.nextInt(m)
        val dividend: BigInt = BigInt(p, Random)
        val divider: BigInt = BigInt(q, Random)
//        val dividend: BigInt = BigInt("65")
//        val divider: BigInt = BigInt("1")
        def zeroCheck(x: BigInt): Int = {
          var flag = false
          var a: Int = m
          while (!flag && (a >= -1)) {
            flag = ((BigInt(1) << a) & x) != 0
            a = a - 1
          }
          a + 1
        }
        val zeroHeadDividend: Int = m - zeroCheck(dividend)
        val zeroHeadDivider: Int = m - zeroCheck(divider)
        val needComputerWidth: Int = zeroHeadDivider - zeroHeadDividend + 1 + 1
        val noguard: Boolean = needComputerWidth % radixLog2 == 0
        val counter: Int = (needComputerWidth + 1) / 2
        if ((divider == 0) || (divider > dividend) || (needComputerWidth <= 0))
          return
        val quotient: BigInt = dividend / divider
        val remainder: BigInt = dividend % divider
        val leftShiftWidthDividend: Int = zeroHeadDividend - (if (noguard) 0 else 1)
        val leftShiftWidthDivider: Int = zeroHeadDivider
//        println("dividend = %8x, dividend = %d ".format(dividend, dividend))
//        println("divider  = %8x, divider  = %d".format(divider, divider))
//        println("zeroHeadDividend  = %d,  dividend << zeroHeadDividend = %d".format(zeroHeadDividend, dividend << leftShiftWidthDividend))
//        println("zeroHeadDivider   = %d,  divider << zeroHeadDivider  = %d".format(zeroHeadDivider, divider << leftShiftWidthDivider))
//        println("quotient   = %d,  remainder  = %d".format(quotient, remainder))
//        println("counter   = %d, needComputerWidth = %d".format(counter, needComputerWidth))
        // test
        testCircuit(new SRT4(n, n, n),
          Seq(chiseltest.internal.NoThreadingAnnotation,
            chiseltest.simulator.WriteVcdAnnotation)) {
          dut: SRT4 =>
            dut.clock.setTimeout(0)
            dut.input.valid.poke(true.B)
            dut.input.bits.dividend.poke((dividend << leftShiftWidthDividend).U)
            dut.input.bits.divider.poke((divider << leftShiftWidthDivider).U)
            dut.input.bits.counter.poke(counter.U)
            dut.clock.step()
            dut.input.valid.poke(false.B)
            var flag = false
            for (a <- 1 to 1000 if !flag) {
              if (dut.output.valid.peek().litValue == 1) {
                flag = true
                println(dut.output.bits.quotient.peek().litValue)
                println(dut.output.bits.reminder.peek().litValue)
                utest.assert(dut.output.bits.quotient.peek().litValue == quotient)
                utest.assert(dut.output.bits.reminder.peek().litValue >> zeroHeadDivider == remainder)
              }
              dut.clock.step()
            }
            utest.assert(flag)
            dut.clock.step(scala.util.Random.nextInt(10))
        }
      }

      testcase(64)
//      for( i <- 1 to 100){
//        testcase(64)
//      }
    }
  }
}