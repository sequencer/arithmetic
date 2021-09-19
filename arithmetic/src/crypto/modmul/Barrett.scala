package crypto.modmul

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.{Counter, Mux1H}

class Barrett(val p: BigInt,
              val mulPipe: Int,
              val addPipe: Int) extends ModMul {
  val m = BigInt(2) << ((BigInt(2) * width) / p ).toInt
  val mul = Module(new DummyMul(width + 1, mulPipe))
  val add = Module(new DummyAdd(width * 2, addPipe))
  val q = RegInit(0.U((width + 1).W))
  val r = RegInit(0.U((width * 2).W))

  // Control Path
  object StateType extends ChiselEnum {
    val s0 = Value("b00000001".U)
    val s1 = Value("b00000010".U)
    val s2 = Value("b00000100".U)
    val s3 = Value("b00001000".U)
    val s4 = Value("b00010000".U)
    val s5 = Value("b00100000".U)
    val s6 = Value("b01000000".U)
    val s7 = Value("b10000000".U)
  }
  val isMul = (state.asUInt() & "b00001110".U).orR()
  val isAdd = (state.asUInt() & "b01110000".U).orR
  val mulDone = if (mulPipe != 0) Counter(isMul, mulPipe)._2 else true.B
  val addDone = if (mulPipe != 0) Counter(isAdd, mulPipe)._2 else true.B
  // TODO: check here.
  val addSign = add.z.head(0)
  val state = RegInit(StateType.s0)
  state := chisel3.util.experimental.decode.decoder(
    state.asUInt() ## mulDone ## addDone ## addSign ## input.valid ## z.ready, {
    val Y = "1"
    val N = "0"
    val DC = "?"
    def to(
      stateI:     String,
      mulDone:    String = DC,
      addDone:    String = DC,
      addSign:    String = DC,
      inputValid: String = DC,
      zReady:     String = DC
    )(stateO:     String
    ) = s"$stateI$mulDone$addDone$addSign$inputValid$zReady->$stateO"
    val s0 = "00000001"
    val s1 = "00000010"
    val s2 = "00000100"
    val s3 = "00001000"
    val s4 = "00010000"
    val s5 = "00100000"
    val s6 = "01000000"
    val s7 = "10000000"
    TruthTable(Seq(
      to(s0, inputValid = Y)(s1),
      to(s0, inputValid = N)(s0),
      to(s1, mulDone = Y)(s2),
      to(s1, mulDone = N)(s1),
      to(s2, mulDone = Y)(s3),
      to(s2, mulDone = N)(s2),
      to(s3, mulDone = Y)(s4),
      to(s3, mulDone = N)(s3),
      to(s4, addDone = Y)(s5),
      to(s4, addDone = N)(s4),
      to(s5, addDone = Y, addSign = N)(s6),
      to(s5, addDone = Y, addSign = Y)(s7),
      to(s5, addDone = N)(s5),
      to(s6, addDone = Y)(s7),
      to(s6, addDone = N)(s6),
    ).mkString("\n"))
  }
  )
  val debouceMul = Mux(mulDone, mul.z, 0.U)
  val debouceAdd = Mux(addDone, add.z, 0.U)


  // Data Path
  when(isMul)(q := debouceMul)

  r := Mux1H(Map(
    // x * y -> z; x * y -> r
    // state 1
    state.asUInt()(1) -> debouceMul,
    // z - q3 * p; r - p
    // state 4, 5, 6
    isAdd -> Mux(addDone, add.z, 0.U)
  ))

  add.valid := isAdd
  add.a := r
  // TODO: add another state here.
  add.b := Mux(state.asUInt()(4), -q, (-p+1).U)

  mul.valid := isMul
  mul.a := Mux1H(Map(
    state.asUInt()(1) -> input.bits.a,
    state.asUInt()(2) -> (q >> (width - 1)),
    state.asUInt()(3) -> (q >> (width + 1))
  ))
  mul.b := Mux1H(Map(
    state.asUInt()(1) -> input.bits.b,
    state.asUInt()(2) -> m.U,
    state.asUInt()(3) -> p.U
  ))

  input.ready := state.asUInt()(0)
  z.valid := state.asUInt()(7)
}


// before Wallace Mul implemented, we use DummyMul as mul.
class DummyMul(width: Int, pipe: Int) extends Module {
  val valid = IO(Input(Bool()))
  val a = IO(Input(UInt(width.W)))
  val b = IO(Input(UInt(width.W)))
  val z = IO(Output(UInt((2 * width).W)))
  val rs = Seq.fill(pipe + 1) {Wire(chiselTypeOf(z))}
  rs.zipWithIndex.foreach{case (r, i) =>
    if (i == 0) r := a * b else r := RegNext(rs(i - 1))
  }
  z := rs.last
}

class DummyAdd(width: Int, pipe: Int) extends Module {
  val valid = IO(Input(Bool()))
  val a = IO(Input(UInt(width.W)))
  val b = IO(Input(UInt(width.W)))
  val z = IO(Output(UInt(width.W)))
  val rs = Seq.fill(pipe + 1) {Wire(chiselTypeOf(z))}
  rs.zipWithIndex.foreach{case (r, i) =>
    if (i == 0) r := a + b else r := RegNext(rs(i - 1))
  }
  z := rs.last
}