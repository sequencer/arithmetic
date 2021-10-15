package multiplier

import addition.prefixadder.common.BrentKungSum
import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object WallaceMultiplierTester extends TestSuite with ChiselUtestTester {
  val tests: Tests = Tests {
    test("3*2 on Wallace") {
      val width = 6;
      testCircuit(new WallaceMultiplier(width)()) { dut =>
        for (i <- -5 until 5) {
          for (j <- -5 until 5) {
            dut.a.poke(i.S)
            dut.b.poke(j.S)
            dut.clock.step()
            dut.z.expect((i * j).S)
          }
        }
      }
    }
  }
}
