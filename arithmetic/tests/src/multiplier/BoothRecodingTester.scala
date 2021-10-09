package multiplier

import chisel3._
import chisel3.tester.{testableData, ChiselUtestTester}
import utest._

object BoothRecodingTester extends TestSuite with ChiselUtestTester {
  val tests: Tests = Tests {
    test("encoding len") {
      testCircuit(new Booth(16)(8)) { dut =>
        dut.input.poke(7.U)
        println(dut.output.peek())
      }
    }
  }
}
