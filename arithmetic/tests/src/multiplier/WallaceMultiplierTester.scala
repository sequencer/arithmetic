package multiplier

import chisel3._
import chisel3.tester.{testableData, ChiselUtestTester}
import utest._

object WallaceMultiplierTester extends TestSuite with ChiselUtestTester {
  val tests: Tests = Tests {
    test("3*2 on Wallace") {
      testCircuit(new WallaceMultiplier(4)()) { dut =>
        dut.a.poke(3.U)
        dut.b.poke(2.U)
        println(dut.z.peek())
      }
    }
  }
}
