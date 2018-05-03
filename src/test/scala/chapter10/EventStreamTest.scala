package chapter10

import akka.testkit.{ TestProbe, TestKit }
import akka.actor.ActorSystem
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import java.util.Date
import scala.concurrent.duration._

class EventStreamTest extends TestKit(ActorSystem("EventStreamTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "EventStream" must {
    "distribute messages" in {

      val deliverOrder = TestProbe()
      val giftModule = TestProbe()

      system.eventStream.subscribe(deliverOrder.ref, classOf[Order])
      system.eventStream.subscribe(giftModule.ref, classOf[Order])

      val msg = Order("me", "Akka in Action", 3)
      system.eventStream.publish(msg)

      deliverOrder.expectMsg(msg)
      giftModule.expectMsg(msg)

      system.eventStream.unsubscribe(giftModule.ref)

      system.eventStream.publish(msg)

      deliverOrder.expectMsg(msg)
      giftModule.expectNoMsg(3 second)

    }
  }
}
