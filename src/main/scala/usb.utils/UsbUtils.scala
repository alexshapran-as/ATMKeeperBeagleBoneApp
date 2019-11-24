package usb.utils

import java.util

import javax.usb.{UsbDevice, UsbHub, UsbPort}

import scala.collection.JavaConversions._

case class UsbUtils() {
  val usb4java: Usb4JavaHigh = new Usb4JavaHigh()
  def validateBBBUsbConnection(): Boolean = {
    val beagleBoneUsbDevice: UsbDevice = usb4java.findDevice(0x1d6b.toShort, 0x0104.toShort)
    println(beagleBoneUsbDevice)
    true
  }

  def printUsbDevices: Unit = usb4java.printUsbDevices()
}
