package crypto.modmul

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import firrtl.{EmittedFirrtlCircuitAnnotation, EmittedVerilogCircuitAnnotation}
import utest._
//import org.apache.commons.math3.primes.Primes.nextPrime

object MontgomerySpec extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Montgomery should generate lofirrtl") {
      (new ChiselStage).execute(Array("-X", "low"), Seq(
        ChiselGeneratorAnnotation(() => new Montgomery(4096, 1)),
      )).collectFirst{case EmittedFirrtlCircuitAnnotation(e) => }
    }
    test("Montgomery should generate system verilog") {
      (new ChiselStage).execute(Array("-X", "sverilog"), Seq(
        ChiselGeneratorAnnotation(() => new Montgomery(4096, 1)),
      )).collectFirst{case EmittedVerilogCircuitAnnotation(e) => }
    }
    test("Montgomery behavior") {
      val u = new Utility()
      val length = scala.util.Random.nextInt(30) + 1 // avoid 0
      val p = u.randPrime(length)
      val width = p.toBinaryString.length
      val R_inv = u.modinv((scala.math.pow(2, width)).toInt, p)
      val addPipe = scala.util.Random.nextInt(10) + 1
      var a = scala.util.Random.nextInt(p)
      var b = scala.util.Random.nextInt(p)
      val res = BigInt(a) *BigInt(b) * BigInt(R_inv) % BigInt(p)
      println("Parameter" ,p, width, R_inv, a, b, res)

      testCircuit(new Montgomery(width, addPipe), Seq(chiseltest.simulator.VcsBackendAnnotation)){dut: Montgomery =>
        dut.clock.setTimeout(0)
        dut.p.poke(p.U)
        dut.pPrime.poke(true.B)
        dut.a.poke(a.U)
        dut.b.poke(b.U)
        dut.b_add_p.poke((p+b).U)
        dut.clock.step()
        dut.valid.poke(true.B)

        for(a <- 1 to 2000) {
          dut.clock.step()
          if(dut.out_valid.peek().litValue == 1) {
            println("Parameter" ,p, width, R_inv, a, b, res)
            utest.assert(dut.out.peek().litValue == res)
          }
        }
      }
    }
  }
}
