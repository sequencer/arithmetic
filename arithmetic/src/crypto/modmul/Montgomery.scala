package crypto.modmul

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.{Counter, Mux1H}

class Montgomery(pWidth: Int = 4096, addPipe: Int) extends Module {
  val p = IO(Input(UInt(pWidth.W)))
  val pPrime = IO(Input(Bool()))
  val a = IO(Input(UInt(pWidth.W)))
  val b = IO(Input(UInt(pWidth.W)))
  val b_add_p = IO(Input(UInt((pWidth + 1).W))) // b + p
  val valid = IO(Input(Bool())) // input valid
  val out = IO(Output(UInt(pWidth.W)))
  val out_valid = IO(Output(Bool())) // output valid

  val u = Reg(Bool())
  val i = Reg(UInt(pWidth.W))
  val nextT = Reg(UInt((pWidth + 2).W))

  // multicycle prefixadder
  val adder = Module(new DummyAdd(pWidth + 2, addPipe))
  val add_stable = RegInit(0.U((pWidth + 2).W))
  // Control Path
  object StateType extends ChiselEnum {
    val s0 = Value("b00001".U) // t = 0
    val s1 = Value("b00010".U) // loop
    val s2 = Value("b00100".U) // loop done
    val s3 = Value("b01000".U) // if-then
    val s4 = Value("b10000".U) // done
  }

  val state = RegInit(StateType.s0)
  val isAdd = (state.asUInt & "b1010".U).orR
  adder.valid := isAdd
  val addDoneNext = RegInit(false.B)
  addDoneNext := addDone
  lazy val addDone = if (addPipe != 0) Counter(isAdd && (~addDoneNext), addPipe + 1)._2 else true.B
  val addSign = (nextT < p.asUInt)
  state := chisel3.util.experimental.decode
    .decoder(
      state.asUInt() ## addDoneNext ## valid ## i.head(1) ## addSign, {
        val Y = "1"
        val N = "0"
        val DC = "?"
        def to(
          stateI:  String,
          addDone: String = DC,
          valid:   String = DC,
          iHead:   String = DC,
          addSign: String = DC
        )(stateO:  String
        ) = s"$stateI$addDone$valid$iHead$addSign->$stateO"
        val s0 = "00001"
        val s1 = "00010"
        val s2 = "00100"
        val s3 = "01000"
        val s4 = "01000"
        TruthTable.fromString(
          Seq(
            to(s0, valid = N)(s0),
            to(s0, valid = Y)(s1),
            to(s1, addDone = Y)(s2),
            to(s1, addDone = N)(s1),
            to(s2, iHead = Y, addSign = N)(s3),
            to(s2, iHead = Y, addSign = Y)(s4),
            to(s2, iHead = N)(s1),
            to(s3, addDone = Y)(s4),
            to(s3, addDone = N)(s3),
            "???????"
          ).mkString("\n")
        )
      }
    )
    .asTypeOf(StateType.Type())

  i := Mux1H(
    Map(
      state.asUInt()(0) -> 1.U,
      state.asUInt()(2) -> i.rotateLeft(1),
      (state.asUInt() & "b11010".U).orR() -> i
    )
  )

  adder.a := nextT
  adder.b := Mux1H(
    Map(
      (state.asUInt()(1) && ((a & i).orR) && u.asBool) -> b_add_p,
      (state.asUInt()(1) && ((a & i).orR) && (~u.asBool)) -> b,
      (state.asUInt()(1) && (~((a & i).orR)) && u.asBool) -> p,
      // last select
      state.asUInt()(3) -> -p
    )
  )

  val debounceAdd = Mux(addDone, adder.z, 0.U)
  when(addDone)(add_stable := debounceAdd)
  nextT := Mux1H(
    Map(
      (state.asUInt()(1) || (state.asUInt()(3) && (add_stable.head(1).asBool))) -> (add_stable >> 1),
      (((state.asUInt & "b10101".U).orR) || (state.asUInt()(3) && ~(add_stable.head(1).asBool))) -> nextT
    )
  )

  u := Mux1H(
    Map(
      (state.asUInt & "b00001".U).orR -> (a(0).asUInt & b(0).asUInt & pPrime.asUInt),
      (state.asUInt & "b00100".U).orR -> ((nextT(0) + ((a & (i.rotateLeft(1))).orR) & b(0)) & pPrime.asUInt),
      (state.asUInt & "b11010".U).orR -> u
    )
  )

  out := nextT
  out_valid := state.asUInt()(4)
}
