package float

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}
import scala.math._

object SquareRootTester extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Sqrt Float FP32 should pass") {
      def testcase(): Unit = {
        val oprandFloat:  Float = (Random.nextInt(100000)+Random.nextFloat() ).toFloat
        val oprandDouble: Double = oprandFloat.toDouble

        val oprandString = java.lang.Float.floatToIntBits(oprandFloat).toBinaryString
        val oprandRawString = Seq.fill(32 - oprandString.length)("0").mkString("") + oprandString
        val oprandSigString = oprandRawString.substring(9, 32)

        val ExepctFracIn = if(oprandFloat<0.5)"b01" + oprandSigString + "0"  else "b1" + oprandSigString + "00"
        val circuitInput = "b"+ oprandRawString

        val x = sqrt(oprandDouble)
        val xDoublestring = java.lang.Double.doubleToLongBits(x).toBinaryString
        val xFloatstring = java.lang.Float.floatToIntBits(x.toFloat).toBinaryString
        val xDouble = (Seq.fill(64 - xDoublestring.length)("0").mkString("") + xDoublestring)
        val xFloat = (Seq.fill(32 - xFloatstring.length)("0").mkString("") + xFloatstring)
        // 0.xxxxxx,   hidden 1+23bits + 2bits for round
        val sigExpect =   "1"+xDouble.substring(12, 37)
        // todo:
        val expExpect =   xFloat.substring(1,9)

        // test
        testCircuit(
          new SqrtFloat(8,24),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SqrtFloat =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.oprand.poke(circuitInput.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (i <- 0 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
              val resultActual = dut.output.bits.result.peek().litValue.toString(2)
              val sigActual = dut.output.bits.sig.peek().litValue.toString(2)
              val expActualraw = dut.output.bits.exp.peek().litValue.toString(2)
              val expActual = (Seq.fill(8 - expActualraw.length)("0").mkString("") + expActualraw)

              if(sigExpect != sigActual ){
                println(oprandFloat.toString + ".sqrtx = " + x.toString)
                println("input = " + circuitInput)
                println("expect reult = " + xFloat)
                println("sig_expect = "+ sigExpect)
                println("sig_actual = "+ sigActual)

                utest.assert(sigExpect  == sigActual)
              }

              if (expActual != expExpect) {
                println(oprandFloat.toString + ".sqrtx = " + x.toString)
                println("input = "+circuitInput)
                println("expect reult = "+ xFloat)
                println("exp_expect = " + expExpect)
                println("exp_actual = " + expActual)
                utest.assert(expActual ==expExpect)
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