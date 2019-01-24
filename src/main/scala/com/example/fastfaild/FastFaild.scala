package com.example.fastfaild

import akka.actor.{Actor, ActorSystem, Props}

class Father extends Actor{
 val ch= context.actorOf(Child.props)

  override def receive: Receive = {
    case "fail"=>ch!"fail"
  }
}
object  Father {
  def props:Props=Props(new Father)
}

class Child extends  Actor{
  override def preStart(): Unit = println(" child  startup")

  override def postStop(): Unit = println(" child  stop")

  override def receive: Receive = {
    case "fail"=> throw new Exception("child fail")
  }
}

object Child{
  def props:Props=Props(new Child)
}

/*
 actor 的失败管理
 每当一个actor失败（抛出异常或未处理的异常气泡receive）时，它会暂时中止。
 如前所述，失败信息将传播到父级，然后父级决定如何处理子actor引起的异常。
 通过这种方式，父母可以担任孩子的监督员。默认的主管策略是停止并重新启动子项。
 如果不更改默认策略，则所有失败都会导致重新启动
 */
object FastFaild extends  App {
  val sys=ActorSystem("root")
  val fa=sys actorOf(Father props,"father")
  fa ! "fail"
}
