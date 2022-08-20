package crypto.modmul

import chisel3._
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

object MontgomerySpec extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Montgomery should pass") {
      var specfic_test = false
      if (specfic_test) {
        var a = BigInt("643")
        var b = BigInt("3249")
        var p = BigInt("3323")
        var width = 64
        var R = BigInt("4294967296")
        var R_inv = BigInt("806")
        var addPipe = 10
        var res = (a) * (b) * (R_inv) % (p)
        var R_div_2 = (R) / BigInt(2)
        var random_width = 0
        println("addPipe", addPipe)
        println(width, random_width)
        println("res = ", res)
        testCircuit(new Montgomery(64, addPipe), Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)){dut: Montgomery =>
          dut.clock.setTimeout(0)
          dut.p.poke(p.U)
          dut.pPrime.poke(true.B)
          dut.a.poke(a.U)
          dut.b.poke(b.U)
          dut.input_width.poke(R_div_2.U)
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
              // need to wait a cycle because there is s5 -> s6 or s4 -> s6 in the state machine
              dut.clock.step()
              utest.assert(dut.out.peek().litValue == res)
            }
          }
          utest.assert(flag)
        }
      } else {
        var u = new Utility()
        var length = scala.util.Random.nextInt(20) + 10 // (10, 30)
        var p = u.randPrime(length)
        var a = scala.util.Random.nextInt(p)
        var b = scala.util.Random.nextInt(p)
        var width = p.toBinaryString.length
        var R = (scala.math.pow(2, width)).toInt
        var R_inv = u.modinv(R, p)
        var addPipe = scala.util.Random.nextInt(10) + 1      
        var res = BigInt(a) * BigInt(b) * BigInt(R_inv) % BigInt(p)
        var R_div_2 = (scala.math.pow(2, width-1)).toInt
        var random_width = scala.util.Random.nextInt(20) + 10 // to test if bigger length hardware can support smaller length number
        println("addPipe", addPipe)
        println(width, random_width)
        println("res = ", res)
        testCircuit(new Montgomery(64, addPipe), Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)){dut: Montgomery =>
          dut.clock.setTimeout(0)
          dut.p.poke(p.U)
          dut.pPrime.poke(true.B)
          dut.a.poke(a.U)
          dut.b.poke(b.U)
          dut.input_width.poke(R_div_2.U)
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
              // need to wait a cycle because there is s5 -> s6 or s4 -> s6 in the state machine
              dut.clock.step()
              utest.assert(dut.out.peek().litValue == res)
            }
          }
          utest.assert(flag)
        }
      }
    }
  }
}
