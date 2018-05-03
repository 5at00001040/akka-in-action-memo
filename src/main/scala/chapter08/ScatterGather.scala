package chapter08

import java.util.Date
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer

import akka.actor._
import java.text.SimpleDateFormat


case class PhotoMessage(id: String,
                        photo: String,
                        creationTime: Option[Date] = None,
                        speed: Option[Int] = None,
                        license: Option[String] = None)


object ImageProcessing {
  val dateFormat = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS")
  def getSpeed(image: String): Option[Int] = {
    val attributes = image.split('|')
    if (attributes.size == 3)
      Some(attributes(1).toInt)
    else
      None
  }
  def getTime(image: String): Option[Date] = {
    val attributes = image.split('|')
    if (attributes.size == 3)
      Some(dateFormat.parse(attributes(0)))
    else
      None
  }
  def getLicense(image: String): Option[String] = {
    val attributes = image.split('|')
    if (attributes.size == 3)
      Some(attributes(2))
    else
      None
  }
  def createPhotoString(date: Date, speed: Int): String = {
    createPhotoString(date, speed, " ")
  }

  def createPhotoString(date: Date,
                        speed: Int,
                        license: String): String = {
    "%s|%s|%s".format(dateFormat.format(date), speed, license)
  }
}

class GetSpeed(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: PhotoMessage => {
      pipe ! msg.copy(
        speed = ImageProcessing.getSpeed(msg.photo))
    }
  }
}
class GetTime(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: PhotoMessage => {
      pipe ! msg.copy(creationTime =
        ImageProcessing.getTime(msg.photo))
    }
  }
}
class GetLicense(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: PhotoMessage => {
      println("got licence message: " + msg.id)
      pipe ! msg.copy(license =
        ImageProcessing.getLicense(msg.photo))
    }
  }
}



class RecipientList(recipientList: Seq[ActorRef]) extends Actor {
  def receive = {
    case msg: AnyRef => recipientList.foreach(_ ! msg)
  }
}


case class TimeoutMessage(msg: PhotoMessage)
case class MessageBuffer(msg: PhotoMessage, mergeCount: Int)

class Aggregator(timeout: FiniteDuration, pipe: ActorRef)
  extends Actor {

  val messages = new ListBuffer[MessageBuffer]
  implicit val ec = context.system.dispatcher
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    messages.foreach(self ! _.msg)
    messages.clear()
  }

  def receive = {
    case rcvMsg: PhotoMessage => {
      messages.find(_.msg.id == rcvMsg.id) match {
        case Some(alreadyBuffer) => {
          val newCombinedMsg = new PhotoMessage(
            rcvMsg.id,
            rcvMsg.photo,
            rcvMsg.creationTime.orElse(alreadyBuffer.msg.creationTime),
            rcvMsg.speed.orElse(alreadyBuffer.msg.speed),
            rcvMsg.license.orElse(alreadyBuffer.msg.license)
          )

          val mergeCount = alreadyBuffer.mergeCount + 1
          if (mergeCount >= 3) {
            pipe ! newCombinedMsg
          } else {
            messages += MessageBuffer(newCombinedMsg, mergeCount)
          }

          //cleanup message
          messages -= alreadyBuffer
        }
        case None => {
          messages += MessageBuffer(rcvMsg, 1)
          context.system.scheduler.scheduleOnce(
            timeout,
            self,
            new TimeoutMessage(rcvMsg))
        }
      }
    }
    case TimeoutMessage(rcvMsg) => {
      messages.find(_.msg.id == rcvMsg.id) match {
        case Some(alreadyBuffer) => {
          pipe ! alreadyBuffer.msg
          messages -= alreadyBuffer
        }
        case None => //message is already processed
      }
    }
    case ex: Exception => throw ex
  }
}

