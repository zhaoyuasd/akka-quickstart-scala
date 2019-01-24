package com.example.demo

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.io.StdIn

class IotSupervisor extends Actor with ActorLogging{
  override def preStart(): Unit = log.info("IotSupervisor app start")

  override def postStop(): Unit = log.info("IotSupervisor app stop")

  override def receive: Receive = Actor.emptyBehavior
}
object  IotSupervisor{
  def props:Props=Props(new IotSupervisor )
}

object IotApp extends App {
 val sys=ActorSystem("iot-system")

  try{
    val supervisor=sys actorOf(IotSupervisor props,"iot-supervisor")
    StdIn readLine()
  }finally {
    sys.terminate()
  }
}
