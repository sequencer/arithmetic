package division.srt

import utest._
import chisel3._
import utils.extend



object SRTSpec extends TestSuite{
  override def tests: Tests = Tests {
    test("SRT should draw PD") {
      val srt = SRTTable(4,2,4,4)
//      println(srt.tables)
//      println(srt.tablesToQDS)
      srt.dumpGraph(srt.pd, os.root / "tmp" / "srt4-2-4-4.png")
    }
  }
}
