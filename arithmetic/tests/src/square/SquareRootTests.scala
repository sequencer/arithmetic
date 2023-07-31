package square

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}
import scala.math._

object SquareRootTest extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Square Root should pass") {
      def testcase(): Unit = {
        // parameters
        val oprand: Double = 0.75
        val inputOprandRawString = java.lang.Double.doubleToLongBits(oprand).toBinaryString
        val inputOprandString =
          "b1" + (Seq.fill(64 - inputOprandRawString.length)("0").mkString("") + inputOprandRawString).substring(12, 35)
        println("inputString = " + inputOprandString)

        val x = sqrt(oprand)
        println("x(double) = " + x.toString)
        val xstring = java.lang.Double.doubleToLongBits(x).toBinaryString
        // 0.xxxxxx, hiden 1 + 23bits
        val resultExpect = "1" + (Seq.fill(64 - xstring.length)("0").mkString("") + xstring).substring(12, 35)

        // test
        testCircuit(
          new SquareRoot(2, 2, 24, 26),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SquareRoot =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.operand.poke(inputOprandString.U)
          dut.input.bits.counter.poke(5.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (i <- 0 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
              println("result_expect = " + resultExpect)
              println("result_actual = " + dut.output.bits.result.peek().litValue.toString(2).substring(0, 26))
              utest.assert(dut.output.bits.result.peek().litValue.toString(2).substring(0, 24) == resultExpect)

            } else
              dut.clock.step()

          }
          utest.assert(flag)
        }
      }

      testcase()

    }
  }
}
