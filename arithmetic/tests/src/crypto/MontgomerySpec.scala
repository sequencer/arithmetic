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
        ChiselGeneratorAnnotation(() => new Montgomery(4096, 2, 1)),
      )).collectFirst{case EmittedFirrtlCircuitAnnotation(e) => }
    }
    test("Montgomery should generate system verilog") {
      (new ChiselStage).execute(Array("-X", "sverilog"), Seq(
        ChiselGeneratorAnnotation(() => new Montgomery(4096, 2, 1)),
      )).collectFirst{case EmittedVerilogCircuitAnnotation(e) => }
    }
//    test("Montgomery behavior") {
//      testCircuit(new Barrett(19260817, 4, 4), Seq(chiseltest.simulator.WriteVcdAnnotation)){dut: Montgomery =>
//        dut.input.bits.a.poke(7.U)
//        dut.input.bits.b.poke(9.U)
//        dut.input.valid.poke(true.B)
//        println("init", dut.z.bits.peek().litValue, dut.z.valid.peek().litValue)
//        for(a <- 1 to 100) {
//          dut.clock.step()
//           println(a, dut.z.bits.peek().litValue, dut.z.valid.peek().litValue, dut.z.ready.peek().litValue)
//        }
//      }
//    }
  }
}
