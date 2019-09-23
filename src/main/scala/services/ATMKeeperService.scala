package services

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, ActorSystem, Identify, PoisonPill, Props}
import com.typesafe.config._
import authenticator.Authenticator._

object ATMKeeperService {

  private case class Command(cmd: String, signature: String)
  private case class StartService()
  private case class Init(remote: ActorRef)

  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
        akka {
          actor.warn-about-java-serializer-usage = off
          actor.provider = "akka.remote.RemoteActorRefProvider"
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "172.16.1.203"
              port = $port
            }
          }
        }
    """)

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class BeagleBoneService extends Actor {

    var remoteActorBank: ActorRef = _

    override def receive: Receive = {

      case StartService() =>
        val BankSystem = "akka.tcp://BankSystem@172.16.1.203:24321"
        val BankPath = "/user/bank"
        val url = BankSystem + BankPath
        val bnakSelection: ActorSelection = context.actorSelection(url)
        bnakSelection ! Identify(0)

      case ActorIdentity(0, Some(ref)) => self ! ref

      case ActorIdentity(0, None) =>
        println("Something’s wrong - ain’t no pongy anywhere!")
        context.stop(self)

      case remote: ActorRef =>
        remote ! s"Connection established with ${self.path}"
        remote ! Init(self)
        remoteActorBank = remote

      case msg: String =>
        println("got message from bank: " + msg)

      case Command(cmd, signature) =>
        val realBankSender: ActorRef = sender
        println(s"Recieved COMMAND: ${cmd}")
        checkSignatures(signature, cmd) match {
          case Right(msg) =>
            realBankSender ! Right(msg)
          case Left(errMsg) =>
            realBankSender ! Left(errMsg)
        }

      case "close" => self ! PoisonPill

    }

  }

  case class BeagleBone() {

    val system: ActorSystem = remotingSystem("BeagleBoneSystem", 24567)
    val localActorBBB: ActorRef = system.actorOf(Props[BeagleBoneService], "beaglebone")
    def startBBBService(): Unit = localActorBBB ! StartService()

  }

}
