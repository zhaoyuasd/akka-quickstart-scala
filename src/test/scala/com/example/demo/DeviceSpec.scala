package com.example.demo

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DeviceSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("DeviceSpec"))
  override def afterAll: Unit = {
    shutdown(system)
  }

  "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.ReadTemperature(resquestId = 42), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.resquestId  should ===(42L)
    response.value should ===(None)
  }

  "reply with lastes temperature reading " in{
    val probe =TestProbe()
    val deviceActor=system.actorOf(Device.props("group","device"))

    deviceActor.tell(Device.RecordTemperature(1,24.0),probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(resquestId = 1))


    deviceActor.tell(Device.ReadTemperature(resquestId = 2),probe.ref)

    val response1=probe.expectMsgType[Device.RespondTemperature]

    response1.resquestId should ===(2L)
    response1.value should === (Some(24.0))

    deviceActor.tell(Device.RecordTemperature(3,55.0),probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(resquestId = 3))

    deviceActor tell(Device.ReadTemperature(4),probe.ref)
    val response2=probe.expectMsgType[Device.RespondTemperature]

    response2.resquestId should === (4L)

    response2.value should ===(Some(55.0))
  }

}
