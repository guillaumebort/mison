package mison

object Main {
  import Index._

  def main(args: Array[String]): Unit = {
    val json = getJson("long")
    println(json)

    val values = Parser.parse(json, Seq("html_url", "created_at"))
    println(values.get("html_url"))
    println(values.get("created_at"))
  }

  // -- Utilities

  def toBinaryString(bitmap: Bitmap): String = {
    val buffer = new StringBuffer()
    bitmap.foreach { l =>
      buffer.append(toBinaryString(l))
    }
    buffer.toString
  }

  def toBinaryString(l: Long): String = {
    val str = String.format("%64s", java.lang.Long.toBinaryString(l)).replace(' ', '0')
    new StringBuffer(str).reverse.toString
  }

  def toBinaryString(l: Int): String = {
    val str = String.format("%32s", java.lang.Integer.toBinaryString(l)).replace(' ', '0')
    new StringBuffer(str).reverse.toString
  }

  def getJson(w: String): String = {
    io.Source.fromInputStream(getClass.getResourceAsStream("test.json")).getLines.collect {
      case line if line.startsWith(s"$w:") =>
        line.drop(w.size + 1)
    }.toList.head
  }
}