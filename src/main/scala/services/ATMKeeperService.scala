package services

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, ActorSystem, Identify, PoisonPill, Props}
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.security._
import cryptographer.RSA

object ATMKeeperService {

  private case class Command(cmd: String)
  private case class StartService()
  private case class InitBBBForDispenser(remote: ActorRef)
  private case class InitBBBForBank(remote: ActorRef)
  private case class InitBBBPubKey(realSender: ActorRef, publicKey: PublicKey)
  private case class InitBankPubKey(realSender: ActorRef, publicKey: PublicKey)
  private case class InitBankSystem(remote: ActorRef)
  private case class InitDispenserSystem(remote: ActorRef)

  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
        akka {
          actor.warn-about-java-serializer-usage = off
          actor.provider = "akka.remote.RemoteActorRefProvider"
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "192.168.7.2"
              port = $port
            }
          }
        }
    """) // "192.168.7.2"

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class BeagleBoneService extends Actor {

    var remoteActorBank: ActorRef = null
    var remoteActorDispenser: ActorRef = null
    var remoteBankPublicKey: PublicKey = null

    override def receive: Receive = {
      case StartService() =>
        // Identify Bank System
        val bankSystem = "akka.tcp://BankSystem@192.168.43.42:24321" // 10.50.1.61 BMSTU
        val bankPath = "/user/bank"
        val bankUrl = bankSystem + bankPath
        val bankSelection: ActorSelection = context.actorSelection(bankUrl)
        bankSelection ! Identify("bank")
        // Identify Dispenser System
        val dispenserSystem = "akka.tcp://DispenserSystem@192.168.43.42:24325" // 10.50.1.61 BMSTU
        val dispenserPath = "/user/dispenser"
        val dispenserUrl = dispenserSystem + dispenserPath
        val dispenserSelection = context.actorSelection(dispenserUrl)
        dispenserSelection ! Identify("dispenser")

      case ActorIdentity("bank", Some(ref)) => self ! InitBankSystem(ref)

      case ActorIdentity("dispenser", Some(ref)) => self ! InitDispenserSystem(ref)

      case ActorIdentity(correlationId, None) =>
        println(s"Something’s wrong: Perhaps the $correlationId system is not working!")
        context.stop(self)

      case InitBankSystem(bankRemote: ActorRef) =>
        remoteActorBank = bankRemote
        remoteActorBank ! s"Connection established with ${self.path}"
        remoteActorBank ! InitBBBForBank(self)

      case InitDispenserSystem(dispenserRemote: ActorRef) =>
        dispenserRemote ! s"Connection established with ${self.path}"
        dispenserRemote ! InitBBBForDispenser(self)
        remoteActorDispenser = dispenserRemote

      case InitBBBPubKey(realSender: ActorRef, selfPublicKey: PublicKey) =>
        remoteActorBank ! InitBBBPubKey(realSender, selfPublicKey)

      case InitBankPubKey(realSender: ActorRef, remotePublicKey: PublicKey) =>
        remoteBankPublicKey = remotePublicKey
        realSender ! Right((RSA.encrypt("БАНКОМАТ - BEAGLE BONE: Ключи загружены", remoteBankPublicKey), None))

      case msg: String =>
        println(msg)

      case command: Array[Byte] =>
        val realBankSender: ActorRef = sender
        val uploadKeysCommand = new String(command)
        if (uploadKeysCommand == "Command(Загрузить ключи)" || uploadKeysCommand == "Command(Обновить ключи)") {
          println(s"BeagleBone recieved COMMAND: ${uploadKeysCommand}")
          self ! InitBBBPubKey(realBankSender, RSA.getSelfPublicKey)
        } else {
          RSA.decrypt(command) match {
            case cmd: String if cmd == "Command(Сбросить ключи)" =>
              remoteBankPublicKey = null
              RSA.resetKeys
              realBankSender ! Right("БАНКОМАТ - BEAGLE BONE: Ключи сброшены".getBytes(), None)

            case cmd: String if cmd == "Command(Сообщить состояние устройств)" =>
              println(s"BeagleBone Recieved COMMAND: ${cmd}")
              implicit val timeout: Timeout = Timeout(1000.seconds)
              val response: Future[Either[String, String]] = (remoteActorDispenser ? cmd).mapTo[Either[String, String]]
              response map {
                case Right(dispenserMsg) =>
                  realBankSender ! Right((RSA.encrypt("BEAGLE BONE: состояние стабильно", remoteBankPublicKey),
                    Some(RSA.encrypt(dispenserMsg, remoteBankPublicKey))))
                case Left(errMsg) =>
                  realBankSender ! Left(RSA.encrypt(errMsg, remoteBankPublicKey))
              }

            case cmd: String if cmd == "Command(Сообщить состояние диспенсера)" ||
                                cmd == "Command(Заблокировать диспенсер)" ||
                                cmd == "Command(Снять блокировку диспенсера)" ||
                                cmd == "Command(Инкассация банкомата)" ||
                                cmd == "Command(Тест контроллера ББ)" ||
                                cmd == "Command(Тест датчиков КББ)" ||
                                cmd == "Command(Тест Д)" ||
                                cmd == "Command(Отключить КББ)" =>
              println(s"BeagleBone Recieved COMMAND: ${cmd}")
              implicit val timeout: Timeout = Timeout(10.seconds)
              val response: Future[Either[String, String]] = (remoteActorDispenser ? cmd).mapTo[Either[String, String]]
              response map {
                case Right(dispenserMsg) =>
                  realBankSender ! Right((RSA.encrypt(s"BEAGLE BONE: получена команда: $cmd", remoteBankPublicKey),
                    Some(RSA.encrypt(dispenserMsg, remoteBankPublicKey))))
                case Left(errMsg) =>
                  realBankSender ! Left(RSA.encrypt(errMsg, remoteBankPublicKey))
              }

            case invalid =>
              println(s"BeagleBone recieved Invalid COMMAND: ${invalid}")
              Left(RSA.encrypt("Недоступная команда", remoteBankPublicKey))
          }
        }
    }
  }

  case class BeagleBone() {
    val system: ActorSystem = remotingSystem("BeagleBoneSystem", 24567)
    val localActorBBB: ActorRef = system.actorOf(Props[BeagleBoneService], "beaglebone")
    def startBBBService(): Unit = localActorBBB ! StartService()
  }

}
