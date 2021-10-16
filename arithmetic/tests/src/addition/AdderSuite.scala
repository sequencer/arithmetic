package addition

import chiseltest.formal.BoundedCheck
import formal.FormalSuite

trait AdderSuite extends FormalSuite {
  def formalFullAdder(dut: () => FullAdder)(implicit testPath: utest.framework.TestPath) = verify(dut, Seq(BoundedCheck(0)))
}
