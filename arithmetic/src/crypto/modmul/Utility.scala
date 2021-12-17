package crypto.modmul

class Utility {
  // Check if given p is a prime
  def isPrime(p:Int): Boolean = {
    if(p <= 2) {
      return false
    }
    for (j <- 2 until p){
      if ((p%j)==0) return false
    }
    return true
  }
  // Generate random prime with max value 2 ^ length
  def randPrime(length:Int): Int = {
    var max = (scala.math.pow(2, length)).toInt
    var p = scala.util.Random.nextInt(max) + 1
    while(!isPrime(p)) {
      p = scala.util.Random.nextInt(max) + 1
    }
    return p
  }
  // Extended Euclidean algorithm
  def egcd(a: Int, b:Int): (Int, Int, Int) = {
    if (a == 0) {
      return (b, 0, 1)
    }
    var (g, y, x) = egcd(b % a, a)
    return (g, x - ((b /a) * y), y)
  }
  // modulus inversion
  def modinv(R:Int, p:Int): Int = {
    var (g, x, y) = egcd(R, p)
    if (g != 1) { // does not exist
      return 0
    }
    if(x < 0) {
      return p+x
    }
    return (x % p)
  }
}
