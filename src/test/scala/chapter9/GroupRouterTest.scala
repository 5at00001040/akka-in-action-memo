package chapter9

import java.util.Date

import akka.actor._
import akka.routing.{Broadcast, FromConfig, RoundRobinGroup}
import akka.testkit._
import akka.util.Timeout
import chapter8._
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

class GroupRouterTest extends TestKit(ActorSystem("GroupRouterTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  val timeout = 2 seconds

  override def afterAll(): Unit = {
    system.terminate()
  }

  "GroupRouter" must {
    "group router" in {

      val endProbe = TestProbe()
      val creator = system.actorOf(Props(new GetLicenseCreator(2, endProbe.ref)),"Creator")
      val router = system.actorOf(FromConfig.props(), "groupRouter")

      val photoLicense = "123xyz"
      val msg = PhotoMessage("id1",
        ImageProcessing.createPhotoString(new Date(), 60, photoLicense))

      router ! msg

      val combinedMsg = PhotoMessage(msg.id,
        msg.photo,
        None,
        None,
        Some(photoLicense))

      endProbe.expectMsg(combinedMsg)

    }
    "restart routee" in {

      val endProbe = TestProbe()
      val creator = system.actorOf(Props(new GetLicenseCreator2(2, endProbe.ref)),"Creator2")
      val paths = List("/user/Creator2/GetLicense0", "/user/Creator2/GetLicense1")
      val router = system.actorOf(RoundRobinGroup(paths).props(), "groupRouter2")

      router ! Broadcast(PoisonPill)
      Thread.sleep(100)

      val photoLicense = "123xyz"
      val msg = PhotoMessage("id1",
        ImageProcessing.createPhotoString(new Date(), 60, photoLicense))

      router ! msg

      val combinedMsg = PhotoMessage(msg.id,
        msg.photo,
        None,
        None,
        Some(photoLicense))

      //      endProbe.expectMsg(combinedMsg)
      endProbe.expectMsgType[PhotoMessage](1 second)

    }
  }

}