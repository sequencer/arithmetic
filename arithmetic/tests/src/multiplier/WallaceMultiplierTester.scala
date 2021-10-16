package multiplier

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object WallaceMultiplierTester extends TestSuite with ChiselUtestTester {
  val tests: Tests = Tests {
    test("3*2 on Wallace") {
      for (width <- 6 until 9) {
        testCircuit(new WallaceMultiplier(width)()) { dut =>
          dut.a.poke(-1.S)
          dut.b.poke(-1.S)
          println(dut.z.peek)
//          for (i <- -31 until 32) {
//            for (j <- -31 until 32) {
//              dut.a.poke(i.S)
//              dut.b.poke(j.S)
//              dut.clock.step()
//              dut.z.expect((i * j).S)
//            }
//          }
        }
      }
    }
  }
}
