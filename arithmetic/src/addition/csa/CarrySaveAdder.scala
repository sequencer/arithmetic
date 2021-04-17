package arithmetic.addition.csa

import chisel3._
import chisel3.util._

trait BitCompress {
  def compressor: Int => Compressor
}

class CarrySaveAdderIO(val m: Int, val n: Int)(val len: Int) extends Bundle {
  val in = Input(Vec(m, UInt(len.W)))
  val out = Output(Vec(n, UInt(len.W)))
}

class CompressorIO(val m: Int, val n: Int) extends Bundle {
  val in = Input(Vec(m, Bool()))
  val out = Output(Vec(n, Bool()))
}

abstract class Compressor(val m: Int, val n: Int) extends Module {
  val io = IO(new CompressorIO(m, n))
}

abstract class CarrySaveAdder(val m: Int, val n: Int)(val width: Int) extends Module with BitCompress {
  override val desiredName: String = this.getClass.getSimpleName + s"_$width"

  val io = IO(new CarrySaveAdderIO(m, n)(width))
  val compressorMods = Seq.tabulate(width)(i => Module(compressor(i)))
  val result = Wire(Vec(width, Vec(n, Bool())))
  for (((res, cmp), idx) <- result.zip(compressorMods).zipWithIndex) {
    cmp.io.in := io.in.map(_(idx))
    res := cmp.io.out
  }
  io.out.zipWithIndex.foreach { case (out, idx) => out := Cat(result.reverseMap(_(idx))) }
}

trait BitCompress2_2 extends BitCompress { this: CarrySaveAdder =>
  override def compressor: Int => Compressor = (_: Int) =>
    new Compressor(m, n) {
      require(io.in.size == m)
      require(io.out.size == n)
      io.out := VecInit(Seq(io.in.head ^ io.in.last, io.in.head & io.in.last))
    }
}

class HalfAdder(width: Int) extends CarrySaveAdder(2, 2)(width) with BitCompress2_2

trait BitCompress3_2 extends BitCompress { this: CarrySaveAdder =>
  override def compressor: Int => Compressor = (_: Int) =>
    new Compressor(m, n) {
      val a :: b :: c :: Nil = io.in.toList
      val a_xor_b = a ^ b
      val a_and_b = a & b
      val sum = a_xor_b ^ c
      val carry = a_and_b | (a_xor_b & c)
      io.out := VecInit(Seq(sum, carry))
    }
}

class CarrySaveAdder3_2(width: Int) extends CarrySaveAdder(3, 2)(width) with BitCompress3_2

trait BitCompress5_3 extends BitCompress { this: CarrySaveAdder =>
  override def compressor: Int => Compressor = (_: Int) =>
    new Compressor(m, n) {
      val a :: b :: c :: d :: e :: Nil = io.in.toList
      val sum_abc = a +& b +& c
      val sum_all = sum_abc + d + e
      io.out := sum_all.asBools()
    }
}

class CarrySaveAdder5_3(width: Int) extends CarrySaveAdder(5, 3)(width) with BitCompress5_3
