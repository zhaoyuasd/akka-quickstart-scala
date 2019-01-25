package com.example.demo

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class DeviceGroupQuerySpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("DeviceSpec"))
  override def afterAll: Unit = {
    shutdown(system)
  }


  // 设备正常返回
  "return temperature value for working devices" in {
    val requester = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      requestId = 1,
      requester = requester.ref,
      timeout = 3.seconds
    ))

    device1.expectMsg(Device.ReadTemperature(resquestId = 0))
    device2.expectMsg(Device.ReadTemperature(resquestId = 0))

    queryActor.tell(Device.RespondTemperature(resquestId = 0, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(resquestId = 0, Some(2.0)), device2.ref)

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.Temperature(2.0)
      )
    ))
  }


  // 部分设备没有值返回
  "return TemperatureNotAvailable for devices with no readings" in {
    val requester = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      requestId = 1,
      requester = requester.ref,
      timeout = 3.seconds
    ))

    device1.expectMsg(Device.ReadTemperature(resquestId = 0))
    device2.expectMsg(Device.ReadTemperature(resquestId = 0))

    queryActor.tell(Device.RespondTemperature(resquestId = 0, None), device1.ref)
    queryActor.tell(Device.RespondTemperature(resquestId = 0, Some(2.0)), device2.ref)

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.TemperatureNotAvailable,
        "device2" -> DeviceGroup.Temperature(2.0)
      )
    ))
  }


  //部分设备在返回前结束
  "return DeviceNotAvailable if device stops before answering" in {
    val requester = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      requestId = 1,
      requester = requester.ref,
      timeout = 3.seconds
    ))

    device1.expectMsg(Device.ReadTemperature(resquestId = 0))
    device2.expectMsg(Device.ReadTemperature(resquestId = 0))

    queryActor.tell(Device.RespondTemperature(resquestId = 0, Some(1.0)), device1.ref)
    device2.ref ! PoisonPill

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.DeviceNotAvailable
      )
    ))
  }


// 超时
  "return DeviceTimedOut if device does not answer in time" in {
    val requester = TestProbe()

    val device1 = TestProbe()
    val device2 = TestProbe()

    val queryActor = system.actorOf(DeviceGroupQuery.props(
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      requestId = 1,
      requester = requester.ref,
      timeout = 1.second
    ))

    device1.expectMsg(Device.ReadTemperature(resquestId=0))
    device2.expectMsg(Device.ReadTemperature( 0))

    queryActor.tell(Device.RespondTemperature(resquestId = 0, Some(1.0)), device1.ref)

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.DeviceTimedOut
      )
    ))
  }

}
