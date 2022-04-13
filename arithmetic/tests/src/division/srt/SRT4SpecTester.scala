package division.srt

import chisel3._
import chisel3.tester.{ChiselUtestTester}

object SRT4SpecTester extends TestSuite with ChiselUtestTester{
    def tests: Tests = Tests{
        test("SRT4 should pass"){
            val u = ???

            testCircuit(new SRT(32, 32, 32), Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)){ dut: SRT =>

            }
        }
    }

}