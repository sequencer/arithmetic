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
        val oprand: Double = 0.71484375
        val x = sqrt(oprand)
        val xstring = java.lang.Long.toBinaryString(java.lang.Double.doubleToRawLongBits(x))
        println(xstring)
        011000011100011010100100110100110110011100011110101

        // test
        testCircuit(
          new SquareRoot(2, 2, 24, 26),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SquareRoot =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.operand.poke("b101101110000000000000000".U)
          dut.input.bits.counter.poke(5.U)
          println("ready = %d".format(dut.input.ready.peek().litValue))
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (i <- 0 to 1000) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true

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
