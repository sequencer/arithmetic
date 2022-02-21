package crypto.chacha

import chisel3._
import chisel3.util.Decoupled

class ChaCha extends Module {
  val head = "expand 32-byte k".grouped(4).map(_.getBytes("ascii"))

  // allocate key for this ChaCha Module
  // outer nonce should be updated as well.
  // for each hypervisor, there should be a ChaCha Pool.
  // updated by MMIO Store
  val key = IO(Input(Vec(2, Vec(4, UInt(32.W)))))
  // Should be ASID tag captured from page info
  // should be an additional userbits in TileLink protocol
  val nonce = IO(Input(UInt(96.W)))
  // key and counter is valid can start to calculate.
  val valid = IO(Input(Bool()))

  // used by nonce-pool
  val output = IO(Decoupled(new Bundle {
    val nonce = Vec(4, UInt(32.W))
    val x = Vec(4, Vec(4, UInt(32.W)))
  }))

  // Internal generated nonce which will be used for encrypt and decrypt.
  val counter = RegInit(0.U(32.W))
  val update = RegInit(false.B)
  // nonce update logic
  nonce := Mux(update, counter + 1.U, counter)

  val matrix: Seq[Seq[UInt]] = Seq.fill(4)(Seq.fill(4)(RegInit(0.U(32.W))))

  val state = RegInit(0.U(4.W))
  val flag: Bool = state(0) || state(2)

  matrix.map { row =>
    val a = row(0)
    val b = row(1)
    val c = row(2)
    val d = row(3)
    val adder = Module(new Add)
    val xor = Module(new Xor)

    adder.a := Mux(flag, a, c)
    adder.b := Mux(flag, b, d)
    xor.a := Mux(flag, d, b)
    xor.b := Mux(flag, a, c)

  }
}

class Add extends Module {
  val a = IO(Input(UInt(32.W)))
  val b = IO(Input(UInt(32.W)))
  val c = IO(Output(UInt(32.W)))
  c := a +% b
}

class Xor extends Module {
  val a = IO(Input(UInt(32.W)))
  val b = IO(Input(UInt(32.W)))
  val c = IO(Output(UInt(32.W)))
  c := a ^ b
}
