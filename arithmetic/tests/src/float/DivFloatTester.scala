package float

import chisel3._
import chiseltest._
import utest._

import scala.util.Random

import division.srt.srt16._

object DivFloatTester extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Div Float should pass") {
      def testcase(width: Int): Unit = {
        def extendTofull(input:String, width:Int) =(Seq.fill(width - input.length)("0").mkString("") + input)
        val n:         Int = width

        val xFloat = (Random.nextInt(100000) + Random.nextFloat() ).toFloat
        val dFloat = (Random.nextInt(100000) + Random.nextFloat() ).toFloat

        val xFloatString = extendTofull(java.lang.Float.floatToIntBits(xFloat).toBinaryString, 32)
        val dFloatString = extendTofull(java.lang.Float.floatToIntBits(dFloat).toBinaryString, 32)
        val xInput = "b"+xFloatString
        val dInput = "b"+dFloatString


        val counter = 8

        val qDouble = xFloat / dFloat
        val qFloat = qDouble.toFloat
        val qDoubleString = extendTofull(java.lang.Double.doubleToLongBits(qDouble).toBinaryString, 64)
        val qFloatString = extendTofull(java.lang.Float.floatToIntBits(qFloat).toBinaryString, 32)
        val sig_Expect = qFloatString.substring(9, 32)
        val exp_Expect = qFloatString.substring(1, 9)


        val xExp = Integer.parseInt(xFloatString.substring(1, 9), 2)
        val dExp = Integer.parseInt(dFloatString.substring(1, 9), 2)

        // test
        testCircuit(
          new DivFloat(8,24),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: DivFloat =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke((xInput).U)
          dut.input.bits.divisor.poke((dInput).U)
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
                println("expInt_Actual = " + expInt_Actual)
                println("expInt_Expect = " + expInt_Expect)

//                println("all q = " + quotient_actual)
//                println("all q size ="+ quotient_actual.length.toString)

                println("sig_expect = " + sig_Expect)
                println("sig_actual = " + sig_Actual)

              }
              if((sig_Expect != sig_Actual)|| (exp_Actual != exp_Expect)||(result_Actual != qFloatString)){
                printvalue()
                utest.assert(sig_Expect == sig_Actual)
                utest.assert(exp_Expect == exp_Actual)
                utest.assert(result_Actual != qFloatString)
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