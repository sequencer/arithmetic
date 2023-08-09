package float

import chisel3._
import chiseltest._
import utest._
import scala.util.{Random}
import scala.math._

object SqrtFloatTester extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Sqrt Float FP32 should pass") {
      def testcase(): Unit = {
        def extendTofull(input:String, width:Int) =(Seq.fill(width - input.length)("0").mkString("") + input)
        val oprandFloat:  Float = Random.nextInt(1000000)+Random.nextFloat()
        val oprandDouble: Double = oprandFloat.toDouble

        val oprandString = extendTofull(java.lang.Float.floatToIntBits(oprandFloat).toBinaryString,32)
        val oprandSigString = oprandString.substring(9, 32)

        val oprandInput = "b"+ oprandString

        val t = sqrt(oprandDouble)
        val tFloatString = extendTofull(java.lang.Float.floatToIntBits(t.toFloat).toBinaryString,32)
        // 0.xxxxxx,   hidden 1+23bits + 2bits for round
        val sigExpect =   tFloatString.substring(9,32)
        val expExpect =   tFloatString.substring(1,9)

        // test
        testCircuit(
          new SqrtFloat(8,24),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SqrtFloat =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.oprand.poke(oprandInput.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (i <- 0 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true
              val resultActual = extendTofull(dut.output.bits.result.peek().litValue.toString(2),32)
              val sigActual = extendTofull(dut.output.bits.sig.peek().litValue.toString(2),23)
              val expActual = extendTofull(dut.output.bits.exp.peek().litValue.toString(2),8)

              def printValue() :Unit = {
                println(oprandFloat.toString + ".sqrtx = " + t.toString)
                println("input = " + oprandInput)
                println("exp_expect = " + expExpect)
                println("exp_actual = " + expActual)
                println("sig_expect = " + sigExpect)
                println("sig_actual = " + sigActual)
                println("result_expect = " + tFloatString)
                println("result_actual = " + resultActual)
              }


              if(sigExpect != sigActual ){
                printValue()
                utest.assert(sigExpect  == sigActual)
              }

              if (expActual != expExpect) {
                printValue()
                utest.assert(expActual ==expExpect)
              }

              if(resultActual != tFloatString) {
                printValue()
                utest.assert(resultActual == tFloatString)
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