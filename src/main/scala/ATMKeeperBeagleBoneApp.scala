import javax.usb.UsbDevice
import services.ATMKeeperService.BeagleBone

object ATMKeeperBeagleBoneApp {
  def main(args: Array[String]): Unit = {
    val bank: UsbDevice = Usb4Scala.getDevice(0x1d6b.toShort, 0x0104.toShort)
    println(bank)
    while(true) {
      Usb4Scala.readMessage(Usb4Scala.getDeviceInterface(bank, 0), 0)
    }
    val beagleBone: BeagleBone = BeagleBone()
    beagleBone.startBBBService()
  }
}
