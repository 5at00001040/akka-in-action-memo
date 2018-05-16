package chapter12

import java.nio.file.{FileSystems, Files}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future

object LogsApp extends App {

  val config = ConfigFactory.load() 
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val logsDir = {
    val dir = config.getString("log-stream-processor.logs-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }
  val maxLine = config.getInt("log-stream-processor.max-line")

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher
  
  val decider : Supervision.Decider = {
    case _: LogStreamProcessor.LogParseException => Supervision.Stop
    case _                    => Supervision.Stop
  }
  
  implicit val materializer = ActorMaterializer(
   ActorMaterializerSettings(system)
     .withSupervisionStrategy(decider)
  )
  
  val api = new LogsApi(logsDir, maxLine).routes
 
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port)
 
  val log =  Logging(system.eventStream, "logs")
  bindingFuture.map { serverBinding =>
    log.info(s"Bound to ${serverBinding.localAddress} ")
  }.onFailure { 
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }
}
