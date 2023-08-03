package sqrt

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}
import scala.math._

object SquareRootTester extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Sqrt FP32 should pass") {
      def testcase(): Unit = {
        val oprandFloat:  Float = (0.25 + Random.nextFloat() * 3/4).toFloat
        val oprandDouble: Double = oprandFloat.toDouble

        val oprandString = java.lang.Float.floatToIntBits(oprandFloat).toBinaryString
        val oprandRawString = Seq.fill(32 - oprandString.length)("0").mkString("") + oprandString
        val oprandSigString = oprandRawString.substring(9, 32)

        val inputFloatString = if(oprandFloat<0.5)"b01" + oprandSigString + "0"  else "b1" + oprandSigString + "00"

        val x = sqrt(oprandDouble)
        val xstring = java.lang.Double.doubleToLongBits(x).toBinaryString
        // 0.xxxxxx, hiden 1 + 23bits
        val resultExpect = "1" + (Seq.fill(64 - xstring.length)("0").mkString("") + xstring).substring(12, 37)

        // test
        testCircuit(
          new SquareRoot(2, 2, 26, 26),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SquareRoot =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.operand.poke(inputFloatString.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (i <- 0 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
              val resultActual = dut.output.bits.result.peek().litValue.toString(2).substring(0, 26)
              if(resultExpect != resultActual){
                println(oprandFloat.toString + ".sqrtx = " + x.toString)
                println(inputFloatString)
                utest.assert(resultExpect  == resultActual)
              }

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
