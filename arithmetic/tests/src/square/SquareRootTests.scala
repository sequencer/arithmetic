package square

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}

object SquareRootTest extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Square Root should pass") {
      def testcase(): Unit = {
        // parameters

        // test
        testCircuit(
          new SquareRoot(2, 2, 8, 10),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SquareRoot =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.operand.poke(0.U)
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
