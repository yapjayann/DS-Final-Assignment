package spotify


import com.typesafe.config.{Config, ConfigFactory}
import java.net.{InetAddress, NetworkInterface}
import scala.collection.JavaConverters._

object MyConfiguration {
  var localAddress: Option[InetAddress] = None
  var runLocalOnly: Option[Boolean] = None

  def askForConfig(): Config = {
    val interfaces = (for {
      inf <- NetworkInterface.getNetworkInterfaces.asScala
      addr <- inf.getInetAddresses.asScala
    } yield addr).toList

    interfaces.zipWithIndex.foreach { case (address, index) =>
      println(s"$index: ${address.getHostAddress}")
    }

    println("Select network interface:")
    val selection = scala.io.StdIn.readInt()
    localAddress = Some(interfaces(selection))

    println("Enter port to bind:")
    val port = scala.io.StdIn.readInt()

    val isLocalOnly = scala.io.StdIn.readLine("Run locally only? (y/n): ").equalsIgnoreCase("y")
    runLocalOnly = Some(isLocalOnly)

    val externalHost = if (isLocalOnly) "127.0.0.1" else scala.io.StdIn.readLine("Enter public domain or external IP:")
    applyConfig(externalHost, localAddress.get.getHostAddress, port.toString)
  }

  def applyConfig(extHostName: String, intHostName: String, port: String): Config = {
    ConfigFactory.parseString(
      s"""
         |akka {
         |  loglevel = "INFO"
         |  actor.provider = "cluster"
         |  remote.artery {
         |    transport = tcp
         |    canonical.hostname = "$extHostName"
         |    canonical.port = $port
         |    bind.hostname = "$intHostName"
         |    bind.port = $port
         |  }
         |  cluster {
         |    seed-nodes = [
         |      "akka://SpotifySystem@$extHostName:$port"
         |    ]
         |    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
         |  }
         |}
         """.stripMargin
    )
  }
}

