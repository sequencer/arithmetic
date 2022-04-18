package crypto.chacha

import chisel3._
import chisel3.experimental.VecLiteralException
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import utest._

class ChachaTest {
  def print_state(state: Array[Int], s: String)= {
    print(s)
    var i = 0
    for(i <- 0 to state.length-1) {
      if(i % 4 == 0) {
        println()
      }
      print("0x")
      print(state(i).toHexString)
      print(",\t")
    }
    println()
  }

  def randomVar(nonce_len: Int):(Array[Int],Array[Int]) = {
    var key = new Array[Int](8)    
    var nonce = new Array[Int](nonce_len)
    var max = (scala.math.pow(2, 32)).toInt
    var i = 0
    for(i <- 0 to key.length-1) {
      key(i) = scala.util.Random.nextInt(max)
    }
    for(i <- 0 to nonce.length-1) {
      nonce(i) = scala.util.Random.nextInt(max)
    }
    return (key, nonce)
  }

  def stateInit(key: Array[Int], counter: Array[Int], nonce: Array[Int]): Array[Int] = {
    var state = new Array[Int](16)
    state(0) = 0x61707865
    state(1) = 0x3320646e
    state(2) = 0x79622d32
    state(3) = 0x6b206574
    var i = 0
    for(i <- 0 to key.length-1) {
      state(4+i) = key(i)
    }
    for(i <- 0 to counter.length-1) {
      state(key.length + 4+i) = counter(i)
    }
    for(i <- 0 to nonce.length-1) {
      state(counter.length+key.length + 4+i) = nonce(i)
    }
    return state
  }

  def chacha_block(stateInput: Array[Int]): Array[Int] = {
    var state = new Array[Int](stateInput.length)
    var i = 0
    var j = 0
    for(i <- 0 to stateInput.length-1) {
      state(i) = stateInput(i)
    }
    for(j <- 0 to 9) {
      // println(2*j)
      state = quarterroun(state, 0, 4,  8, 12);
      // print_state(state, "1")
      state = quarterroun(state, 1, 5,  9, 13);
      // print_state(state, "2")
      state = quarterroun(state, 2, 6, 10, 14);
      // print_state(state, "3")
      state = quarterroun(state, 3, 7, 11, 15);
      // print_state(state, "odd")
      
      // println(2*j + 1)
      state = quarterroun(state, 0, 5, 10, 15);
      state = quarterroun(state, 1, 6, 11, 12);
      state = quarterroun(state, 2, 7,  8, 13);
      state = quarterroun(state, 3, 4,  9, 14);
      // print_state(state, "even")
      
    }
    return state
  }

  def rotl32(x:Int, n:Int):Int= {
    return  x << n | (x >>> (-n & 31));
  }
  
  def quarterroun(input: Array[Int], a:Int, b:Int, c:Int, d:Int):Array[Int] = {
    var x = new Array[Int](input.length)
    var i = 0
    for(i <- 0 to input.length-1) {
      x(i) = input(i)
    }
    x(a) += x(b)
    x(d) = rotl32(x(d) ^ x(a), 16)
    // print_state(x, "1")

    x(c) += x(d)
    x(b) = rotl32(x(b) ^ x(c), 12)
    // print_state(x, "2")

    x(a) += x(b)
    x(d) = rotl32(x(d) ^ x(a),  8)
    // print_state(x, "3")

    x(c) += x(d)
    x(b) = rotl32(x(b) ^ x(c),  7)
    // print_state(x, "4")
    return x
  }
}

object ChachaSpec extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Chacha should pass") {
      var chachaTest = new ChachaTest()
      var nonce_len = 2
      var (key, nonce) = chachaTest.randomVar(nonce_len)
      // set count to 0
      var counter = new Array[Int](4-nonce_len)  
      for(i <- 0 to counter.length-1) {
        counter(i) = 0
      }
      var state = chachaTest.stateInit(key, counter, nonce)
      var res = chachaTest.chacha_block((state))

      // chachaTest.print_state(key, "\nkey")
      // chachaTest.print_state(counter, "\ncounter")
      // chachaTest.print_state(nonce, "\nnonce")
      // chachaTest.print_state(state, "\nstate")
      // chachaTest.print_state(res, "\nfinal res")

      var chachaParam = new ChaChaParameter(32 * nonce_len)
      // testCircuit(new ChaCha(chachaParam), Seq(chiseltest.simulator.VcsBackendAnnotation, chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteFsdbAnnotation)){dut: ChaCha =>
      testCircuit(new ChaCha(chachaParam), Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)){dut: ChaCha =>
        dut.clock.setTimeout(0)
        val tmp_k1 = Vec(4, UInt(32.W)).Lit(0 -> key(0).U, 1 -> key(1).U, 2 -> key(2).U, 3 -> key(3).U)
        val tmp_k2 = Vec(4, UInt(32.W)).Lit(0 -> key(4).U, 1 -> key(5).U, 2 -> key(6).U, 3 -> key(7).U)
        dut.key.poke(Vec(2, Vec(4, UInt(32.W))).Lit(0->tmp_k1, 1->tmp_k2))

        var tmp_n1 = nonce(0).toLong
        tmp_n1 = tmp_n1 << 32
        tmp_n1 = tmp_n1 + nonce(1)
        dut.nonce.poke(tmp_n1.U)

        dut.clock.step()
        dut.clock.step()
        // delay two cycles then set valid = true
        dut.valid.poke(true.B)
        dut.clock.step()
        var flag = false
        var a = 1
        for(a <- 1 to 200) {
          dut.clock.step()
          if(dut.output.valid.peek().litValue == 1) {
            flag = true
            // wait one cycle
            dut.clock.step()
            for(i <- 0 to 3) {
              for(j <- 0 to 3) {
                 utest.assert(dut.output.bits.x(i)(j).peek().litValue.toInt == res(i*4+j));
              }
            }
          }
        }
        utest.assert(flag)
      }
    }
  }
}
