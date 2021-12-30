package crypto.modmul

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object MontgomerySpec extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("montgomery") {
      val u = new Utility()
      val length = scala.util.Random.nextInt(20) + 10 // (10, 30)
      var p = u.randPrime(length)
      
      var width = p.toBinaryString.length
      var R_inv = u.modinv((scala.math.pow(2, width)).toInt, p)
      var addPipe = scala.util.Random.nextInt(10) + 1
      var a = scala.util.Random.nextInt(p)
      var b = scala.util.Random.nextInt(p)

      // p = 860011
      // width = 20
      // R_inv = 479292
      // a = 690112
      // b = 695455      
      val res = BigInt(a) * BigInt(b) * BigInt(R_inv) % BigInt(p)
      println("Parameter" ,p, width, R_inv, a, b, res)

      testCircuit(new Montgomery(width, addPipe), Seq(chiseltest.simulator.VcsBackendAnnotation, chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteFsdbAnnotation)){dut: Montgomery =>
        //  testCircuit(new Montgomery(width, addPipe), Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)){dut: Montgomery =>
        dut.clock.setTimeout(0)
        dut.p.poke(p.U)
        dut.pPrime.poke(true.B)
        dut.a.poke(a.U)
        dut.b.poke(b.U)
        dut.b_add_p.poke((p+b).U)
        dut.clock.step()
        dut.clock.step()
        // delay two cycles then set valid = true
        dut.valid.poke(true.B)
        dut.clock.step()
        var flag = false
        for(a <- 1 to 1000) {
          dut.clock.step()
          if(dut.out_valid.peek().litValue == 1) {
            flag = true
            print(dut.out.peek().litValue)
            // need to wait a cycle because there is s5 -> s6 or s4 -> s6 in the state machine
            dut.clock.step()
            utest.assert(dut.out.peek().litValue == res)
          }
        }
        println("Parameter" ,p, width, R_inv, a, b, res)
        utest.assert(flag)
      }
    }
  }
}
