
/*============================================================================

This Chisel source file is part of a pre-release version of the HardFloat IEEE
Floating-Point Arithmetic Package, by John R. Hauser (with some contributions
from Yunsup Lee and Andrew Waterman, mainly concerning testing).

Copyright 2017 SiFive, Inc.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
    this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions, and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of SiFive nor the names of its contributors may
    be used to endorse or promote products derived from this software without
    specific prior written permission.

THIS SOFTWARE IS PROVIDED BY SIFIVE AND CONTRIBUTORS "AS IS", AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE
DISCLAIMED.  IN NO EVENT SHALL SIFIVE OR CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

=============================================================================*/
package tests
import chisel3._
import chisel3.util._
import float._

class DivRecFN_io(expWidth: Int, sigWidth: Int) extends Bundle {
    val a = Bits((expWidth + sigWidth).W)
    val b = Bits((expWidth + sigWidth).W)
    val roundingMode   = UInt(3.W)
    val detectTininess = UInt(1.W)
    val out = Bits((expWidth + sigWidth).W)
    val exceptionFlags = Bits(5.W)

}

class
    ValExec_DivSqrtRecFN_small_div(expWidth: Int, sigWidth: Int, options: Int) extends Module
{
    val io = IO(new Bundle {
        val input = Flipped(Decoupled(new DivRecFN_io(expWidth, sigWidth)))

        val output = new Bundle {
            val a = Flipped(Input(Bits((expWidth + sigWidth).W)))
            val b = Flipped(Input(Bits((expWidth + sigWidth).W)))
            val roundingMode   = Output(UInt(3.W))
            val detectTininess = Output(UInt(1.W))
        }

        val expected = new Bundle {
            val out = Output(Bits((expWidth + sigWidth).W))
            val exceptionFlags = Output(Bits(5.W))
            val recOut = Output(Bits((expWidth + sigWidth + 1).W))
        }

        val actual = new Bundle {
            val out = Output(Bits((expWidth + sigWidth + 1).W))
            val exceptionFlags = Output(Bits(5.W))
        }

        val check = Output(Bool())
        val pass = Output(Bool())
    })

    val ds = Module(new DivSqrt(8,24))
    val cq = Module(new Queue(new DivRecFN_io(expWidth, sigWidth), 5))

    cq.io.enq.valid := io.input.valid && ds.input.ready
    cq.io.enq.bits := io.input.bits

    io.input.ready := ds.input.ready && cq.io.enq.ready
    ds.input.valid := io.input.valid && cq.io.enq.ready
    ds.input.bits.sqrt := false.B
    ds.input.bits.a := io.input.bits.a
    ds.input.bits.b := io.input.bits.b
    ds.input.bits.roundingMode   := io.input.bits.roundingMode
    //ds.input.bits.detectTininess := io.input.bits.detectTininess

    io.output.a := cq.io.deq.bits.a
    io.output.b := cq.io.deq.bits.b
    io.output.roundingMode   := cq.io.deq.bits.roundingMode
    io.output.detectTininess := cq.io.deq.bits.detectTininess

    io.expected.out := cq.io.deq.bits.out
    io.expected.exceptionFlags := cq.io.deq.bits.exceptionFlags
    io.expected.recOut := cq.io.deq.bits.out

    io.actual.out := ds.output.bits.result
    io.actual.exceptionFlags := ds.output.bits.exceptionFlags

    cq.io.deq.ready := ds.output.valid

    io.check := ds.output.valid
    io.pass :=
        cq.io.deq.valid &&
          (io.actual.out===io.expected.recOut) &&
        (io.actual.exceptionFlags === io.expected.exceptionFlags)
}

class SqrtRecFN_io(expWidth: Int, sigWidth: Int) extends Bundle {
    val a = Bits((expWidth + sigWidth).W)
    val roundingMode   = UInt(3.W)
    val detectTininess = UInt(1.W)
    val out = Bits((expWidth + sigWidth).W)
    val exceptionFlags = Bits(5.W)

}

class
    ValExec_DivSqrtRecFN_small_sqrt(expWidth: Int, sigWidth: Int, options: Int)
    extends Module
{
    val io = IO(new Bundle {
        val input = Flipped(Decoupled(new SqrtRecFN_io(expWidth, sigWidth)))

        val output = new Bundle {
            val a = Output(Bits((expWidth + sigWidth).W))
            val roundingMode   = Output(UInt(3.W))
            val detectTininess = Output(UInt(1.W))
        }

        val expected = new Bundle {
            val out = Output(Bits((expWidth + sigWidth).W))
            val exceptionFlags = Output(Bits(5.W))
            val recOut = Output(Bits((expWidth + sigWidth + 1).W))
        }

        val actual = new Bundle {
            val out = Output(Bits((expWidth + sigWidth + 1).W))
            val exceptionFlags = Output(Bits(5.W))
        }

        val check = Output(Bool())
        val pass = Output(Bool())
    })

    val ds = Module(new DivSqrt(8,24))
    val cq = Module(new Queue(new SqrtRecFN_io(expWidth, sigWidth), 5))

    cq.io.enq.valid := io.input.valid && ds.input.ready
    cq.io.enq.bits := io.input.bits

    io.input.ready := ds.input.ready && cq.io.enq.ready
    ds.input.valid := io.input.valid && cq.io.enq.ready
    ds.input.bits.sqrt := true.B
    ds.input.bits.a := io.input.bits.a
    ds.input.bits.b := DontCare
    ds.input.bits.roundingMode   := io.input.bits.roundingMode
//    ds.input.bits.detectTininess := io.input.bits.detectTininess

    io.output.a := cq.io.deq.bits.a
    io.output.roundingMode   := cq.io.deq.bits.roundingMode
    io.output.detectTininess := cq.io.deq.bits.detectTininess

    io.expected.out := cq.io.deq.bits.out
    io.expected.exceptionFlags := cq.io.deq.bits.exceptionFlags
    io.expected.recOut := cq.io.deq.bits.out

    io.actual.exceptionFlags := ds.output.bits.exceptionFlags
    io.actual.out := ds.output.bits.result

    cq.io.deq.ready := ds.output.valid

    io.check := ds.output.valid
    io.pass :=
        cq.io.deq.valid &&
          (io.actual.out === io.expected.recOut) &&
        (io.actual.exceptionFlags === io.expected.exceptionFlags)
}
