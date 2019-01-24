package com.example

import akka.actor.{Actor, ActorSystem, Props}

import scala.io.StdIn

object PrintMyActorRef{
  def  props:Props=Props(new PrintMyActorRef)
}


class PrintMyActorRef extends Actor{
  override def receive: Receive = {
    case "printit"=>
        val secondRef=context.actorOf(Props.empty,"second-actor")
          //Actor[akka://testSystem/user/first-actor/second-actor#982202745]
       println(s"Second:$secondRef")
  }
}

object myAkkaActor extends App {
  val system=ActorSystem("testSystem") // 根actor

  val firstRef=system.actorOf(PrintMyActorRef.props,"first-actor")
  // Actor[akka://testSystem/user/first-actor#-757571445]
  println(s"First :$firstRef")
  firstRef ! "printit"

  println(">>> press enter to exit <<<")
  StdIn.readLine()
  system.terminate()
}
