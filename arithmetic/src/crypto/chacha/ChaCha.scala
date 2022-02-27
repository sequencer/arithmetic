package crypto.chacha

import chisel3._
import chisel3.util.{Decoupled, Mux1H, PopCount}

case class ChaChaParameter(nonceWidth: Int)
class ChaCha(parameter: ChaChaParameter) extends Module {
  val head = "expand 32-byte k".grouped(4).map(_.getBytes("ascii"))

  // allocate key for this ChaCha Module
  // outer nonce should be updated as well.
  // for each hypervisor, there should be a ChaCha Pool.
  // updated by MMIO Store
  val key = IO(Input(Vec(2, Vec(4, UInt(32.W)))))
  // Should be ASID tag captured from page info
  // should be an additional userbits in TileLink protocol
  val nonce = IO(Input(UInt(parameter.nonceWidth.W)))
  // key and counter is valid can start to calculate.
  val valid = IO(Input(Bool()))

  // used by nonce-pool
  val output = IO(Decoupled(new Bundle {
    val nonce = Vec(4, UInt(32.W))
    val x = Vec(4, Vec(4, UInt(32.W)))
  }))

  // Internal generated nonce which will be used for encrypt and decrypt.
  val counter = RegInit(0.U((128 - parameter.nonceWidth).W))
  val update = RegInit(false.B)
  // nonce update logic
  nonce := Mux(update, counter + 1.U, counter)

  val matrix: Seq[Seq[UInt]] = Seq.fill(4)(Seq.fill(4)(RegInit(0.U(32.W))))

  val state = RegInit(1.U(4.W))
  assert(PopCount(state) === 1.U(4.W), "state must be OneHot.")
  state := state << 1

  val rounds = RegInit(0.U(5.W))
  rounds := Mux(rounds === 19.U, 0.U, rounds + 1.U)
  val control = chisel3.util.experimental.decode.decoder(
    state,
    chisel3.util.experimental.decode.TruthTable.fromString(
      s"""???1->11000100110
         |??1?->00101001001
         |?1??->11000010110
         |1???->00011000000
         |      ???????????""".stripMargin
    )
  )
  assert(rounds < 20.U, "rounds should less then 20")
  val flag:           Bool = control(0)
  val updateA:        Bool = control(1)
  val updateBShift12: Bool = control(2)
  val updateBShift7:  Bool = control(3)
  val updateC:        Bool = control(4)
  val updateDShift16: Bool = control(5)
  val updateDShift8:  Bool = control(6)
  val keepA:          Bool = control(7)
  val keepB:          Bool = control(8)
  val keepC:          Bool = control(9)
  val keepD:          Bool = control(10)

  val permuteOdd:     Bool = rounds(0) && state(3)
  val permuteEven:    Bool = !rounds(0) && state(3)

  // Data path
  matrix.foreach { row =>
    val a = row(0)
    val b = row(1)
    val c = row(2)
    val d = row(3)
    val adder = Module(new Add)
    val xor = Module(new Xor)
    adder.a := Mux(flag, a, c)
    adder.b := Mux(flag, b, d)
    xor.a := Mux(flag, d, b)
    xor.b := adder.c
    a := Mux1H(
      Map(
        updateA -> adder.c,
        permuteOdd -> ???, // matrix()()
        permuteEven -> ???, // matrix()()
        keepA -> a
      )
    )
    b := Mux1H(
      Map(
        updateBShift12 -> xor.c.rotateLeft(12),
        updateBShift7 -> xor.c.rotateLeft(7),
        permuteOdd -> ???, // matrix()()
        permuteEven -> ???, // matrix()()
        keepB -> b
      )
    )
    c := Mux1H(
      Map(
        updateC -> adder.c,
        permuteOdd -> ???, // matrix()()
        permuteEven -> ???, // matrix()()
        keepC -> c
      )
    )
    d := Mux1H(
      Map(
        updateDShift16 -> xor.c.rotateLeft(16),
        updateDShift8 -> xor.c.rotateLeft(8),
        permuteOdd -> ???, // matrix()()
        permuteEven -> ???, // matrix()()
        keepD -> d
      )
    )
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
