package division.srt.srt4

import chisel3._
import chiseltest._
import utest._

import scala.util.Random

object SRT4FracTest extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT4 Fraction should pass") {
      def testcase(width: Int, x: Int, d: Int): Unit = {
        // parameters
        val radixLog2: Int = 2
        val n:         Int = width
        val m:         Int = n - 1

//        val dividend: BigInt = BigInt("fffffff0", 16) + x
        val dividend: BigInt = 350 // 101011110
        val divisor:  BigInt = 197 // 11000101
        val counter = 14

        // test
        testCircuit(
          new SRT4(n, n, n),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SRT4 =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke((dividend << 24).U)
          dut.input.bits.divider.poke((divisor << 24).U)
          dut.input.bits.counter.poke(counter.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (a <- 1 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true

              def printvalue(): Unit = {

                println(
                  "%d / %d = %d --- %d".format(
                    dividend,
                    divisor,
                    dut.output.bits.quotient.peek().litValue,
                    dut.output.bits.reminder.peek().litValue
                  )
                )
              }

//              utest.assert(dut.output.bits.quotient.peek().litValue == quotient)
//              utest.assert(dut.output.bits.reminder.peek().litValue >> zeroHeaddivisor == remainder)
            }
            dut.clock.step()
          }
          utest.assert(flag)
          dut.clock.step(scala.util.Random.nextInt(10))
        }
      }

//      for (i <- 0 to 15) {
//        for (j <- 1 to 16) {
//          testcase(32, i, j)
//        }
//      }

//            for (i <- 2 to 15) {
//              for (j <- 1 to i-1) {
//                testcase(4, i, j)
//              }
//            }
//      testcase(32, 15, 1)

    }
  }
}
