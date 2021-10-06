package crypto.modmul

import chisel3._
import chiseltest.iotesters._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class BarrettSpecTester(c: Barrett) extends PeekPokeTester(c) {
  for {
//    i <- -10 to 10
//    j <- -10 to 10
    i <- -10 to -9
    j <- -10 to -8
  } {
	val io = new Bundle {
		// val a = Input(UInt(9.W))
		// val b = Input(UInt(9.W))
		// val z = Output(UInt(9.W))
		var a = i
		var b = j
  	}
    poke(c.input, io)
    step(1)
    println(s"signed adder $i + $j got ${peek(c.z)} should be ${i+j}")
    expect(c.z, i + j)
    step(1)
  }

}

class BarrettSpecClass extends AnyFreeSpec with Matchers {
  "tester should returned signed values with interpreter" in {
    Driver.execute(Array("--backend-name", "firrtl", "--target-dir", "test_run_dir"), () => new Barrett(19260817, 1, 1)) { c =>
      new BarrettSpecTester(c)
    } should be (true)
  }

  //TODO: make this work
  "tester should returned signed values" ignore {
    Driver.execute(Array("--backend-name", "verilator", "--target-dir", "test_run_dir"), () => new Barrett(19260817, 1, 1)) { c =>
      new BarrettSpecTester(c)
    } should be (true)
  }
}
