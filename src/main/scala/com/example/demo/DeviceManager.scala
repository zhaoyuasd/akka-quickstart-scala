package com.example.demo

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object DeviceManager {
 def props:Props=Props(new DeviceManager)
  final  case class RequestTrackDevice(groupId:String,deviceId:String)
  case object DeviceRegistered
}

class  DeviceManager extends Actor with ActorLogging{
  import DeviceManager._

  var groupIdToActor=Map.empty[String,ActorRef]
  var actorToGroupId=Map.empty[ActorRef,String]

  override def preStart(): Unit = log.info("DeviceManager start")
  override def postStop(): Unit = log.info("DeviceManager stop")

  override def receive: Receive = {
    case trackMsg @ RequestTrackDevice(groupId,_)=>
      groupIdToActor.get(groupId) match {
        case Some(ref)=>
            ref forward trackMsg
        case None=>
            log.info("Creating device group actor for {}",groupId)
            val groupActor=context.actorOf(DeviceGroup.props(groupId),"group-"+groupId)
            context watch(groupActor)
            groupActor forward trackMsg
            groupIdToActor += groupId-> groupActor
            actorToGroupId += groupActor->groupId
      }

    case Terminated(groupActor)=>
       val groupId=actorToGroupId(groupActor)
       log.info("Device group actor for {} has been terminated ",groupId)
      actorToGroupId -=groupActor
      groupIdToActor -=groupId
  }

}

