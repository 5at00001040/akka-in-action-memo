package chapter12

import java.nio.file.{Files, Path}

import akka.Done

import scala.util.{Failure, Success}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{BidiFlow, FileIO, Flow, Framing, Keep}
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import spray.json._
import akka.http.scaladsl.server.Directives._


// test command
/*
echo -e "my-host-1 | web-app | ok    | 2018-05-09T15:30:01.127Z | 5 tickets sold. ||\n" | http -v POST http://localhost:5000/logs/1 Content-Type:text/plain
http -v GET http://localhost:5000/logs/1
 */

class LogsApi(
               val logsDir: Path,
               val maxLine: Int
             )(
               implicit val executionContext: ExecutionContext,
               val materializer: ActorMaterializer
             ) extends EventMarshalling {
  def logFile(id: String) = logsDir.resolve(id)

  val inFlow = Framing.delimiter(ByteString("\n"), maxLine)
  .map(x => {
    val y = x.decodeString("UTF8")
    println(y)
    y
  })
    .map(LogStreamProcessor.parseLineEx)
    .collect{case Some(e) => e}

  val outFlow = Flow[Event].map { event =>
    ByteString(event.toJson.compactPrint)
  }

  val bidiFlow = BidiFlow.fromFlows(inFlow, outFlow)


  import java.nio.file.StandardOpenOption._


  val logToJsonFlow = bidiFlow.join(Flow[Event])

  def logFileSink(logId: String) = FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))
  def logFileSource(logId: String) = FileIO.fromPath(logFile(logId))

  def routes: Route = postRoute ~ getRoute

  def postRoute =
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post {
          entity(as[HttpEntity]) { entity =>
            onComplete(
              entity
                .dataBytes
                .via(logToJsonFlow)
                .toMat(logFileSink(logId))(Keep.right)
                .run()
            ) {
              case Success(IOResult(count, Success(Done))) =>
                complete((StatusCodes.OK, LogReceipt(logId, count)))
              case Success(IOResult(count, Failure(e))) =>
                complete((StatusCodes.BadRequest, ParseError(logId, e.getMessage)))
              case Failure(e) =>
                complete((StatusCodes.BadRequest, ParseError(logId, e.getMessage)))
            }
          }
        }
      }
    }

  def getRoute =
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        get {
          if (Files.exists(logFile(logId))) {
            val src = logFileSource(logId)
            complete(
              HttpEntity(ContentTypes.`application/json`, src)
            )
          } else {
            complete(StatusCodes.NotFound)
          }
        }
      }

    }
}
