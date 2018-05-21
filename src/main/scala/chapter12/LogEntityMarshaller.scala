package chapter12

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString

object LogEntityMarshaller extends EventMarshalling {
  
  type LEM = ToEntityMarshaller[Source[ByteString, _]]
  def create(maxJsonObject: Int): LEM = {
    val js = ContentTypes.`application/json`
    val txt = ContentTypes.`text/plain(UTF-8)`

    val jsMarshaller = Marshaller.withFixedContentType(js) {
      src:Source[ByteString, _] => HttpEntity(js, src)
    }

    val txtMarshaller = Marshaller.withFixedContentType(txt) {
      src:Source[ByteString, _] => HttpEntity(txt, toText(src, maxJsonObject))
    }

    Marshaller.oneOf(jsMarshaller, txtMarshaller)
  }

  def toText(src: Source[ByteString, _], maxJsonObject: Int): Source[ByteString, _] = {
    src.via(LogJson.jsonToLogFlow(maxJsonObject))
  }
}

