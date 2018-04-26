package chapter8

import java.util.Date

import akka.actor._
import akka.testkit._
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

class ScatterGatherTest
  extends TestKit(ActorSystem("ScatterGatherTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  val timeout = 2 seconds

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The ScatterGather" must {
    "scatter the message and gather them again" in {

      val endProbe = TestProbe()
      val aggregateRef = system.actorOf(
        Props(new Aggregator(timeout, endProbe.ref)))
      val speedRef = system.actorOf(
        Props(new GetSpeed(aggregateRef)))
      val timeRef = system.actorOf(
        Props(new GetTime(aggregateRef)))
      val actorRef = system.actorOf(
        Props(new RecipientList(Seq(speedRef, timeRef))))

      val photoDate = new Date()
      val photoSpeed = 60
      val msg = PhotoMessage("id1",
        ImageProcessing.createPhotoString(photoDate, photoSpeed))

      actorRef ! msg

      val combinedMsg = PhotoMessage(msg.id,
        msg.photo,
        Some(photoDate),
        Some(photoSpeed))

      endProbe.expectMsg(combinedMsg)


    }
    "3 way merge test" in {

      val endProbe = TestProbe()
      val aggregateRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val speedRef = system.actorOf(Props(new GetSpeed(aggregateRef)))
      val timeRef = system.actorOf(Props(new GetTime(aggregateRef)))
      val licenseRef = system.actorOf(Props(new GetLicense(aggregateRef)))
      val actorRef = system.actorOf(Props(new RecipientList(Seq(speedRef, timeRef, licenseRef))))

      val photoDate = new Date()
      val photoSpeed = 60
      val photoLicense = "123xyz"
      val msg = PhotoMessage("id1",
        ImageProcessing.createPhotoString(photoDate, photoSpeed, photoLicense))

      actorRef ! msg

      val combinedMsg = PhotoMessage(msg.id,
        msg.photo,
        Some(photoDate),
        Some(photoSpeed),
        Some(photoLicense))

      endProbe.expectMsg(combinedMsg)


    }
  }
}