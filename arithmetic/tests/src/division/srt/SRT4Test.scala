package division.srt.srt4

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}

object SRT4Test extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT4 should pass") {
      def testcase(width: Int): Unit = {
        // parameters
        val radixLog2: Int = 2
        val n:         Int = width
        val m:         Int = n - 1
        val p:         Int = Random.nextInt(m)
        val q:         Int = Random.nextInt(m)
        val dividend:  BigInt = BigInt(p, Random)
        val divisor:   BigInt = BigInt(q, Random)
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
        val zeroHeaddivisor:   Int = m - zeroCheck(divisor)
        val needComputerWidth: Int = zeroHeaddivisor - zeroHeadDividend + 1 + radixLog2 - 1
        val noguard:           Boolean = needComputerWidth % radixLog2 == 0
        val guardWidth:        Int = if (noguard) 0 else 2 - needComputerWidth % 2
        val counter:           Int = (needComputerWidth + 1) / 2
        if ((divisor == 0) || (divisor > dividend) || (needComputerWidth <= 0))
          return
        val quotient:               BigInt = dividend / divisor
        val remainder:              BigInt = dividend % divisor
        val leftShiftWidthDividend: Int = zeroHeadDividend - (if (noguard) 0 else 1)
        val leftShiftWidthdivisor:  Int = zeroHeaddivisor
        println("leftShiftWidthDividend  = %d ".format(leftShiftWidthDividend))
        /*        println("zeroHeadDividend_ex   = %d".format(zeroHeadDividend))
        println("zeroHeaddivisor_ex   = %d".format(zeroHeaddivisor))
        println("noguard = "+ noguard)
        println("quotient   = %d,  remainder  = %d".format(quotient, remainder))
        println("counter_ex   = %d, needComputerWidth_ex = %d".format(counter, needComputerWidth))*/
        // test
        testCircuit(
          new SRT4(n, n, n),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SRT4 =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke((dividend << leftShiftWidthDividend).U)
          dut.input.bits.divider.poke((divisor << leftShiftWidthdivisor).U)
          dut.input.bits.counter.poke(counter.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (a <- 1 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
//              println("%x / %x = %x --- %x".format(dividend, divisor, quotient, remainder))
//              println(
//                "%x / %x = %x --- %x".format(
//                  dividend,
//                  divisor,
//                  dut.output.bits.quotient.peek().litValue,
//                  dut.output.bits.reminder.peek().litValue >> zeroHeaddivisor
//                )
//              )
              utest.assert(dut.output.bits.quotient.peek().litValue == quotient)
              utest.assert(dut.output.bits.reminder.peek().litValue >> zeroHeaddivisor == remainder)
            }
            dut.clock.step()
          }
          utest.assert(flag)
          dut.clock.step(scala.util.Random.nextInt(10))
        }
      }

//      testcase(64)
      for( i <- 1 to 30){
        testcase(64)
      }
    }
  }
}
