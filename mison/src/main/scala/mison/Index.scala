package mison

import Macros._

object Index {

  type Bitmap = Array[Long]

  class StructuralCharacters(val bitmaps: Array[Bitmap]) extends AnyVal {
    def backslashs = bitmaps(0)
    def quotes = bitmaps(1)
    def colons = bitmaps(2)
    def leftBraces = bitmaps(3)
    def rightBraces = bitmaps(4)
  }

  def apply(json: Array[Byte], maxLevel: Int, avx2Thresold: Int = 150): Index = {
    val structuralCharacters = IndexBuilder.structuralCharacters(json, json.length > avx2Thresold)
    IndexBuilder.filterUnstructuralQuotes(structuralCharacters)
    IndexBuilder.filterUnstructuralCharacters(structuralCharacters)
    IndexBuilder.buildLeveledColonIndex(structuralCharacters, maxLevel)
  }
}

class Index(val bitmaps: Array[Index.Bitmap]) extends AnyVal {
  import Index._

  /**
    * See Mison 4.3
    * Generate the colon positions for a given level & range
    */
  def getColonPositions(start: Int, end: Int, level: Int): java.util.List[Int] = {
    val startw = start / 64
    val endw = (end + 63) / 64
    val colons = bitmaps(level)
    val positions = new java.util.ArrayList[Int](32)
    forloop(startw, _ < endw, _ + 1) { i =>
      var colon = colons(i)
      while(colon != 0) {
        val bit = E(colon)
        val offset = (i * 64) + popcnt(bit - 1)
        if(start <= offset && offset <= end) {
          positions.add(offset)
        }
        colon = R(colon)
      }
    }
    positions
  }

  def level(l: Int): Bitmap = bitmaps(l)
}

object IndexBuilder {
  System.load(new java.io.File(new java.io.File(System.getProperty("user.dir")), "native/target/release/libmison_avx.dylib").getAbsolutePath)

  import Index._

  /**
    * See Mison 4.2.1
    * Returns the bitmaps in this order: \,",:,{,}
    */
  def structuralCharacters(json: Array[Byte], useAvx2: Boolean): StructuralCharacters =
    if(useAvx2) {
      val len = (json.length + 63) / 64
      val bitmap0 = Array.ofDim[Long](len * 5)
      val bitmaps = Array.ofDim[Bitmap](5)
      @native def avx2Bitmaps(jsonBytes: Array[Byte], bitmap: Bitmap): Unit
      avx2Bitmaps(json, bitmap0)
      forloop(0, _ < 5, _ + 1) { i =>
        bitmaps(i) = Array.ofDim[Long](len)
        System.arraycopy(bitmap0, len * i, bitmaps(i), 0, len)
      }
      new StructuralCharacters(bitmaps)
    }
    else {
      val len = (json.length + 63) / 64
      def bitmap(maskCharacter: Char): Bitmap = {
        val maskByte = maskCharacter.toByte
        val bitmap = Array.ofDim[Long](len)
        val bound = Math.min(json.length, bitmap.length * 64)
        forloop(0, _ < len, _ + 1) { l =>
          forloop(((l + 1) * 64) - 1, _ >= l * 64, _ - 1) { i =>
            bitmap(l) = (bitmap(l) << 1) | ((if(i < bound) (if(json(i) == maskByte) 0x1 else 0x0) else 0x0))
          }
        }
        bitmap
      }
      new StructuralCharacters(Array(bitmap('\\'), bitmap('"'), bitmap(':'), bitmap('{'), bitmap('}')))
    }

