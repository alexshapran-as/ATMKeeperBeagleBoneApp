import services.ATMKeeperService.BeagleBone
import net._
import scala.collection.JavaConversions._


object ATMKeeperBeagleBoneApp {
  def main(args: Array[String]): Unit = {
    val network = new Network(0, new Example, 255)
    val ports: List[String] = network.getPortList.toList
    network.connect(ports.head, Example.speed)
//    val beagleBone: BeagleBone = BeagleBone()
//    beagleBone.startBBBService()
  }
}
