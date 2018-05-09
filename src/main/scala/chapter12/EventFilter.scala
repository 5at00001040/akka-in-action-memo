package chapter12

import java.nio.file.Paths
import java.nio.file.StandardOpenOption._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.stream._
import chapter12.LogStreamProcessor.LogParseException
import spray.json._

import scala.concurrent.Future

object EventFilter extends App with EventMarshalling {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()


  val inputFile = Paths.get("/tmp/test/input")
  val outputFile = Paths.get("/tmp/test/output")
  val filterState: State = Error

  // test data
"""
my-host-1 | web-app | ok    | 2018-05-09T15:30:01.127Z | 5 tickets sold. ||
my-host-2 | web-app | ok    | 2018-05-09T15:30:02.127Z | 3 tickets sold. ||
my-host-1 | web-app | ok    | 2018-05-09T15:30:03.127Z | 1 tickets sold. ||
my-host-2 | web-app | error | 2018-05-09T15:30:04.127Z | exception!!     ||
my-host-3 | web-app | ok    | 2018-05-09T15:30:05.127Z | 3 tickets sold. ||
my-host-2 | web-app | ok  | | 2018-05-09T15:30:06.127Z |-1 tickets sold. ||
my-host-3 | web-app | ok    | 2018-05-09T15:30:07.127Z | 3 tickets sold. ||
"""

  val maxLine = 10240

  val source: Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(inputFile)

  val sink: Sink[ByteString, Future[IOResult]] =
    FileIO.toPath(outputFile, Set(CREATE, WRITE, APPEND))


  val frame: Flow[ByteString, String, NotUsed] = {
    Framing.delimiter(ByteString("\n"), maxLine)
      .map(_.decodeString("UTF8"))
  }

  val decider : Supervision.Decider = {
    case _: LogParseException => {
      println("catch LogParseException")
      Supervision.Resume
    }
    case _                    => Supervision.Stop
  }

  val parse: Flow[String, Event, NotUsed] = {
    Flow[String]
      .map(LogStreamProcessor.parseLineEx)
      .collect{case Some(e) => e}
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
  }

  val filter: Flow[Event, Event, NotUsed] = {
    Flow[Event].filter(_.state == filterState)
  }

  val serialize: Flow[Event, ByteString, NotUsed] = {
    Flow[Event].map(event => ByteString(event.toJson.compactPrint))
  }

  val composedFlow: Flow[ByteString, ByteString, NotUsed] = {
    frame
      .via(parse)
      .via(filter)
      .via(serialize)
  }

  val runnableGraph: RunnableGraph[Future[IOResult]] = {
    source.via(composedFlow).toMat(sink)(Keep.right)
  }



  runnableGraph.run().foreach{ result =>
    println(s"Wrote ${result.count} bytes to '${outputFile}'.")
    system.terminate()
  }

}
