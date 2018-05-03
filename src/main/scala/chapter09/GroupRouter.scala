package chapter09

import akka.actor._
import akka.actor.{Actor, Props}
import akka.routing._
import chapter08.GetLicense

class GroupRouter extends App {

}

class GetLicenseCreator(nrActors: Int, nextStep: ActorRef) extends Actor {
  var createdActors = Seq[ActorRef]()

  override def preStart(): Unit = {
    super.preStart()
    createdActors = (0 until  nrActors).map(nr => {
      context.actorOf(Props(new GetLicense(nextStep)), "GetLicense" + nr)
    })
  }

  override def receive() = {
    case _ =>
  }
}

class GetLicenseCreator2(nrActors: Int, nextStep: ActorRef) extends Actor {

  override def preStart(): Unit = {
    super.preStart()
    (0 until nrActors).map(nr => {
      val child = context.actorOf(Props(new GetLicense(nextStep)), "GetLicense" + nr)
      context.watch(child)
    })
  }

  override def receive() = {
    case Terminated(child) => {
      val newChild = context.actorOf(Props(new GetLicense(nextStep)), child.path.name)
      context.watch(newChild)
    }
  }
}

case class PreferredSize(size: Int)

class DynamicRouteeSizer(nrActors: Int, props: Props, router: ActorRef) extends Actor {
  var nrChildren = nrActors
  var childInstanceNr = 0

  override def preStart() = {
    super.preStart()
    (0 until nrChildren).map(nr => createRoutee())
  }

  def createRoutee() = {
    childInstanceNr += 1
    val child = context.actorOf(props, "routee" + childInstanceNr)
    val selection = context.actorSelection(child.path)
    router ! AddRoutee(ActorSelectionRoutee(selection))
    context.watch(child)
  }

  override def receive() = {
    case PreferredSize(size) => {
      if (size < nrChildren) {
        context.children.take(nrChildren - size).foreach(
          ref => {
            val selection = context.actorSelection(ref.path)
            router ! RemoveRoutee(ActorSelectionRoutee(selection))
          }
        )
        router ! GetRoutees
      } else {
        (nrChildren until size).map(nr => createRoutee())
      }
      nrChildren = size
    }
    case routees: Routees => {
      import collection.JavaConversions._

      val active = routees.getRoutees.map{
        case x: ActorRefRoutee => x.ref.path.toString
        case x: ActorSelectionRoutee => x.selection.pathString
      }

      for(routee <- context.children) {
        val index = active.indexOf(routee.path.toStringWithoutAddress)
        if (index >= 0) {
          active.remove(index)
        } else {
          routee ! PoisonPill
        }
      }

      for (terminated <- active) {
        val name = terminated.substring(terminated.lastIndexOf("/")+1)
        val child = context.actorOf(props, name)
        context.watch(child)
      }



      println("routees " + routees)
      if(routees.getRoutees.size() < nrChildren) {
        (routees.getRoutees.size() until nrChildren).map(nr => createRoutee())
      }
    }
    case Terminated(child) => {
      router ! GetRoutees
    }
  }



}
