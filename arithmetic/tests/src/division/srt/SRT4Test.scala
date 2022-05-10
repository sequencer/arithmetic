package division.srt

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object SRT4Test extends TestSuite with ChiselUtestTester{
  def tests: Tests = Tests {
    test("SRT4 should pass") {
      // parameters
      val dividendWidth: Int = 7
      val dividerWidth: Int  = 7
      val n: Int = 10
      val dividend: Int = 15 << 3
      val divider:  Int = 3 << 5
      val counter: Int = 2
      val quotient: Int = dividend / divider
      val remainder:  Int = dividend % divider
      // test
      testCircuit(new SRT(dividendWidth, dividerWidth, n),
        Seq(chiseltest.internal.NoThreadingAnnotation,
        chiseltest.simulator.WriteVcdAnnotation)){
          dut: SRT =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke("b1111000".U)
          dut.input.bits.divider.poke( "b1100000".U)
          dut.input.bits.counter.poke(counter.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for(a <- 1 to 20 if !flag) {
            if(dut.output.valid.peek().litValue == 1) {
              flag = true
              dut.output.bits.quotient.expect(5.U)
              dut.output.bits.reminder.expect(0.U)
            }
            else
              dut.clock.step()
          }
            utest.assert(flag)
      }
    }
  }
}