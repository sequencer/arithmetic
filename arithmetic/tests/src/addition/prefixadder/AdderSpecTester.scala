package addition.prefixadder

import addition.AdderSuite
import arithmetic.addition.prefixadder.common.{BrentKungAdder, KoggeStoneAdder, RippleCarry3Adder, RippleCarryAdder}
import utest._

object AdderSpecTester extends AdderSuite {
  val width = 8
  // TODO: utest only support static test(cannot use map function), maybe we can try to use macro to implement this.
  val tests: Tests = Tests {
    test("ripple carry should pass") {
      formalFullAdder(() => new RippleCarryAdder(width), "ripple_carry")
    }
    test("ripple carry 3 fan-in should pass") {
      formalFullAdder(() => new RippleCarry3Adder(width), "ripple_carry3")
    }
    test("kogge stone should pass") {
      formalFullAdder(() => new KoggeStoneAdder(width), "kogge_stone")
    }
    test("brent kung should pass") {
      formalFullAdder(() => new BrentKungAdder(width), "ripple_carry3")
   }
  }
}
