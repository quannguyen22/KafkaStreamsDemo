package joining

import java.io._
import java.util.Properties

import common.PropsHelper
import org.apache.kafka.common.serialization.{StringDeserializer, _}
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.{ConsumerRecordFactory, OutputVerifier}
import org.scalatest._

class LeftJoinTopologyTests
  extends FunSuite
  with BeforeAndAfter
  with Matchers {

  val props = PropsHelper.createBasicStreamProperties("left-join-application", "localhost:9092")
  val integerDeserializer: IntegerDeserializer = new IntegerDeserializer
  val stringDeserializer: StringDeserializer = new StringDeserializer

  before {
  }

  after {
  }


  test("Should produce correct output") {

    //arrange
    val recordFactory: ConsumerRecordFactory[java.lang.Integer, java.lang.String] =
      new ConsumerRecordFactory[java.lang.Integer, java.lang.String](new IntegerSerializer, new StringSerializer)
    val leftJoinTopology = new LeftJoinTopology()
    val testDriver = new TopologyTestDriver(leftJoinTopology.createTopolgy(), props)

    //act
    testDriver.pipeInput(recordFactory.create("LeftTopic", 1, null, 1900L))
    testDriver.pipeInput(recordFactory.create("RightTopic", 1, null, 1901L))

    testDriver.pipeInput(recordFactory.create("LeftTopic", 1, "1", 2902L))


    OutputVerifier.compareValue(testDriver.readOutput("leftJoinOutput", integerDeserializer, stringDeserializer),
      "Left='1', Right='null'")


    //push these out past 2 seconds (which is what Topology Join Window duration is)
    Thread.sleep(2500)

    testDriver.pipeInput(recordFactory.create("LeftTopic", 1, "2", 5916L))
    testDriver.pipeInput(recordFactory.create("RightTopic", 1, "4", 5916L))
    OutputVerifier.compareValue(testDriver.readOutput("leftJoinOutput", integerDeserializer, stringDeserializer),
      "Left='2', Right='4'")
    OutputVerifier.compareValue(testDriver.readOutput("leftJoinOutput", integerDeserializer, stringDeserializer),
      "Left='2', Right='4'")
    val result1 = testDriver.readOutput("leftJoinOutput", integerDeserializer, stringDeserializer)
    assert(result1 == null)

    cleanup(props, testDriver)
  }


  def cleanup(props:Properties, testDriver: TopologyTestDriver) = {

    try {
      //there is a bug on windows which causes this line to throw exception
      testDriver.close
    } catch {
      case e: Exception => {
        delete(new File("C:\\data\\kafka-streams"))
      }
    }
  }

  def delete(file: File) {
    if (file.isDirectory)
      Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete(_))
    file.delete
  }
}