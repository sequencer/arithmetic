package arithmetic.addition.csa

import chisel3._
import chisel3.tester._
import utest._
import scala.annotation.tailrec

trait HasCSASpec {
  def CSASpec[T <: CarrySaveAdder](dut: T): Unit = {
    val maxPerInput: Int = 1 << dut.width
    val max = math.pow(maxPerInput, dut.m)

    val input = Array.fill(dut.m)(0)
    @tailrec
    def test(n: Int): Unit = {
      var refSum = 0
      for((in, i) <- dut.io.in.zipWithIndex){
        // convert radix from 10 to maxPerInput
        // new_bit = (raw / maxPerInput) mod maxPerInput
        val x = (n >> (dut.width * i)) % maxPerInput
        refSum += x
        input(i) = x
        in.poke(x.U)
      }
      dut.clock.step(1)
      val output = dut.io.out.map(_.peek().litValue())
      val dutSum = output.zip(dut.outputWeights).map({
        case (v, w) => v * w
      }).sum
      require(dutSum == refSum, s"input: ${input.mkString(" ")} output:${output.mkString(" ")}\n" +
        s"refSum: $refSum dutSum: $dutSum"
      )

      if((n + 1) < max){
        test(n + 1)
      }
    }

    test(0)
  }
}

object CSASpecTester extends ChiselUtestTester with HasCSASpec {
  val width = 1
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
