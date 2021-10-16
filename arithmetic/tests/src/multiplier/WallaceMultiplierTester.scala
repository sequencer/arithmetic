package multiplier

import chiseltest.formal.BoundedCheck
import formal.FormalSuite
import utest._

object FormalWallaceTester extends FormalSuite {
  val tests: Tests = Tests {
    test("wallace multiplier 1") {
      verify(new WallaceMultiplier(1)(), Seq(BoundedCheck(1)))
    }
    test("wallace multiplier 4") {
      verify(new WallaceMultiplier(4)(), Seq(BoundedCheck(1)))
    }
    test("wallace multiplier 7") {
      verify(new WallaceMultiplier(7)(), Seq(BoundedCheck(1)))
    }
  }
}
