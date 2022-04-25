package division.srt

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object SRT4Test extends TestSuite with ChiselUtestTester{
  def tests: Tests = Tests {
    test("SRT4 should pass") {
      // parameters
      val dividendWidth: Int = 4
      val dividerWidth: Int  = 3
      val n: Int = 3
//      val dividend: Int = 7
//      val divider:  Int = 3
      val countr: Int = 2
      val remainder: Int = dividend / divider
      val quotient:  Int = dividend % divider
      // test
      testCircuit(new SRT(dividendWidth, dividerWidth, n),
        Seq(chiseltest.internal.NoThreadingAnnotation,
        chiseltest.simulator.WriteVcdAnnotation)){
          dut: SRT =>
          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke("b0111".U)
          dut.input.bits.divider.poke("b011".U)
          dut.input.bits.counter.poke(countr.U)
          dut.clock.step(countr)
          dut.output.bits.quotient.expect(quotient.U)
          dut.output.bits.reminder.expect(remainder.U)
      }
    }
  }
}