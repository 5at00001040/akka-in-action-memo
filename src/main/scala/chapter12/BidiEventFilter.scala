package chapter12


import java.nio.file.{Path, Paths}
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._

import scala.concurrent.Future
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.{ActorAttributes, ActorMaterializer, IOResult, Supervision}
import akka.stream.scaladsl._
import akka.stream.scaladsl.JsonFraming
import akka.util.ByteString
import chapter12.LogStreamProcessor.LogParseException
import spray.json._
import com.typesafe.config.{Config, ConfigFactory}

object BidiEventFilter extends App with EventMarshalling {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val maxLine = 10240
  val maxJsonObject = 102400
  val ioType = "json"

  val inputFile = Paths.get("/tmp/test/input_json")
  val outputFile = Paths.get("/tmp/test/output")
  val filterState: State = Error

  val decider : Supervision.Decider = {
    case _: LogParseException => {
      println("catch LogParseException")
      Supervision.Resume
    }
    case _: FramingException  => {
      println("catch FramingException")
      Supervision.Resume
    }
    case _                    => Supervision.Stop
  }

  val inFlow: Flow[ByteString, Event, NotUsed] =
    if(ioType == "json") {
      JsonFraming.objectScanner(maxJsonObject)
        .map(_.decodeString("UTF8").parseJson.convertTo[Event])
    } else {
      Framing.delimiter(ByteString("\n"), maxLine)
        .map(_.decodeString("UTF8"))
        .map(LogStreamProcessor.parseLineEx)
        .collect { case Some(event) => event }
        .withAttributes(ActorAttributes.supervisionStrategy(decider))
    }

  val outFlow: Flow[Event, ByteString, NotUsed] =
    if(ioType == "json") {
      Flow[Event].map(event => ByteString(event.toJson.compactPrint))
    } else {
      Flow[Event].map{ event =>
        ByteString(LogStreamProcessor.logLine(event))
      }
    }

  val bidiFlow = BidiFlow.fromFlows(inFlow, outFlow)

  val filter: Flow[Event, Event, NotUsed] = {
    Flow[Event].filter(_.state == filterState)
  }

  val flow = bidiFlow.join(filter)


  val source: Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(inputFile)

  val sink: Sink[ByteString, Future[IOResult]] =
    FileIO.toPath(outputFile, Set(CREATE, WRITE, APPEND))



  val runnableGraph: RunnableGraph[Future[IOResult]] =
    source.via(flow).toMat(sink)(Keep.right)



  runnableGraph.run().foreach{ result =>
    println(s"Wrote ${result.count} bytes to '${outputFile}'.")
    system.terminate()
  }



}