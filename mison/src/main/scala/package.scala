package object mison {
  /**
    * See Mison 2.2
    * Useful bit manipulations
    */
  @inline def R(x: Long): Long = x & (x - 1)
  @inline def E(x: Long): Long = x & -x
  @inline def S(x: Long): Long = x ^ (x - 1)
  @inline def popcnt(x: Long): Int = java.lang.Long.bitCount(x)
  @inline def leadingOnes(x: Long): Int = java.lang.Long.numberOfLeadingZeros(~x)
  @inline def leadingZeros(x: Long): Int = java.lang.Long.numberOfLeadingZeros(x)
  @inline def trailingZeros(x: Long): Int = java.lang.Long.numberOfTrailingZeros(x)
}