  /**
    * See Mison 4.2.2
    * Build the structural quote bitmap
    */
  def filterUnstructuralQuotes(structuralCharacters: StructuralCharacters): Unit = {
    // First we identify the backslash immediatly followed by a quote character
    val backslashs = structuralCharacters.backslashs
    val quotes = structuralCharacters.quotes
    val len = backslashs.size
    val backslashQuotes = Array.ofDim[Long](len)
    forloop(0, _ < len - 1, _ + 1) { i =>
      backslashQuotes(i) = ((quotes(i) >> 1) | (quotes(i + 1) << 63)) & backslashs(i)
    }
    backslashQuotes(len - 1) = (quotes(len - 1) >> 1) & backslashs(len - 1)

    // Then for each identified backslah we count the consecutive backslash to figure out
    // if it escapes the following quote character
    val unstructuralQuoteMask = Array.ofDim[Long](len)
    forloop(0, _ < len, _ + 1) { i =>
      var unstructuralQuote: Long = 0x0
      var backslashQuoteBits = backslashQuotes(i)
      while(backslashQuoteBits != 0) {
        // Check the leftmost backslash
        val mask = S(backslashQuoteBits)
        val countOnes = popcnt(mask)
        val backslashOnLeft = (backslashs(i) & mask) << (64 - countOnes)
        val numberOfLeadingOnes = leadingOnes(backslashOnLeft)
        if(numberOfLeadingOnes != countOnes) {
          if((numberOfLeadingOnes & 1) == 1) {
            unstructuralQuote = unstructuralQuote | E(backslashQuoteBits)
          }
          backslashQuoteBits = R(backslashQuoteBits)
        }
        else {
          sys.error("TODO");
        }
      }
      unstructuralQuoteMask(i) = ~unstructuralQuote
    }

    // Now we use the computed mask to filter our escaped quotes
    quotes(0) &= unstructuralQuoteMask(0) << 1
    forloop(1, _ < len, _ + 1) { i =>
      quotes(i) &= (unstructuralQuoteMask(i) << 1) | (unstructuralQuoteMask(i - 1) >> 63)
    }
  }

  /**
    * See Mison 4.2.3
    * Build the string mask bitmap & mask structural characters
    */
  def filterUnstructuralCharacters(structuralCharacters: StructuralCharacters): Unit = {
    val quotes = structuralCharacters.quotes
    val len = quotes.length
    val stringMask = Array.ofDim[Long](len)
    var n = 0
    forloop(0, _ < len, _ + 1) { i =>
      var quote = quotes(i)
      var mask: Long = 0x0
      while(quote != 0) {
        mask = mask ^ S(quote)
        quote = R(quote)
        n = n + 1
      }
      stringMask(i) = if((n & 1) == 0) ~mask else mask
    }

    // Use the string mask to filter out unstructural characters
    val colons = structuralCharacters.colons
    val leftBraces = structuralCharacters.leftBraces
    val rightBraces = structuralCharacters.rightBraces
    forloop(0, _ < len, _ + 1) { i =>
      quotes(i) = stringMask(i)
      colons(i) = colons(i) & stringMask(i)
      leftBraces(i) = leftBraces(i) & stringMask(i)
      rightBraces(i) = rightBraces(i) & stringMask(i)
    }
  }

  /**
    * See Mison 4.2.4
    * Build leveled colon bitmap
    */
  def buildLeveledColonIndex(structuralCharacters: StructuralCharacters, maxLevel: Int): Index = {
    val stringMask = structuralCharacters.quotes
    val colons = structuralCharacters.colons
    val leftBraces = structuralCharacters.leftBraces
    val rightBraces = structuralCharacters.rightBraces
    val len = colons.size
    val index = Array.ofDim[Bitmap](maxLevel + 1)
    index(0) = Array.ofDim[Long](len)
    System.arraycopy(stringMask, 0, index(0), 0, len)
    forloop(0, _ < maxLevel, _ + 1) { i =>
      index(i + 1) = Array.ofDim[Long](len)
      System.arraycopy(colons, 0, index(i + 1), 0, len)
    }

    val S = new java.util.Stack[Array[Long]]()
    forloop(0, _ < len, _ + 1) { i =>
      var left = leftBraces(i)
      var right = rightBraces(i)
      loop {
        var rightBit = E(right)
        var leftBit = E(left)
        while(leftBit != 0 && (rightBit == 0 || java.lang.Long.compareUnsigned(leftBit, rightBit) < 0)) {
          S.push(Array(i, leftBit))
          left = R(left)
          leftBit = E(left)
        }
        if(rightBit != 0) {
          val x = S.pop
          val j = x(0).asInstanceOf[Int]
          val leftBit = x(1)
          val level = S.size
          if(level > 0 && level <= maxLevel) {
            val upperLevel = level - 1
            if(i == j) {
              index(upperLevel + 1)(i) = index(upperLevel + 1)(i) & ~(rightBit - leftBit)
            }
            else {
              index(upperLevel + 1)(j) = index(upperLevel + 1)(j) & (leftBit - 1)
              index(upperLevel + 1)(i) = index(upperLevel + 1)(i) & ~(rightBit - 1)
              forloop(j + 1, _ < i, _ + 1) { k =>
                index(upperLevel + 1)(k) = 0
              }
            }
          }
          right = R(right)
        }
        else {
          break
        }
      }
    }

    new Index(index)
  }

}