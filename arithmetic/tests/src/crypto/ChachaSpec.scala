package crypto.chacha

import chisel3._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chisel3.tester.{ChiselUtestTester, testableClock, testableData}
import org.bouncycastle.crypto.engines.ChaChaEngine
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}
import utest._;

//import java.security.SecureRandom;;

class ChachaTest {
  def print_state(state: Array[Int], s: String)= {
    print(s)
    var i = 0
    for(i <- 0 to state.length-1) {
      if(i % 4 == 0) {
        print("\n")
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
    val loop = 20 /2
    for(j <- 0 to loop-1) {
      state = quarterround(state, 0, 4,  8, 12);
      state = quarterround(state, 1, 5,  9, 13);
      state = quarterround(state, 2, 6, 10, 14);
      state = quarterround(state, 3, 7, 11, 15);
      
      state = quarterround(state, 0, 5, 10, 15);
      state = quarterround(state, 1, 6, 11, 12);
      state = quarterround(state, 2, 7,  8, 13);
      state = quarterround(state, 3, 4,  9, 14);
    }
    for(i <- 0 to stateInput.length-1) {
      state(i) = state(i) + stateInput(i)
    }
    return state
  }

  def rotl32(x:Int, n:Int):Int= {
    return  x << n | (x >>> (-n & 31));
  }
  
  def quarterround(input: Array[Int], a:Int, b:Int, c:Int, d:Int):Array[Int] = {
    var x = new Array[Int](input.length)
    var i = 0
    for(i <- 0 to input.length-1) {
      x(i) = input(i)
    }
    x(a) += x(b)
    x(d) = rotl32(x(d) ^ x(a), 16)

    x(c) += x(d)
    x(b) = rotl32(x(b) ^ x(c), 12)

    x(a) += x(b)
    x(d) = rotl32(x(d) ^ x(a),  8)

    x(c) += x(d)
    x(b) = rotl32(x(b) ^ x(c),  7)
    return x
  }

  def doChaCha(state: Array[Int]):Array[Int] = {
    val round = 20               
    val result = new Array[Int](state.length);
    ChaChaEngine.chachaCore(round, state, result);
    return result
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

      val java_res = chachaTest.doChaCha(state)      

      var chachaParam = new ChaChaParameter(32 * nonce_len)
      testCircuit(new ChaCha(chachaParam), Seq(chiseltest.internal.NoThreadingAnnotation, chiseltest.simulator.WriteVcdAnnotation)){dut: ChaCha =>
        dut.clock.setTimeout(0)
        val tmp_k1 = Vec(4, UInt(32.W)).Lit(
          0 -> BigInt(key(0).toHexString, 16).U,
          1 -> BigInt(key(1).toHexString, 16).U, 
          2 -> BigInt(key(2).toHexString, 16).U, 
          3 -> BigInt(key(3).toHexString, 16).U
        )
        val tmp_k2 = Vec(4, UInt(32.W)).Lit(
          0 -> BigInt(key(4).toHexString, 16).U, 
          1 -> BigInt(key(5).toHexString, 16).U, 
          2 -> BigInt(key(6).toHexString, 16).U, 
          3 -> BigInt(key(7).toHexString, 16).U
        )
        dut.key.poke(Vec(2, Vec(4, UInt(32.W))).Lit(0->tmp_k1, 1->tmp_k2))

        var tmp_nonce = nonce(0).toHexString
        var tmp_n = nonce(1).toHexString
        var tmp_n_len = 8 - tmp_n.length() // make sure the length is 8 (32 / 4 = 8)
        if(tmp_n_len > 0) {
          for(i <- 0 to  tmp_n_len-1) {
            tmp_n = "0" + tmp_n
          }
        }
        tmp_nonce = tmp_nonce + tmp_n
        dut.nonce.poke(BigInt(tmp_nonce, 16).U(64.W))

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
                 utest.assert((dut.output.bits.x(i)(j).peek().litValue.toInt + state(i*4+j)) == res(i*4+j));
              }
            }
          }
        }
        utest.assert(flag)
      }
    }
  }
}
