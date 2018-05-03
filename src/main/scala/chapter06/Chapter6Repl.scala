package chapter06

object BackendRepl {

  // start sbt console
  // sbt console

  // list 6.2

  val conf = """
  akka {
    actor {
      provider = "akka.remote.RemoteActorRefProvider"
    }
    remote {
      enabled-transports = ["akka.remote.netty.tcp"]
      netty.tcp {
        hostname = "0.0.0.0"
        port = 2551
      }
    }
  }
  """

  // list 6.3

  import akka.actor._
  import com.typesafe.config._

  // start akka
  val config = ConfigFactory.parseString(conf)
  val backend = ActorSystem("backend", config)


  // list 6.4

  class Simple extends Actor {
    def receive = {
      case m => println(s"received $m!")
    }
  }

  backend.actorOf(Props[Simple], "simple")

  // stop akka
  // backend.terminate()

  // quit sbt console
  // :q

}

object FrontendRepl {

  // start sbt console
  // sbt console

  // list 6.5

  val conf = """
    akka {
    actor {
      provider = "akka.remote.RemoteActorRefProvider"
    }
    remote {
      enabled-transports = ["akka.remote.netty.tcp"]
      netty.tcp {
        hostname = "0.0.0.0"
        port = 2552
      }
    }
  }
  """

  import akka.actor._
  import com.typesafe.config._

  // start akka
  val config = ConfigFactory.parseString(conf)
  val frontend = ActorSystem("frontend", config)



  // list 6.6

  val path = "akka.tcp://backend@0.0.0.0:2551/user/simple"
  val simple = frontend.actorSelection(path)

  simple ! "Hello Remote World!"


  // stop akka
  // frontend.terminate()

  // quit sbt console
  // :q

}
