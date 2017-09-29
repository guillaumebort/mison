package mison

import Macros._

object Parser {

  def parse(json: String, query: Seq[String], encoding: String = "utf-8"): Map[String,String] = {
    val jsonBytes = json.getBytes(encoding)
    val index = Index(jsonBytes, 1)
    val stringMask = index.bitmaps(0)
    val level1Positions = index.getColonPositions(0, json.size, 1)
    val len = level1Positions.size
    val fields = query.zipWithIndex.toMap

    @inline def fieldNameBounds(colonPosition: Int): (Int, Int) = {
      var (start, end) = (-1, -1)
      forloop((colonPosition + 63) / 64 - 1, _ >= 0, _ - 1) { i =>
        var (s, e) = (-1, -1)
        var mask = ~stringMask(i)
        loop {
          val bits = E(mask)
          val offset = (i * 64) + trailingZeros(bits)
          if(offset < colonPosition) {
            if(offset == e + 1) {
              e = offset
            }
            else {
              s = offset
              e = offset
            }
            mask = R(mask)
            if(mask == 0) break
          }
          else {
            break
          }
        }
        if(e == start - 1) {
          start = s
        }
        else {
          start = s
          end = e
        }
        if(start % 64 != 0) {
          break
        }
      }
      (start, end)
    }

    @inline def fieldName(colonPosition: Int): String = {
      val (start, end) = fieldNameBounds(colonPosition)
      new String(jsonBytes, start, end - start, encoding)
    }

    @inline def extractValue(from: Int, to: Int): String = {
      var (start, end) = (-1, -1)
      val to0 = if(to == -1) (json.size - 1) else (fieldNameBounds(to)._1 - 2)
      forloop(from, _ < to0, _ + 1) { i =>
        val c = json(i)
        if(c != ' ') {
          start = i
          break
        }
      }
      forloop(to0, _ > start, _ - 1) { i =>
        val c = jsonBytes(i).toChar
        if(c != ' ' && c != ',' && c != '}') {
          end = i + 1
          break
        }
      }
      new String(jsonBytes, start, end - start, encoding)
    }

    val result = collection.mutable.HashMap.empty[String,String]
    forloop(0, _ < len, _ + 1) { i =>
      val position = level1Positions.get(i)
      val field = fieldName(position)
      if(fields.contains(field)) {
        val value = extractValue(position + 1, if((i + 1) < level1Positions.size) level1Positions.get(i + 1) else -1)
        result += (field -> value)
      }
    }

    result.toMap
  }

}