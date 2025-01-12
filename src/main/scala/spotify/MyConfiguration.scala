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

    // Display available interfaces to the user
    interfaces.zipWithIndex.foreach { case (address, index) =>
      println(s"$index: ${address.getHostAddress}")
    }

    println("Select network interface:")
    val selection = scala.io.StdIn.readInt()
    localAddress = Some(interfaces(selection))

    println("Enter port to bind (default: 2552):")
    val port = scala.io.StdIn.readLine().trim match {
      case "" => 2552 // Default port
      case p  => p.toInt
    }

    val isLocalOnly = scala.io.StdIn.readLine("Run locally only? (y/n): ").equalsIgnoreCase("y")
    runLocalOnly = Some(isLocalOnly)

    val externalHost = if (isLocalOnly) "127.0.0.1" else {
      println("Enter public domain or external IP:")
      scala.io.StdIn.readLine().trim
    }

    val config = applyConfig(externalHost, localAddress.get.getHostAddress, port.toString)

    // Log the generated configuration
    println(s"Generated Configuration: \n${config.root().render()}")
    config
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
         |    # Hardcoding seed nodes for consistency with the server
         |    seed-nodes = [
         |      "akka://SpotifyServer@192.168.0.3:2551"
         |    ]
         |    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
         |  }
         |}
         """.stripMargin
    )
  }
}
