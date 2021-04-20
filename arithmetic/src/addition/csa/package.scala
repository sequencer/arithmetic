package arithmetic.addition

package object csa {
  class HalfAdder(width: Int) extends CarrySaveAdder(2, 2, _ => common.CSACompressor2_2)(width)
  class CarrySaveAdder3_2(width: Int) extends CarrySaveAdder(3, 2, _ => common.CSACompressor3_2)(width)
  class CarrySaveAdder5_3(width: Int) extends CarrySaveAdder(5, 3, _ => common.CSACompressor5_3)(width)
}
