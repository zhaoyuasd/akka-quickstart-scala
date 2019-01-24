package com.example

import akka.actor.{Actor, ActorSystem, Props}

/*
preStart() 在actor开始之后但在它处理第一条消息之前调用。
postStop()在actor停止之前调用。此后不会处理任何消息。
StartStopActor1 start
StartStopActor2 start
StartStopActor2 stop
StartStopActor1 stop
 */


class StartStopActor1 extends  Actor{
  override def preStart(): Unit = {
    println("StartStopActor1 start")
    context.actorOf(StartStopActor2.props,"second")
  }

  override def postStop(): Unit = println("StartStopActor1 stop")

  override def receive: Receive = {
    case "stop" => context.stop(self)
  }
}

object StartStopActor1{
  def pros:Props=Props(new StartStopActor1)
}

class StartStopActor2 extends Actor {
  override def preStart(): Unit = println("StartStopActor2 start")
  override def postStop(): Unit = println("StartStopActor2 stop")
  override def receive: Receive = Actor.emptyBehavior
}

object StartStopActor2{
  def props:Props=Props(new StartStopActor2)
}


object akkaPreStartAndPreStop extends App {
   val sys=ActorSystem("akkaPreStartAndPreStop")
   val first=sys.actorOf(StartStopActor1.pros,"first")
       first ! "stop"

}
