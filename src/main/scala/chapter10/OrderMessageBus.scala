package chapter10

import akka.event.{ActorEventBus, EventBus, LookupClassification}

class OrderMessageBus extends EventBus with LookupClassification with ActorEventBus {
  type Event = Order
  type Classifier = Boolean
  def mapSize = 2

  protected def classify(event: OrderMessageBus#Event) = {
    event.number > 1
  }

  protected def publish(event: OrderMessageBus#Event, subscriber: OrderMessageBus#Subscriber) = {
    subscriber ! event
  }
}
