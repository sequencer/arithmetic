package crypto.modmul

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import firrtl.{EmittedFirrtlCircuitAnnotation, EmittedVerilogCircuitAnnotation}
import utest._

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
      val p = 41
      val R_inv = 25
      val width = 6
      val addPipe = 1
      var a = scala.util.Random.nextInt(p)
      var b = scala.util.Random.nextInt(p)
      var res = a * b * R_inv % p
      testCircuit(new Montgomery(width, addPipe), Seq(chiseltest.simulator.WriteVcdAnnotation)){dut: Montgomery =>
        dut.p.poke(p.U)
        dut.pPrime.poke(true.B)
        dut.a.poke(a.U)
        dut.b.poke(b.U)
        dut.b_add_p.poke((p+b).U)
        dut.clock.step()
        dut.valid.poke(true.B)

        for(a <- 1 to 100) {
          dut.clock.step()
          if(dut.out_valid.peek().litValue == 1) {
            utest.assert(dut.out.peek().litValue == res)
          }
        }
      }
    }
  }
}
