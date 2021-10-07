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
      )).collectFirst{case EmittedFirrtlCircuitAnnotation(e) => }
    }
    test("barrett should generate system verilog") {
      (new ChiselStage).execute(Array("-X", "sverilog"), Seq(
        ChiselGeneratorAnnotation(() => new Barrett(19260817, 1, 1)),
      )).collectFirst{case EmittedVerilogCircuitAnnotation(e) => }
    }
    test("barrett behavior") {
      testCircuit(new Barrett(19260817, 4, 4)){dut: Barrett =>
        dut.input.bits.a.poke(7.U)
        dut.input.bits.b.poke(9.U)
        println("init", dut.z.bits.peek().litValue, dut.z.valid.peek().litValue)
        for(a <- 1 to 50) {
          dut.clock.step()
          println(a, dut.z.bits.peek().litValue, dut.z.valid.peek().litValue, dut.z.ready.peek().litValue)
        }
      }
    }
  }
}
