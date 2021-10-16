package multiplier

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object WallaceMultiplierTester extends TestSuite with ChiselUtestTester {
  val tests: Tests = Tests {
    test("more data") {
      for (width <- 8 until 11) {
        testCircuit(new WallaceMultiplier(width)()) { dut =>
          for (i <- -31 until 32) {
            for (j <- -31 until 32) {
              println(s"test (width = $width) $i * $j = ${dut.z.peek}")
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
}
