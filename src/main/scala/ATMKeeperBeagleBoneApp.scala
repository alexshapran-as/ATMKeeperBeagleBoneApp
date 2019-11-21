import javax.usb.UsbDevice
import services.ATMKeeperService.BeagleBone

object ATMKeeperBeagleBoneApp {
  def main(args: Array[String]): Unit = {
    val bank: UsbDevice = Usb4Scala.getDevice(0x05e3.toShort, 0x0608.toShort)
//    List(Bus 001 Device 001: ID 1d6b:0002)
//    List(Bus 001 Device 002: ID 05e3:0608)
//    List(Bus 001 Device 003: ID 0cf3:9271)
    println(bank)
    while(true) {
      Usb4Scala.readMessage(Usb4Scala.getDeviceInterface(bank, 0), 0)
    }
    val beagleBone: BeagleBone = BeagleBone()
    beagleBone.startBBBService()
  }
}
