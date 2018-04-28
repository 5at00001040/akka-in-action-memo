package chapter8

import akka.actor._
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable.ListBuffer

object CarOption extends Enumeration {
  val CAR_COLOR_GRAY, NAVIGATION, PERKING_SENSORS = Value
}

case class Order(options: Seq[CarOption.Value])
case class Car(color: String = "", hasNavigation: Boolean = false, hasPerkingSensors: Boolean = false)

case class RouteSlipMessage(routeSlip: Seq[ActorRef], message: AnyRef)

trait RouteSlip {
  def sendMessageToNextTask(routeSlip: Seq[ActorRef], message: AnyRef): Unit = {
    val nextTask = routeSlip.head
    val newSlip = routeSlip.tail

    if (newSlip.isEmpty) {
      nextTask ! message
    } else {
      nextTask ! RouteSlipMessage(routeSlip = newSlip, message = message)
    }
  }
}

class PaintCar(color: String) extends Actor with RouteSlip {
  override def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip, car.copy(color = color))
    }
  }
}

class AddNavigation() extends Actor with RouteSlip {
  override def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip, car.copy(hasNavigation = true))
    }
  }
}

class AddParkingSensors() extends Actor with RouteSlip {
  override def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip, car.copy(hasPerkingSensors = true))
    }
  }
}

class SlipRouter(endStep: ActorRef) extends Actor with RouteSlip {
  val paintBlack: ActorRef = context.actorOf(Props(new PaintCar("brack")), "paintBlack")
  val paintGray: ActorRef = context.actorOf(Props(new PaintCar("gray")), "paintGray")
  val addNavigation: ActorRef = context.actorOf(Props[AddNavigation], "navigation")
  val addParkingSensor: ActorRef = context.actorOf(Props[AddParkingSensors], "parkingSensor")

  override def receive = {
    case order: Order => {
      val routeSlip = createRouteSlip(order.options)
      sendMessageToNextTask(routeSlip, new Car)
    }
  }

  private def createRouteSlip(options: Seq[CarOption.Value]): Seq[ActorRef] = {
    val routeSlip = new ListBuffer[ActorRef]
    if (!options.contains(CarOption.CAR_COLOR_GRAY)) {
      routeSlip += paintBlack
    }
    options.foreach{
      case CarOption.CAR_COLOR_GRAY => routeSlip += paintGray
      case CarOption.NAVIGATION => routeSlip += addNavigation
      case CarOption.PERKING_SENSORS => routeSlip += addParkingSensor
      case _ =>
    }
    routeSlip += endStep
    routeSlip
  }
}
