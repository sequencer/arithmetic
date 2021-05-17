package addition

import formal.FormalSuite

trait AdderSuite extends FormalSuite {
  def formalFullAdder(dut: () => FullAdder, name: String) = formal(dut, name, success, 0, Nil)
}
