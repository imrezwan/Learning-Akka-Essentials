package faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object StartingStoppingActor extends App {

  val system = ActorSystem("StoppingActorDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {

    import Parent._
    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) => // doesn't stop immidiately
        log.info(s"Stopping child $name")
        val childOption = children.get(name)
        childOption.foreach(context.stop)
      case Stop => context.stop(self)
      case message => log.info(message.toString)
    }

    override def receive: Receive = withChildren(Map())
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  import Parent._
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")

  val child = system.actorSelection("/user/parent/child1")
  child ! "Hi Kid"

//  parent ! Stop

  parent ! StopChild("child1")
//  for( i <- 1 to 40) child ! "Are you still there ? "+ i

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hi, I am second child"

  parent ! Stop
  for( _ <- 1 to 100) parent ! "Parent Are you there ?"
  for( i <- 1 to 1000) child2 ! "CHILD-2 Are you there ? => " + i


}
