package chapter12

import akka.stream.scaladsl.{BidiFlow, Flow, Framing, JsonFraming}
import akka.util.ByteString
import spray.json._

object LogJson extends EventMarshalling {
  def textInFlow(maxLine: Int) = {
    Framing.delimiter(ByteString("\n"), maxLine)
    .map(_.decodeString("UTF8"))
    .map(LogStreamProcessor.parseLineEx)
    .collect { case Some(e) => e }
  }

  def jsonInFlow(maxJsonObject: Int) = {
    JsonFraming.objectScanner(maxJsonObject) 
      .map(_.decodeString("UTF8").parseJson.convertTo[Event])
  }

  def jsonFramed(maxJsonObject: Int) =
    JsonFraming.objectScanner(maxJsonObject) 

  val jsonOutFlow = Flow[Event].map { event => 
    ByteString(event.toJson.compactPrint)
  }

  val textOutFlow = Flow[Event].map{ event => 
    ByteString(LogStreamProcessor.logLine(event))
  }

  def logToJson(maxLine: Int) = {
    BidiFlow.fromFlows(textInFlow(maxLine), jsonOutFlow)
  }

  def jsonToLog(maxJsonObject: Int) = {
    BidiFlow.fromFlows(jsonInFlow(maxJsonObject), textOutFlow)
  }

  def logToJsonFlow(maxLine: Int) = {
    logToJson(maxLine).join(Flow[Event])
  }

  def jsonToLogFlow(maxJsonObject: Int) = {
    jsonToLog(maxJsonObject).join(Flow[Event])
  }
}