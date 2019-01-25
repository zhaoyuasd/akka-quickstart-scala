package com.example.demo

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.example.demo.DeviceManager.RequestTrackDevice
import scala.concurrent.duration._
// 设备组
object DeviceGroup {
def props(groupId:String):Props=Props(new DeviceGroup((groupId)))

  final case class RequestDeviceList(requestId:Long)
  final case class ReplyDeviceList(requestId:Long,ids:Set[String])

  final case class RequestAllTemperatures(requestId:Long)
  final case class RespondAllTemperatures(requestId:Long,temperatures:Map[String,TemperatureReading])

  sealed trait TemperatureReading
  final case class Temperature(value:Double) extends TemperatureReading
  final case object TemperatureNotAvailable extends TemperatureReading
  final case object DeviceNotAvailable extends TemperatureReading
  final case object DeviceTimedOut extends TemperatureReading
}

class DeviceGroup(groupId:String) extends  Actor with ActorLogging{
  import DeviceGroup. _
  var deviceToActor= Map.empty[String,ActorRef]
  var actorToDeviceId=Map.empty[ActorRef,String]

  override def preStart(): Unit = log.info("DeviceGroup {} started")

  override def postStop(): Unit = log.info("DeviceGroup {} stopped",groupId)

  override def receive: Receive = {
    case trackMsg @ RequestTrackDevice(this.groupId,_) =>
      deviceToActor.get(trackMsg.groupId) match {
        case Some(deviceActor)=>
            deviceActor forward trackMsg
        case None=>
          log.info("create device actor  for {} at group {}",trackMsg.deviceId,trackMsg.groupId)
          val deviceActor=context.actorOf(Device.props(trackMsg.groupId,trackMsg.deviceId),s"device-${trackMsg.deviceId}")
          context watch(deviceActor)
          actorToDeviceId += deviceActor->trackMsg.deviceId
          deviceToActor +=  trackMsg.deviceId->deviceActor
          deviceActor forward trackMsg
      }
    case RequestTrackDevice(groupId,deviceId)=>
      log.warning(" ignoring TrackDevice request for {} . this is responseible for {}",groupId,this.groupId)

    case RequestDeviceList(requestId) =>
      sender() ! ReplyDeviceList(requestId,deviceToActor.keySet)

    case Terminated(deviceActor)=>
      val deviceId=actorToDeviceId(deviceActor)
      log.info("device actor for {} has been terminated ",deviceId)
      actorToDeviceId -=deviceActor
      deviceToActor -=deviceId

      //查询
    case RequestAllTemperatures(requestId) ⇒
      context.actorOf(DeviceGroupQuery.props(
        actorToDeviceId = actorToDeviceId,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }


}

