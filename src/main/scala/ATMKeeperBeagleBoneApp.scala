import services.ATMKeeperService.BeagleBone
import usb.utils.UsbUtils

object ATMKeeperBeagleBoneApp {
  def main(args: Array[String]): Unit = {
    UsbUtils().printUsbDevices
    val beagleBone: BeagleBone = BeagleBone()
    beagleBone.startBBBService()
  }
}
