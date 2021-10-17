package multiplier

import chiseltest.formal.BoundedCheck
import formal.FormalSuite
import utest._

object WallaceMultiplierTester extends FormalSuite {
  val tests: Tests = Tests {
    test("wallace signed multiplier 1") {
      verify(new SignedWallaceMultiplier(1)(), Seq(BoundedCheck(1)))
    }
    test("wallace signed multiplier 4") {
      verify(new SignedWallaceMultiplier(4)(), Seq(BoundedCheck(1)))
    }
    test("wallace signed multiplier 7") {
      verify(new SignedWallaceMultiplier(7)(), Seq(BoundedCheck(1)))
    }
    test("wallace unsigned multiplier 1") {
      verify(new UnsignedWallaceMultiplier(1)(), Seq(BoundedCheck(1)))
    }
    test("wallace unsigned multiplier 4") {
      verify(new UnsignedWallaceMultiplier(4)(), Seq(BoundedCheck(1)))
    }
    test("wallace unsigned multiplier 7") {
      verify(new UnsignedWallaceMultiplier(7)(), Seq(BoundedCheck(1)))
    }
  }
}
