import services.ATMKeeperService.BeagleBone
import usb.utils.UsbUtils
import scala.sys.process._

object ATMKeeperBeagleBoneApp {
  def enableNet(): Boolean = {
    val process1: Process = Process("echo temppwd|sudo -S /sbin/route add default gw 192.168.7.1").run()
    val process2: Process = Process("echo 'nameserver 8.8.8.8' | sudo tee -a /etc/resolv.conf").run()
    (process1.exitValue(), process2.exitValue()) match {
      case (0, 0) => true
      case (_, _) => false
    }
  }

  def main(args: Array[String]): Unit = {
    if (enableNet()) {
      UsbUtils().printUsbDevices
      val beagleBone: BeagleBone = BeagleBone()
      beagleBone.startBBBService()
    } else {
      println("Settings error")
    }
  }
}
