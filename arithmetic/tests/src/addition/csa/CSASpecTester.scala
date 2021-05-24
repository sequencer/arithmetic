package addition.csa

import addition.csa.common.{CSACompressor2_2, CSACompressor3_2, CSACompressor5_3}
import formal.FormalSuite
import utest._

object CSASpecTester extends FormalSuite {
  def tests: Tests = Tests {
    test("CSA53") {
      formal(() => new CarrySaveAdder(5, 3, _ => CSACompressor5_3)(4), "CSA53", success)
    }
    test("CSA32") {
      formal(() => new CarrySaveAdder(3, 2, _ => CSACompressor3_2)(4), "CSA32", success)
    }
    test("CSA22") {
      formal(() => new CarrySaveAdder(2, 2, _ => CSACompressor2_2)(4), "CSA22", success)
    }
  }
}
