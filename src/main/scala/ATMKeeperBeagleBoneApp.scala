import services.ATMKeeperService.BeagleBone
import usb.utils.UsbUtils
import scala.sys.process._

object ATMKeeperBeagleBoneApp {
  def enableNet(): Unit = {
    val process1: Process = Process("echo temppwd|sudo -S /sbin/route add default gw 192.168.7.1").run()
    println(process1.exitValue())
    val process2: Process = Process("echo 'nameserver 8.8.8.8' | sudo tee -a /etc/resolv.conf").run()
    println(process2.exitValue())
  }

  def main(args: Array[String]): Unit = {
    enableNet()
    UsbUtils().printUsbDevices
    val beagleBone: BeagleBone = BeagleBone()
    beagleBone.startBBBService()
  }
}
