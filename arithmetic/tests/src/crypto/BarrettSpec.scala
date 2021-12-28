package crypto.modmul

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object BarrettSpec extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("barrett") {
      val u = new Utility()
      val length = scala.util.Random.nextInt(30) + 2 // avoid 0 and 1
      val p = u.randPrime(length)
      var a = scala.util.Random.nextInt(p)
      var b = scala.util.Random.nextInt(p)
      val res = BigInt(a) * BigInt(b) % BigInt(p)
      var addPipe = scala.util.Random.nextInt(10) + 1
      var mulPipe = scala.util.Random.nextInt(10) + 1

      testCircuit(new Barrett(p, addPipe, mulPipe), Seq(chiseltest.simulator.VcsBackendAnnotation, chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteFsdbAnnotation)){dut: Barrett =>
      dut.clock.setTimeout(0)
        dut.input.bits.a.poke(a.U)
        dut.input.bits.b.poke(b.U)
        dut.input.valid.poke(true.B)
        println(a, b)
        for(a <- 1 to 200) {
          dut.clock.step()
          if(dut.z.valid.peek().litValue == 1) {
            utest.assert(dut.z.bits.peek().litValue == res)
          }
        }
      }
    }
  }
}
