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
      state = quarterroun(state, 0, 4,  8, 12);
      state = quarterroun(state, 1, 5,  9, 13);
      state = quarterroun(state, 2, 6, 10, 14);
      state = quarterroun(state, 3, 7, 11, 15);
      
      state = quarterroun(state, 0, 5, 10, 15);
      state = quarterroun(state, 1, 6, 11, 12);
      state = quarterroun(state, 2, 7,  8, 13);
      state = quarterroun(state, 3, 4,  9, 14);
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

    x(c) += x(d)
    x(b) = rotl32(x(b) ^ x(c), 12)

    x(a) += x(b)
    x(d) = rotl32(x(d) ^ x(a),  8)

    x(c) += x(d)
    x(b) = rotl32(x(b) ^ x(c),  7)
    return x
  }
  def doChaCha(encrypt:Boolean, data: Array[Byte], key: Array[Byte],  nonce: Array[Byte]):(Array[Byte]) = {
    val cipher:ChaChaEngine = new ChaChaEngine(20);
    val paramKey: KeyParameter = new KeyParameter(key);
    val param: ParametersWithIV = new ParametersWithIV(paramKey, nonce);
    cipher.init(encrypt, param); // true - encrypt, false - decrypt
    val result = new Array[Byte](data.length);
    cipher.processBytes(data, 0, data.length, result, 0);
    return result
  }

  def bytes2int(_bytes: Array[Byte], _offset: Int): Int = {
    var b0 = _bytes(_offset + 0) & 0xff
    var b1 = _bytes(_offset + 1) & 0xff
    var b2 = _bytes(_offset + 2) & 0xff
    var b3 = _bytes(_offset + 3) & 0xff
    // return ((b3 << 24) | (b2 << 16) | (b1 << 8) | b0)
    return ((b0 << 24) | (b1 << 16) | (b2 << 8) | b3)
  }
}

object ChachaSpec extends TestSuite with ChiselUtestTester {
  def tests: Tests = Tests {
    test("Chacha should pass") {
      
//        byte[] data = Hex.decode("10000000000000000000000000000000");
//        byte[] key = Hex.decode("80000000000000000000000000000000");
//        byte[] nonce = Hex.decode("80000000000000000000000000000000");
//        byte[] result = new ChaChaTest().doChaCha(true, data, key, nonce);
//        System.out.println(result);

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
      chachaTest.print_state(res, "my test")

      val byte_key: Array[Byte] = new Array[Byte](4 * key.length)
      for(i <- 0 to key.length-1) {
        val tmp = BigInt(key(i)).toByteArray
        for(j <- 0 to tmp.length-1) {
          byte_key(i * 4 + j ) = tmp(j)
        }
      }
      val byte_nonce: Array[Byte] = new Array[Byte](4 * nonce.length)
      for(i <- 0 to nonce.length-1) {
        val tmp = BigInt(nonce(i)).toByteArray
        for(j <- 0 to tmp.length-1) {
          byte_nonce(i * 4 + j ) = tmp(j)
        }
      }
      val byte_data: Array[Byte] = new Array[Byte](32 * 2)
      for(i <- 0 to byte_data.length-1) {
        byte_data(i) = 0x00.toByte
      }

      val java_res = chachaTest.doChaCha(true, byte_data, byte_key, byte_nonce)
      for(i <- 0 to key.length*2-1) {
        println((chachaTest.bytes2int(java_res, 4 *i)).toHexString)
      }

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
