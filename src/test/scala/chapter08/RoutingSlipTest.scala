package chapter08

import java.util.Date

import akka.actor._
import akka.testkit._
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

class RoutingSlipTest
  extends TestKit(ActorSystem("RoutingSlipTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  val timeout = 2 seconds

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The RoutingSlip" must {
    "vanilla car" in {

      val probe = TestProbe()
      val router = system.actorOf(Props(new SlipRouter(probe.ref)), "VanillaSlipRouter")

      val minimalOrder = Order(Seq())

      router ! minimalOrder

      val defaultCar = new Car(
        color = "brack",
        hasNavigation = false,
        hasPerkingSensors = false
      )

      probe.expectMsg(defaultCar)

    }
    "mashimashi car" in {

      val probe = TestProbe()
      val router = system.actorOf(Props(new SlipRouter(probe.ref)), "MashiMashiSlipRouter")

      val fullOrder = new Order(Seq(CarOption.CAR_COLOR_GRAY, CarOption.NAVIGATION, CarOption.PERKING_SENSORS))

      router ! fullOrder

      val carWithFullOptions = new Car(
        color = "gray",
        hasNavigation = true,
        hasPerkingSensors = true
      )

      probe.expectMsg(carWithFullOptions)

    }
  }
}