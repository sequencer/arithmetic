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
      val p = 7681
      var a = scala.util.Random.nextInt(p)
      var b = scala.util.Random.nextInt(p)
      val res = a * b % p
      var addPipe = scala.util.Random.nextInt(10)
      var mulPipe = scala.util.Random.nextInt(10)
      if(addPipe == 0) {
        addPipe = addPipe + 1        
      }
      if(mulPipe == 0) {
        mulPipe = mulPipe + 1
      }
      testCircuit(new Barrett(p, addPipe, mulPipe), Seq(chiseltest.simulator.WriteVcdAnnotation)){dut: Barrett =>
        dut.input.bits.a.poke(a.U)
        dut.input.bits.b.poke(b.U)
        dut.input.valid.poke(true.B)
        println(a, b)
        for(a <- 1 to 100) {
          dut.clock.step()
          if(dut.z.valid.peek().litValue == 1) {
            utest.assert(dut.z.bits.peek().litValue == res)
          }
        }
      }
    }
  }
}
