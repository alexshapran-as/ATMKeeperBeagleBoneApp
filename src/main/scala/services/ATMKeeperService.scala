package services

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, ActorSystem, Identify, PoisonPill, Props}
import akka.util.Timeout
import akka.pattern._
import com.typesafe.config._
import authenticator.Authenticator._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ATMKeeperService {

  private case class Command(cmd: String, signature: String)
  private case class StartService()
  private case class Init(remote: ActorRef)
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
              hostname = "localhost"
              port = $port
            }
          }
        }
    """) // "192.168.7.2"

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class BeagleBoneService extends Actor {

    var remoteActorBank: ActorRef = _
    var remoteActorDispenser: ActorRef = _

    override def receive: Receive = {
      case StartService() =>
        // Identify Bank System
        val bankSystem = "akka.tcp://BankSystem@localhost:24321" // 192.168.0.161
        val bankPath = "/user/bank"
        val bankUrl = bankSystem + bankPath
        val bankSelection: ActorSelection = context.actorSelection(bankUrl)
        bankSelection ! Identify("bank")
        // Identify Dispenser System
        val dispenserSystem = "akka.tcp://DispenserSystem@localhost:24325"
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
        bankRemote ! s"Connection established with ${self.path}"
        bankRemote ! Init(self)
        remoteActorBank = bankRemote

      case InitDispenserSystem(dispenserRemote: ActorRef) =>
        dispenserRemote ! s"Connection established with ${self.path}"
        dispenserRemote ! Init(self)
        remoteActorDispenser = dispenserRemote

      case msg: String =>
        println("got message from bank: " + msg)

      case Command(cmd, signature) if cmd == "Block BBB" =>
        val realBankSender: ActorRef = sender
        println(s"BeagleBone Recieved COMMAND: ${cmd}")
        checkSignatures(signature, cmd) match {
          case Right(_) =>
            realBankSender ! Right(("BEAGLEBONE: * Blocked", ""))
            self ! PoisonPill
          case Left(errMsg) =>
            realBankSender ! Left(errMsg)
        }

      case Command(cmd, signature) =>
        val realBankSender: ActorRef = sender
        println(s"BeagleBone Recieved COMMAND: ${cmd}")
        checkSignatures(signature, cmd) match {
          case Right(beagleBoneMsg) =>
            implicit val timeout: Timeout = Timeout(10.seconds)
            val response: Future[Either[String, String]] = (remoteActorDispenser ? Command(cmd, signature)).mapTo[Either[String, String]]
            response map {
              case Right(dispenserMsg) =>
                realBankSender ! Right((beagleBoneMsg, dispenserMsg))
              case Left(errMsg) =>
                realBankSender ! Left(errMsg)
            }
          case Left(errMsg) =>
            realBankSender ! Left(errMsg)
        }
    }
  }

  case class BeagleBone() {
    val system: ActorSystem = remotingSystem("BeagleBoneSystem", 24567)
    val localActorBBB: ActorRef = system.actorOf(Props[BeagleBoneService], "beaglebone")
    def startBBBService(): Unit = localActorBBB ! StartService()
  }

}
