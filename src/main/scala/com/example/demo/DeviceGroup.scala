package com.example.demo

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.example.demo.DeviceManager.RequestTrackDevice

object DeviceGroup {
def props(groupId:String):Props=Props(new DeviceGroup((groupId)))

  final case class RequstDeviceList(requestId:Long)
  final case class ReplyDeviceList(requestId:Long,ids:Set[String])
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

    case RequstDeviceList(requestId) =>
      sender() ! ReplyDeviceList(requestId,deviceToActor.keySet)

    case Terminated(deviceActor)=>
      val deviceId=actorToDeviceId(deviceActor)
      log.info("device actor for {} has been terminated ",deviceId)
      actorToDeviceId -=deviceActor
      deviceToActor -=deviceId

  }
}

