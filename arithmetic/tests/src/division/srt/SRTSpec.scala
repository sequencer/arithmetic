package division.srt

import utest._



object SRTSpec extends TestSuite{
  override def tests: Tests = Tests {
    test("SRT should draw PD") {
      val srt = SRTTable(4, 2, 4, 4)
//      val table = srt.tables.flatMap {
//      case (i, ps) => ps.flatMap{ case (d, xs) => xs.map(x => (d.toDouble, x.toDouble*16)) }}.groupBy(_._1)
//      table.map{case (x, y) => println(y)}
      srt.dumpGraph(srt.pd, os.root / "tmp" / "srt4-2-4-4.png")
    }
  }
}
