package sqrt

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}
import scala.math._

object SquareRootTest extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Square Root for FP32 should pass") {
      def testcase(): Unit = {
        val oprandFloat:  Float = (0.5 + Random.nextFloat() / 2).toFloat
        val oprandDouble: Double = oprandFloat.toDouble
        val oprandDoubleRawString = java.lang.Double.doubleToLongBits(oprandDouble).toBinaryString
        val oprandFloatRawString = java.lang.Float.floatToIntBits(oprandFloat).toBinaryString

        val inputFloatString = {
          "b1" + (Seq.fill(32 - oprandFloatRawString.length)("0").mkString("") + oprandFloatRawString)
            .substring(9, 32)
        }
        val x = sqrt(oprandDouble)
        val xstring = java.lang.Double.doubleToLongBits(x).toBinaryString
        // 0.xxxxxx, hiden 1 + 23bits
        val resultExpect = "1" + (Seq.fill(64 - xstring.length)("0").mkString("") + xstring).substring(12, 37)
        //        println(oprandFloat.toString + ".sqrtx = " + x.toString)

        // test
        testCircuit(
          new SquareRoot(2, 2, 24, 26),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SquareRoot =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.operand.poke(inputFloatString.U)
          dut.input.bits.counter.poke(5.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (i <- 0 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
              val resultActual = dut.output.bits.result.peek().litValue.toString(2).substring(0, 26)
//              println("result_expect26 = " + resultExpect)
//              println("result_actual26 = " + resultActual)
//              println("result_expect24 = " + resultExpect.substring(0, 24))
//              println("result_actual24 = " + resultActual.substring(0, 24))
              utest.assert(
                (resultExpect)  == (resultActual)
              )
            } else
              dut.clock.step()
          }
          utest.assert(flag)
        }
      }

      for (i <- 1 to 100) {
        testcase()
      }

    }
  }
}
