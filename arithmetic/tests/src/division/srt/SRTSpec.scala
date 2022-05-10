package division.srt

import utest._


object SRTSpec extends TestSuite{
  override def tests: Tests = Tests {
    test("SRT should draw PD") {
      val srt = SRTTable(4, 2, 5, 5)
      srt.dumpGraph(srt.pd, os.root / "tmp" / "srt4-2-5-5.png")
    }
  }
}
