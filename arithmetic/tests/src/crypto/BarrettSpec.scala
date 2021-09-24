package crypto.modmul

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import firrtl.{EmittedFirrtlCircuitAnnotation, EmittedVerilogCircuitAnnotation}
import utest._


object BarrettSpec extends TestSuite {
  def tests: Tests = Tests {
    test("barrett should generate lofirrtl") {
      (new ChiselStage).execute(Array("-X", "low"), Seq(
        ChiselGeneratorAnnotation(() => new Barrett(19260817, 1, 1)),
      )).collectFirst{case EmittedFirrtlCircuitAnnotation(e) => println(e)}
    }
    test("barrett should generate system verilog") {
      (new ChiselStage).execute(Array("-X", "sverilog"), Seq(
        ChiselGeneratorAnnotation(() => new Barrett(19260817, 1, 1)),
      )).collectFirst{case EmittedVerilogCircuitAnnotation(e) => println(e)}
    }
  }
}