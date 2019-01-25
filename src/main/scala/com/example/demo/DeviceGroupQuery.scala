package com.example.demo

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

object DeviceGroupQuery {
  case object  CollectionTimeout
  def props(actorToDeviceId:Map[ActorRef,String],
             requestId:Long,
             requester:ActorRef,
             timeout:FiniteDuration ):Props=Props(new DeviceGroupQuery(actorToDeviceId,requestId,requester,timeout))

}

class DeviceGroupQuery(
  actorToDeviceId:Map[ActorRef,String],
  requestId:Long,
  requester:ActorRef,
  timeout:FiniteDuration )
extends Actor with ActorLogging{
    import DeviceGroupQuery._
    import context.dispatcher

  // 超时设置
  val queryTimeoutTimer=context.system.scheduler.scheduleOnce(timeout,self,CollectionTimeout)

  override def preStart(): Unit = {
    actorToDeviceId.keysIterator.foreach{
      deviceActor=>
           context.watch(deviceActor)  //监督每一个actor 获得其终止时发送的终止信息
           deviceActor! Device.ReadTemperature(0)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()  //如果提前获取资源 则结束这个等待
  }

  override def receive: Receive =waitingForReplies(Map.empty,actorToDeviceId.keySet)

  def  waitingForReplies(
     repliesSoFar:Map[String,DeviceGroup.TemperatureReading],//已经恢复的设备 以及对应的结果
     stillWaiting: Set[ActorRef]  // 在等待结果的设备
                        ):Receive={
    // 这里的 match是什么 直接就上case了
    case Device.RespondTemperature(0,valueOption)=>
       val deviceActor=sender()
       val reading= valueOption match {
         case Some(value)=>DeviceGroup.Temperature(value)
         case None=>DeviceGroup.TemperatureNotAvailable
        }
       receiveResponse(deviceActor,reading,stillWaiting,repliesSoFar)

    case Terminated(deviceActor)=>
      receiveResponse(deviceActor,DeviceGroup.TemperatureNotAvailable,stillWaiting,repliesSoFar)

    case CollectionTimeout =>
      val timeOutReplies=
          stillWaiting.map{
            deviceActor=>
            val deviceId=actorToDeviceId(deviceActor)
            deviceId->DeviceGroup.DeviceTimedOut
          }
        requester !DeviceGroup.RespondAllTemperatures(requestId,repliesSoFar++timeOutReplies)
        context.stop(self)
  }

  def receiveResponse(deviceActor: ActorRef,
                      reading: DeviceGroup.TemperatureReading,
                      stillWaiting: Set[ActorRef],
                      repliesSoFar: Map[String, DeviceGroup.TemperatureReading]):Unit={
    context.unwatch(deviceActor)
    val deviceId=actorToDeviceId(deviceActor)

    val newStillWaiting=stillWaiting-deviceActor

    val newRepliesSoFar=repliesSoFar+(deviceId->reading)

    if(newStillWaiting isEmpty){
      requester ! DeviceGroup.RespondAllTemperatures(requestId,newRepliesSoFar)
      context.stop(self) //这里手动结束查询 同时也结束那个超时等待
    }
    else{
      context.become(waitingForReplies(newRepliesSoFar,newStillWaiting))
    }
  }

}