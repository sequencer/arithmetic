package crypto.modmul

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import firrtl.{EmittedFirrtlCircuitAnnotation, EmittedVerilogCircuitAnnotation}
import utest._

object BarrettSpec extends TestSuite with ChiselUtestTester {
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
    test("barrett behavior") {
      testCircuit(new Barrett(19260817, 1, 1)){dut: Barrett =>
        dut.input.bits.a.poke(1.U)
        dut.input.bits.b.poke(1.U)
        println(dut.z.bits.peek().litValue)
        dut.clock.step()
      }
    }
  }
}