package division.srt.srt16

import chisel3._
import chiseltest._
import utest._

import scala.util.Random

object SRT16FracTester extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("SRT16 Fraction should pass") {
      def testcase(width: Int): Unit = {
        def extendTofull(input:String, width:Int) =(Seq.fill(width - input.length)("0").mkString("") + input)
        val n:         Int = width

        val xFloat = (0.5 +Random.nextFloat() /2).toFloat
        val dFloat = (0.5 + Random.nextFloat() /2).toFloat

        val xFloatString = extendTofull(java.lang.Float.floatToIntBits(xFloat).toBinaryString, 32)
        val dFloatString = extendTofull(java.lang.Float.floatToIntBits(dFloat).toBinaryString, 32)
        val xInput = "b1"+xFloatString.substring(9, 32)+"00000000"
        val dInput = "b1"+dFloatString.substring(9, 32)+"00000000"




        val counter = 8

        val qDouble = xFloat / dFloat
        val qFloat  = qDouble.toFloat
        val qDoubleString = extendTofull(java.lang.Double.doubleToLongBits(qDouble).toBinaryString,64)
        val qFloatString  = extendTofull(java.lang.Float.floatToIntBits(qFloat).toBinaryString, 32)
        val q_SigExpect = qFloatString.substring(9, 32)

        val q_SigExpectInt = Integer.parseInt(q_SigExpect, 2)

        val xExp = Integer.parseInt(xFloatString.substring(1,9),2)
        val dExp = Integer.parseInt(dFloatString.substring(1,9),2)
        val expDiff = xExp - dExp
        val exp_actual = expDiff + 128
        val q_ExpExpect = Integer.parseInt(qFloatString.substring(1,9),2)



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

              val isLess = quotient_actual(4).toString=="0"
              val q_SigBefore = if(isLess) {
                quotient_actual.substring(6,32)
              } else {
                quotient_actual.substring(5,32)
              }

              val sigIncr = q_SigBefore(23).toString.toInt
              val q_SigActualInt = Integer.parseInt(q_SigBefore.substring(0,23), 2) + sigIncr




              def printvalue(): Unit = {

                println(xFloat.toString + "/ " + dFloat.toString + "="+ qDouble.toString)
                println("xinput = " + xInput)
                println("dinput = " + dInput)

                println("all q = " + quotient_actual)
                println("q_except = "+ qFloatString.toString)
                println("all q size ="+ quotient_actual.length.toString)

                println("isLess= "+ isLess.toString)

                println("q_expect = " + q_SigExpect)
                println("q_actual = " + q_SigBefore)

                println("q_expectInt = " + q_SigExpectInt)
                println("q_actualInt = " + q_SigActualInt)

              }
              if(q_SigActualInt != q_SigExpectInt){
                printvalue()
                utest.assert(q_SigActualInt == q_SigExpectInt)

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