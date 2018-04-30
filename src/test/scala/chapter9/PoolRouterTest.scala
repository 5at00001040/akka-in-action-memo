package chapter9

import java.util.Date

import akka.actor._
import akka.routing.{Broadcast, FromConfig}
import akka.testkit._
import chapter8._
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

class PoolRouterTest extends TestKit(ActorSystem("PoolRouterTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  val timeout = 2 seconds

  override def afterAll(): Unit = {
    system.terminate()
  }

  "PoolRouter" must {
    "license task pool" in {

      val endProbe = TestProbe()
      val aggregateRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val speedRef = system.actorOf(Props(new GetSpeed(aggregateRef)))
      val timeRef = system.actorOf(Props(new GetTime(aggregateRef)))
      val licenseRef = system.actorOf(FromConfig.props(Props(new GetLicense(aggregateRef))), "poolRouter")
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
    "send broadcast message" in {
      val endProbe = TestProbe()
      val aggregateRef = system.actorOf(Props(new Aggregator(timeout, endProbe.ref)))
      val licenseRef = system.actorOf(FromConfig.props(Props(new GetLicense(aggregateRef))), "roundRobinPoolRouter")

      val photoDate = new Date()
      val photoSpeed = 60
      val photoLicense = "123xyz"
      val msg = PhotoMessage("id2",
        ImageProcessing.createPhotoString(photoDate, photoSpeed, photoLicense))

      licenseRef ! Broadcast(msg)

      val combinedMsg = PhotoMessage(msg.id,
        msg.photo,
        None,
        None,
        Some(photoLicense))

      endProbe.expectMsg(combinedMsg)

      // FIXME Broadcastがうまくテストできない
//      endProbe.expectMsg(combinedMsg)
//      endProbe.expectMsg(combinedMsg)


    }
  }

}
