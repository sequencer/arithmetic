package crypto.modmul

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.{Cat, Counter, Mux1H}

class Montgomery(pWidth: Int = 4096, addPipe: Int) extends Module {
  val p = IO(Input(UInt(pWidth.W)))
  val pPrime = IO(Input(Bool()))
  val a = IO(Input(UInt(pWidth.W)))
  val b = IO(Input(UInt(pWidth.W)))
  val indexCountBit = 16
  val input_width = IO(Input(UInt(indexCountBit.W)))
  val valid = IO(Input(Bool())) // input valid
  val out = IO(Output(UInt(pWidth.W)))
  val out_valid = IO(Output(Bool())) // output valid

  val b_add_p = Reg(UInt((pWidth + 1).W))
  val invP = Reg(UInt((pWidth).W))
  val negP = Reg(UInt((pWidth + 2).W))
  val loop_u = Reg(Bool())
  val index = Reg(UInt(indexCountBit.W))
  val nextT = Reg(UInt((pWidth + 2).W))

  // multicycle prefixadder
  val adder = Module(new DummyAdd(pWidth + 2, addPipe))
  val add_stable = RegInit(0.U((pWidth + 2).W))
  // Control Path
  object StateType extends ChiselEnum {
    val s0 = Value("b0000001".U) // nextT = 0, loop_u = a(0)b(0)pPrime, b_add_p = b + p
    // loop
    val s1 = Value("b0000010".U) // nextT + b
    val s2 = Value("b0000100".U) // nextT + p
    val s3 = Value("b0001000".U) // nextT + b_add_p
    // loop done
    val s4 = Value("b0010000".U) // index += 1, loop_u = (nextT(0) + a(index)b(0))pPrime, nextT / 2
    val s5 = Value("b0100000".U) // nextT - p
    val s6 = Value("b1000000".U) // done
    val s7 = Value("b10000000".U) // nextT + 0
    val s8 = Value("b100000000".U) // calculate ~p
    val s9 = Value("b1000000000".U) // calculate ~p + 1
  }

  val state = RegInit(StateType.s0)
  val isAdd = (state.asUInt & "b1010101111".U).orR
  adder.valid := isAdd
  val addDoneNext = RegInit(false.B)
  addDoneNext := addDone
  lazy val addDone = if (addPipe != 0) Counter(valid && isAdd && (~addDoneNext), addPipe + 1)._2 else true.B
  val a_i = Reg(Bool())
  val iBreak = (index.asUInt >= input_width.asUInt)
  state := chisel3.util.experimental.decode
    .decoder(
      state.asUInt() ## addDoneNext ## valid ## iBreak ## loop_u ## a_i, {
        val Y = "1"
        val N = "0"
        val DC = "?"
        def to(
          stateI:  String,
          addDone: String = DC,
          valid:   String = DC,
          iBreak:  String = DC,
          loop_u:  String = DC,
          a_i:     String = DC
        )(stateO:  String
        ) = s"$stateI$addDone$valid$iBreak$loop_u$a_i->$stateO"
        val s0 = "0000000001"
        val s1 = "0000000010"
        val s2 = "0000000100"
        val s3 = "0000001000"
        val s4 = "0000010000"
        val s5 = "0000100000"
        val s6 = "0001000000"
        val s7 = "0010000000"
        val s8 = "0100000000"
        val s9 = "1000000000"
        TruthTable.fromString(
          Seq(
            to(s0, valid = N)(s0),
            to(s0, valid = Y, addDone = N)(s0),
            to(s0, valid = Y, addDone = Y)(s8),
            to(s8)(s9),
            to(s9, addDone = N)(s9),
            to(s9, addDone = Y, a_i = Y, loop_u = N)(s1),
            to(s9, addDone = Y, a_i = N, loop_u = Y)(s2),
            to(s9, addDone = Y, a_i = Y, loop_u = Y)(s3),
            to(s9, addDone = Y, a_i = N, loop_u = N)(s7),
            to(s1, addDone = Y)(s4),
            to(s1, addDone = N)(s1),
            to(s2, addDone = Y)(s4),
            to(s2, addDone = N)(s2),
            to(s3, addDone = Y)(s4),
            to(s3, addDone = N)(s3),
            to(s7, addDone = Y)(s4),
            to(s7, addDone = N)(s7),
            to(s4, iBreak = Y)(s5),
            to(s4, iBreak = N, a_i = Y, loop_u = N)(s1),
            to(s4, iBreak = N, a_i = N, loop_u = Y)(s2),
            to(s4, iBreak = N, a_i = Y, loop_u = Y)(s3),
            to(s4, iBreak = N, a_i = N, loop_u = N)(s7),
            to(s5, addDone = Y)(s6),
            to(s5, addDone = N)(s5),
            to(s6, valid = N)(s0),
            to(s6, valid = Y)(s6),
            "??????????"
          ).mkString("\n")
        )
      }
    )
    .asTypeOf(StateType.Type())

  index := Mux1H(
    Map(
      state.asUInt()(0) -> 0.U,
      state.asUInt()(4) -> (index + 1.U),
      (state.asUInt & "b1111101110".U).orR -> index
    )
  )

  b_add_p := Mux(addDone & state.asUInt()(0), debounceAdd, b_add_p)

  loop_u := Mux1H(
    Map(
      state.asUInt()(0) -> (a(0).asUInt & b(0).asUInt & pPrime.asUInt),
      (state.asUInt & "b0010001110".U).orR -> ((add_stable(1) + (a(index + 1.U) & b(0))) & pPrime.asUInt),
      (state.asUInt & "b1101110000".U).orR -> loop_u
    )
  )

  a_i := Mux1H(
    Map(
      state.asUInt()(0) -> a(0),
      (state.asUInt & "b0010001110".U).orR -> a(index + 1.U),
      (state.asUInt & "b1101110000".U).orR -> a_i
    )
  )

  nextT := Mux1H(
    Map(
      state.asUInt()(0) -> 0.U,
      state.asUInt()(4) -> (add_stable >> 1),
      state.asUInt()(5) -> add_stable,
      (state.asUInt & "b1111001110".U).orR -> nextT
    )
  )
  val TWithoutSubControl = Reg(UInt(1.W))
  val TWithoutSub = Reg(UInt((pWidth + 2).W))
  TWithoutSubControl := Mux(state.asUInt()(5), 0.U, 1.U)
  TWithoutSub := Mux(state.asUInt()(5) && (TWithoutSubControl === 1.U), nextT, TWithoutSub)
  invP := Mux(state.asUInt()(8), ~p, invP)
  negP := Mux(state.asUInt()(9), add_stable, negP)

  adder.a := Mux1H(
    Map(
      state.asUInt()(0) -> p,
      state.asUInt()(9) -> 1.U,
      (state.asUInt & "b0111111110".U).orR -> nextT
    )
  )
  adder.b := Mux1H(
    Map(
      (state.asUInt & "b0100000011".U).orR -> b,
      state.asUInt()(9) -> Cat(3.U, invP),
      state.asUInt()(2) -> p,
      state.asUInt()(3) -> b_add_p,
      state.asUInt()(7) -> 0.U,
      state.asUInt()(5) -> negP
    )
  )
  lazy val debounceAdd = Mux(addDone, adder.z, 0.U)
  add_stable := Mux(addDone, debounceAdd, add_stable)

  // output
  out := Mux(nextT.head(1).asBool, TWithoutSub, nextT)
  out_valid := state.asUInt()(6)
}
