package arithmetic.addition.csa

import chisel3._
import chisel3.tester._
import utest._
import scala.annotation.tailrec

trait HasCSASpec {
  def CSASpec[T <: CarrySaveAdder](dut: T): Unit = {
    val maxPerInput: Int = 1 << dut.width
    val max = math.pow(maxPerInput, dut.m)

    @tailrec
    def test(n: Int): Unit = {
      var refSum = 0
      for((in, i) <- dut.io.in.zipWithIndex){
        val x = (n >> dut.m * i) % maxPerInput
        refSum += x
        in.poke(x.U)
      }
      dut.clock.step(1)
      // y has higher priority than x
      val dutSum = dut.io.out.map(_.peek().litValue()).reduce((x, y) => x + (y << 1))
      require(dutSum == refSum)

      if((n + 1) < max){
        test(n + 1)
      }
    }

    test(0)
  }
}

object CSASpecTester extends ChiselUtestTester with HasCSASpec {
  val width = 3
  val tests: Tests = Tests {
    test("half adder(2-2 carry save adder) should pass"){
      testCircuit(new HalfAdder(width))(CSASpec)
    }
    test("3-2 carry save adder should pass"){
      testCircuit(new CarrySaveAdder3_2(width))(CSASpec)
    }
    test("5-3 carry save adder should pass"){
      testCircuit(new CarrySaveAdder5_3(width))(CSASpec)
    }
  }
}
