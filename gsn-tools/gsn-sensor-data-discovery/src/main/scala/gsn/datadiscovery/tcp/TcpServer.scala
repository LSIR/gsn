package gsn.datadiscovery.tcp

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

class TcpServer extends Actor {
 
  import Tcp._
  import context.system
 
  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 5000))
 
  def receive = {
    case b @ Bound(localAddress) =>
      println("Bound")
 
    case CommandFailed(_: Bind) =>
      context stop self
      println("CommandFailed")
 
    case c @ Connected(remote, local) =>
      val handler = context.actorOf(Props[SimplisticHandler])
      val connection = sender()
      connection ! Register(handler)
      println("Connected (Server)")
  }
 
}