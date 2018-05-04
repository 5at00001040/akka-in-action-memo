package chapter10

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import scala.concurrent.duration._

class OrderMessageBusTest extends TestKit(ActorSystem("OrderMessageBusTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Custom Bus" must {
    "deliver messages" in {

      val bus = new OrderMessageBus

      val singleBooks = TestProbe()
      bus.subscribe(singleBooks.ref, false)

      val multiBooks = TestProbe()
      bus.subscribe(multiBooks.ref, true)


      val msg1 = Order("me", "Akka in Action", 1)
      bus.publish(msg1)

      singleBooks.expectMsg(msg1)
      multiBooks.expectNoMsg(3 second)

      val msg2 = Order("me", "Akka in Action", 3)
      bus.publish(msg2)

      singleBooks.expectNoMsg(3 second)
      multiBooks.expectMsg(msg2)

    }
  }
}
