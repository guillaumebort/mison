package mison

import java.util.concurrent._
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 200, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
class Benchmarks {

  @Param(Array("empty", "short", "medium", "long", "huge", "giant"))
  var entry: String = _
  var json: String = _

  @Setup
  def prepare(): Unit = {
    Index
    json = Main.getJson(entry)
  }

  @Benchmark
  def mison() {
    val values = Parser.parse(json, Seq("html_url", "created_at"))
    val htmlUrl = values.get("html_url")
    val createdAt = values.get("created_at")
  }

  @Benchmark
  def fastJson() {
    val values = com.alibaba.fastjson.JSON.parseObject(json)
    val htmlUrl = values.get("html_url")
    val createdAt = values.get("created_at")
  }

  @Benchmark
  def jackson() {
    val mapper = new com.fasterxml.jackson.databind.ObjectMapper()
    val root = mapper.readTree(json)
    val htmlUrl = root.path("html_url").asText
    val createdAt = root.path("created_at").asText
  }

}
