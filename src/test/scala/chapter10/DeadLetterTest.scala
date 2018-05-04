package chapter10

import java.util.Date

import akka.actor.{ActorSystem, DeadLetter, PoisonPill, Props}
import akka.testkit.TestActors.EchoActor
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

class DeadLetterTest extends TestKit(ActorSystem("DeadLetterTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers
  with ImplicitSender {

  override def afterAll()  {
    system.terminate()
  }

  "DeadLetter" must {
    "Destination error" in {
      val deadLetterMonitor = TestProbe()

      system.eventStream.subscribe(deadLetterMonitor.ref, classOf[DeadLetter])

      val actor = system.actorOf(Props[EchoActor], "echo")
      actor ! PoisonPill

      val msg = Order("me", "Akka in Action", 1)
      actor ! msg

      val dead = deadLetterMonitor.expectMsgType[DeadLetter]
      dead.message must be(msg)
      dead.sender must be(testActor)
      dead.recipient must be(actor)
    }
    "Error handling" in {
      val deadLetterMonitor = TestProbe()

      system.eventStream.subscribe(deadLetterMonitor.ref, classOf[DeadLetter])

      val actor = system.actorOf(Props[EchoActor], "echo")

      val msg = Order("me", "Akka in Action", 1)
      val dead = DeadLetter(msg, testActor, actor)
      system.deadLetters ! dead

      deadLetterMonitor.expectMsg(dead)
    }
  }
}
