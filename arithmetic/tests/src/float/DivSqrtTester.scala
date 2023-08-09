package float

import chisel3._
import chiseltest._
import utest._

import scala.util.Random
import division.srt.srt16._

import scala.math.sqrt

object DivSqrtTester extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("DivSqrt should pass") {
      def testcase(width: Int): Unit = {
        def extendTofull(input:String, width:Int) =(Seq.fill(width - input.length)("0").mkString("") + input)
        val n:         Int = width

        val xFloat = (Random.nextInt(100000) + Random.nextFloat() ).toFloat
        val dFloat = (Random.nextInt(100000) + Random.nextFloat() ).toFloat

        val xFloatString = extendTofull(java.lang.Float.floatToIntBits(xFloat).toBinaryString, 32)
        val dFloatString = extendTofull(java.lang.Float.floatToIntBits(dFloat).toBinaryString, 32)

        val xInput = "b"+xFloatString
        val dInput = "b"+dFloatString

        val opsqrt = "false"


        val qDouble = xFloat / dFloat
        val q = qDouble.toFloat
        val qFloatString = extendTofull(java.lang.Float.floatToIntBits(q).toBinaryString, 32)


        val t = sqrt(xFloat)
        val tFloatString = extendTofull(java.lang.Float.floatToIntBits(t.toFloat).toBinaryString, 32)
        // 0.xxxxxx,   hidden 1+23bits + 2bits for round
        val sigExpect = tFloatString.substring(9, 32)
        val expExpect = tFloatString.substring(1, 9)

        val sig_Expect = if(opsqrt == "true") tFloatString.substring(9, 32) else qFloatString.substring(9, 32)
        val exp_Expect = if(opsqrt == "true") tFloatString.substring(1, 9) else qFloatString.substring(1, 9)
        val result_Expect = if(opsqrt == "true") tFloatString else qFloatString

        // test
        testCircuit(
          new DivSqrt(8,24),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: DivSqrt =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.a.poke((xInput).U)
          dut.input.bits.b.poke((dInput).U)
          dut.input.bits.sqrt.poke(false.B)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (a <- 1 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true

              val sig_Actual = extendTofull(dut.output.bits.sig.peek().litValue.toString(2),23)
              val exp_Actual = extendTofull(dut.output.bits.exp.peek().litValue.toString(2),8)
              val result_Actual = extendTofull(dut.output.bits.result.peek().litValue.toString(2),32)

              val expInt_Actual = Integer.parseInt(exp_Actual,2)
              val expInt_Expect = Integer.parseInt(exp_Expect,2)

              def printvalue(): Unit = {

                println(xFloat.toString + "/ " + dFloat.toString + "="+ qDouble.toString)
                println("result_Actual = " + result_Actual)
                println("result_Expect = " + result_Expect)


                println("sig_expect = " + sig_Expect)
                println("sig_actual = " + sig_Actual)

              }
              if((sig_Expect != sig_Actual)|| (exp_Actual != exp_Expect)||(result_Actual != result_Expect)){
                printvalue()
                utest.assert(sig_Expect == sig_Actual)
                utest.assert(exp_Expect == exp_Actual)
                utest.assert(result_Actual == result_Expect)
              }

            }
            dut.clock.step()
          }
          utest.assert(flag)
          dut.clock.step(scala.util.Random.nextInt(10))
        }
      }


      for (i <- 1 to 100) {
        testcase(32)
      }

    }
  }
}