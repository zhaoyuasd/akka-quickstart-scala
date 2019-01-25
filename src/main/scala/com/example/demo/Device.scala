package com.example.demo

import akka.actor.{Actor, ActorLogging, Props}

object Device {
 def props(groupId:String,deviceId:String):Props=Props(new Device(groupId,deviceId))

  final case class ReadTemperature(resquestId:Long)  //温度读取
  final case class RespondTemperature(resquestId:Long,value:Option[Double]) //温度响应

  final case class RecordTemperature(resquestId:Long,value:Double)
  final case class TemperatureRecorded(resquestId:Long)
}

class Device(groupId:String,deviceId:String) extends Actor with ActorLogging{
 import  Device. _

  var  lastTemperatureReading:Option[Double]=None

  override def preStart(): Unit = log.info("Device actor {}-{} start",groupId,deviceId)

  override def postStop(): Unit = log.info("Device actor {}-{} stop",groupId,deviceId)

  override def receive: Receive = {
    case ReadTemperature(id) =>
       sender() ! RespondTemperature(id,lastTemperatureReading)

    case RecordTemperature(id,value)=>
      log.info("Recorded temperature reading {} with {} ",value,id)
      lastTemperatureReading=Some(value)
      sender() ! TemperatureRecorded(id)
  }
}