package stateless.transformations

import java.time.Duration
import java.util.Properties

import org.apache.kafka.streams.{KafkaStreams, Topology}
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import utils.Settings


class BranchTopology extends App {

  import Serdes._

  val props: Properties = Settings.createBasicStreamProperties(
    "stateless-branch-application", "localhost:9092")

  run()

  private def run(): Unit = {
    val topology = createTopolgy()
    val streams: KafkaStreams = new KafkaStreams(topology, props)
    streams.start()
    sys.ShutdownHookThread {
      streams.close(Duration.ofSeconds(10))
    }
  }

  def createTopolgy(): Topology = {

    val builder: StreamsBuilder = new StreamsBuilder
    val textLines: KStream[String, String] =
      builder.stream[String, String]("InputTopic")

    val predicates : List[(String, String) => Boolean] = List(
      (k,v) => v.startsWith("Odd"),
      (k,v) => v.startsWith("Even")
    )

    //Branch (or split) a KStream based on the supplied predicates into one or more KStream instances.
    //Predicates are evaluated in order. A record is placed to one and only one output stream on
    //the first match: if the n-th predicate evaluates to true, the record is placed to n-th stream.
    //If no predicate matches, the the record is dropped.
    //Branching is useful, for example, to route records to different downstream topics.
    val branches : Array[KStream[String, String]] = textLines.branch(predicates:_*)
    branches(0).to("OddTopic")
    branches(1).to("EvenTopic")
    builder.build()
  }
}
