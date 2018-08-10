package mison

import java.util.concurrent._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

case class UrlCreated(html_url: String = "", created_at: String = "")

object UrlCreated {
  implicit val codec: JsonValueCodec[UrlCreated] = make(CodecMakerConfig())

  val fieldNames: Seq[String] = Seq("html_url", "created_at")
}

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = Array(
  "-server",
  "-Xms2g",
  "-Xmx2g",
  "-XX:NewSize=1g",
  "-XX:MaxNewSize=1g",
  "-XX:InitialCodeCacheSize=512m",
  "-XX:ReservedCodeCacheSize=512m",
  "-XX:+UseParallelGC",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch"
))
class Benchmarks {

  @Param(Array("empty", "short", "medium", "long", "huge", "giant"))
  var entry: String = _
  var json: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def prepare(): Unit = {
    Index
    json = Main.getJson(entry)
    jsonBytes = json.getBytes("UTF-8")
  }

  @Benchmark
  def mison(bh: Blackhole): Unit = {
    val values = Parser.parse(json, UrlCreated.fieldNames)
    bh.consume(values.get("html_url"))
    bh.consume(values.get("created_at"))
  }

  @Benchmark
  def fastJson(bh: Blackhole): Unit = {
    val values = com.alibaba.fastjson.JSON.parseObject(json)
    bh.consume(values.get("html_url"))
    bh.consume(values.get("created_at"))
  }

  @Benchmark
  def jackson(bh: Blackhole): Unit = {
    val mapper = new com.fasterxml.jackson.databind.ObjectMapper()
    val root = mapper.readTree(json)
    bh.consume(root.path("html_url").asText)
    bh.consume(root.path("created_at").asText)
  }

  @Benchmark
  def jsoniterScala(bh: Blackhole): Unit = {
    val root = readFromArray[UrlCreated](jsonBytes)
    bh.consume(root.html_url)
    bh.consume(root.created_at)
  }
}
