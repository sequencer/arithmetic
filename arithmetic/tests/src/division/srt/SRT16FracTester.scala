package division.srt.srt16

import chisel3._
import chiseltest._
import utest._

import scala.util.Random

object SRT16FracTest extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT16 Fraction should pass") {
      def testcase(width: Int): Unit = {
        def extendTofull(input:String, width:Int) =(Seq.fill(width - input.length)("0").mkString("") + input)
        val n:         Int = width

        val xFloat = (0.5 + Random.nextFloat() /2).toFloat
        val dFloat = (0.5 + Random.nextFloat() /2).toFloat

        val xFloatString = extendTofull(java.lang.Float.floatToIntBits(xFloat).toBinaryString, 32)
        val dFloatString = extendTofull(java.lang.Float.floatToIntBits(dFloat).toBinaryString, 32)
        val xInput = "b1"+xFloatString.substring(9, 32)+"00000000"
        val dInput = "b1"+dFloatString.substring(9, 32)+"00000000"


        val counter = 8

        val qDouble = xFloat / dFloat
        val qDoubleString = extendTofull(java.lang.Double.doubleToLongBits(qDouble).toBinaryString,64)
        val q_Expect = "1"+ qDoubleString.substring(12, 39)

        // test
        testCircuit(
          new SRT16(n, n, n),
          Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)
        ) { dut: SRT16 =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke((xInput).U)
          dut.input.bits.divider.poke((dInput).U)
          dut.input.bits.counter.poke(counter.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for (a <- 1 to 1000 if !flag) {
            if (dut.output.valid.peek().litValue == 1) {
              flag = true

              val quotient_actual = extendTofull(dut.output.bits.quotient.peek().litValue.toString(2),32)

              val q_Actual = if(quotient_actual(4).toString=="0") {
                quotient_actual.substring(5,32)
              } else {
                quotient_actual.substring(4,32)
              }


              def printvalue(): Unit = {

                println(xFloat.toString + "/ " + dFloat.toString + "="+ qDouble.toString)
                println("xinput = " + xInput)
                println("dinput = " + dInput)

                println("all q = " + quotient_actual)
                println("all q size ="+ quotient_actual.length.toString)

                println("q_expect = " + q_Expect)
                println("q_actual = " + q_Actual)

              }
              if(q_Expect != q_Actual){
                printvalue()
                utest.assert(q_Expect == q_Actual)
              }



            }
            dut.clock.step()
          }
          utest.assert(flag)
          dut.clock.step(scala.util.Random.nextInt(10))
        }
      }


//      for (i <- 1 to 100) {
//        testcase(32)
//      }

    }
  }
}