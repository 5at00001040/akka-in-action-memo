package chapter9


import scala.concurrent.duration._

import akka.actor._
import org.scalatest._
import akka.testkit._

class SwitchRouterTest
  extends TestKit(ActorSystem("SwitchRoutingTest"))
    with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Switch Router" must {
    "routes depending on state" in {

      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(
        Props(new SwitchRouter(
          normalFlow = normalFlowProbe.ref,
          cleanUp = cleanupProbe.ref)))

      val msg = "message"
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      router ! RouteStateOn

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

    }
  }
}
