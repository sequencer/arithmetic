package queue

import chisel3._
import chisel3.util._

/** Lower index of `in` and `out` has higher priority. */
class MultiQueue[T <: Data](val gen: T,
                            val entriesSize: Int,
                            val enqueueSize: Int,
                            val dequeueSize: Int
                           ) extends Module {
  /** input io. */
  val in: Vec[DecoupledIO[T]] = IO(Flipped(VecInit(Seq.fill(enqueueSize)(Decoupled(gen)))))
  /** output io. */
  val out: Vec[DecoupledIO[T]] = IO(VecInit(Seq.fill(dequeueSize)(Decoupled(gen))))
  /** count of this queue. */
  val count: UInt = IO(Output(UInt(log2Ceil(entriesSize).W)))

  val queue: Vec[T] = Reg(VecInit(Seq.fill(entriesSize)(gen)))

  // TODO: maybe gray code might be better?
  val headPointer: UInt = RegInit(0.U(log2Ceil(entriesSize).W))
  val tailPointer: UInt = RegInit(0.U(log2Ceil(entriesSize).W))

  /** For the case that `headPointer == tailPointer`,
    * there are two cases:
    * 1. queue is empty
    * 2. queue is full
    *
    * if enqueue size is larger than dequeue size.(How many enqueue/dequeue fire.)
    * In the next state, if `headPointer == tailPointer`, queue is full.
    *
    * So a `maybeFull` signal is introduced here to generate full signal in the next state.
    */
  val maybeFull: Bool = Wire(Wire(Bool()))
  // adder here.
  val headSubTail: SInt = headPointer.zext() - tailPointer.zext()
  // only check sign bit.
  val headGETail: Bool = headSubTail >= 0.S
  // xor all bits.
  val headEQTail: Bool = headSubTail === 0.S
  val headGTTail: Bool = headGETail && (!headEQTail)
  val headLTTail: Bool = !headGETail

  val full: Bool = headEQTail && maybeFull
  val empty: Bool = headEQTail && !maybeFull

  /** size of slots that is valid, drop the sign bit. */
  count := Mux1H(
    Map(
      headGTTail -> headSubTail,
      headLTTail -> (headSubTail + entriesSize.S),
      full -> entriesSize.S,
      empty -> 0.S,
    )
  ).asUInt()

  // enqueue logic
  queue := in
    // gather all shift size, and propagate `shiftCount` to next entry.
    .scanLeft(
      // base maskLocation
      headPointer,
      // base valid count(need extend width in case of overflow) as SInt
      (enqueueSize.U - count).asTypeOf(UInt(math.max(entriesSize, enqueueSize).W)).zext(),
      // a placeholder of data, which will be drop at last.
      VecInit(Seq.fill(entriesSize)(gen)),
    ) {
      case ((previousMaskLocation, previousRemainValid, _), i) =>
        val maskLocation: UInt = previousMaskLocation + i.valid.asUInt()
        val remainReady: SInt = previousRemainValid - i.valid.asSInt()
        // this ready couple to previous valid.
        i.ready := previousRemainValid > 0.S
        (maskLocation, remainReady,
          VecInit(Seq
            .fill(entriesSize)(i.bits)
            .zip(UIntToOH(maskLocation, entriesSize).asBools())
            .map { case (data, mask) => VecInit(data.asUInt().asBools().map(_ & mask)).asTypeOf(gen) })
        )
    }
    // drop placeholder
    .drop(1)
    // get all masked result
    .map(_._3)
    // OR all masked input together.
    .reduce { case (a, b) => (a.asUInt() | b.asUInt()).asTypeOf(Vec(enqueueSize, gen)) }

  // dequeue logic.
  out := VecInit(out
    .scanLeft(
      // base maskLocation
      tailPointer,
      // base valid count(need extend width in case of overflow) as SInt
      count.zext(),
      // a placeholder of data, which will be drop at last.
      gen
    ) { case ((previousMaskLocation, previousRemainReady, _), o) =>
      val maskLocation = previousMaskLocation + o.ready.asUInt
      val remainReady = previousRemainReady - o.ready.asSInt
      // this valid couple to previous ready.
      o.valid := remainReady > 0.S
      (maskLocation, remainReady,
        queue
          .zipWithIndex
          .map { case (data, index) =>
            val mask: Bool = index.U === maskLocation
            VecInit(data.asUInt().asBools().map(_ & mask)).asTypeOf(gen)
          }
          .reduce { case (a, b) => (a.asUInt() | b.asUInt()).asTypeOf(gen) }
      )
    }
    // drop placeholder
    .drop(1)
    // get all result
    .map(_._3)
  )
}
