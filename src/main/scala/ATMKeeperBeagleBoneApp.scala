import services.ATMKeeperService.BeagleBone

object ATMKeeperBeagleBoneApp {

  def main(args: Array[String]): Unit = {

    val beagleBone: BeagleBone = BeagleBone()
    beagleBone.startBBBService()

  }

}
