package usb.utils

import javax.usb.UsbDevice

case class UsbUtils() {
  val usb4java: Usb4JavaHigh = new Usb4JavaHigh()
  def validateBBBUsbConnection(): Boolean = {
    val beagleBoneUsbDevice: UsbDevice = usb4java.findDevice(0x1d6b.toShort, 0x0104.toShort)
    println(beagleBoneUsbDevice)
    true
  }

  def printUsbDevices: Unit = usb4java.printUsbDevices()
}
