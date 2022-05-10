package division.srt

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object SRT4Test extends TestSuite with ChiselUtestTester{
  def tests: Tests = Tests {
    test("SRT4 should pass") {
      // parameters
      val dividendWidth: Int = 8
      val dividerWidth: Int  = 8
      val n: Int = 10
      val dividend: Int = 15 << 3
      val divider:  Int = 3 << 5
      val counter: Int = 2
//      val counter: Int = 1
      val quotient: Int = dividend / divider
      val remainder:  Int = dividend % divider
      // test
      testCircuit(new SRT(dividendWidth, dividerWidth, n),
        Seq(chiseltest.internal.NoThreadingAnnotation,
        chiseltest.simulator.WriteVcdAnnotation)){
          dut: SRT =>
//          dut.clock.setTimeout(0)
          dut.input.valid.poke(true.B)
          dut.input.bits.dividend.poke("b01111000".U)
          dut.input.bits.divider.poke( "b01100000".U)
//            dut.input.bits.dividend.poke("b01111000".U)
//            dut.input.bits.divider.poke( "b01100000".U)
          dut.input.bits.counter.poke(counter.U)
          dut.clock.step()
          dut.input.valid.poke(false.B)
          var flag = false
          for(a <- 1 to 20 if !flag) {
            if(dut.output.valid.peek().litValue == 1) {
              flag = true
              dut.clock.step()
              dut.output.bits.quotient.expect(0.U)
              dut.output.bits.reminder.expect("b01111000".U)
//              dut.output.bits.quotient.expect(1.U)
//              dut.output.bits.reminder.expect("b11000".U)
            }
            dut.clock.step()
          }
            utest.assert(flag)
      }
    }
  }
}