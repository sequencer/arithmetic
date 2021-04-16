package adder.tests


import arithmetic.addition.prefixadder.PrefixAdder
import arithmetic.addition.prefixadder.common.{RippleCarryAdder, RippleCarry3Adder, KoggeStoneAdder, BrentKungAdder}
import chisel3._
import chisel3.tester._
import utest._

trait HasAdderSpec {
  def AdderSpec(dut: PrefixAdder): Unit = {
    val max: Int = (1 << dut.width) - 1
    (0 to max).map { a =>
      (0 to max).map { b =>
        dut.io.a.poke(a.U)
        dut.io.b.poke(b.U)
        dut.io.z.expect((a+b).U)
        dut.clock.step()
      }
    }
  }
}

object AdderSpecTester extends ChiselUtestTester with HasAdderSpec {
  val width = 8
  // TODO: utest only support static test(cannot use map function), maybe we can try to use macro to implement this.
  val tests: Tests = Tests {
    test("ripple carry should pass") {
      testCircuit(new RippleCarryAdder(width))(AdderSpec)
    }
    test("ripple carry 3 fan-in should pass") {
      testCircuit(new RippleCarry3Adder(width))(AdderSpec)
    }
    test("kogge stone should pass") {
      testCircuit(new KoggeStoneAdder(width))(AdderSpec)
    }
    test("brent kung should pass") {
      testCircuit(new BrentKungAdder(width))(AdderSpec)
    }
  }
}
