package spotify

import akka.actor.Actor
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.support.igd.PortMappingListener
import org.fourthline.cling.support.model.PortMapping
import java.net.InetAddress

object UpnpManager {
  case class AddPortMapping(port: Int)
}

class UpnpManager extends Actor {
  private val upnpService = new UpnpServiceImpl()

  override def receive: Receive = {
    case UpnpManager.AddPortMapping(port) =>
      try {
        val mappings = Array(
          new PortMapping(port, InetAddress.getLocalHost.getHostAddress, PortMapping.Protocol.TCP, "TCP Mapping"),
          new PortMapping(port, InetAddress.getLocalHost.getHostAddress, PortMapping.Protocol.UDP, "UDP Mapping")
        )
        val listener = new PortMappingListener(mappings)
        upnpService.getRegistry.addListener(listener)
        upnpService.getControlPoint.search()
        context.system.log.info(s"UPnP Port mapping added for port $port.")
      } catch {
        case ex: Exception =>
          context.system.log.error(s"Failed to add UPnP port mapping for port $port: ${ex.getMessage}")
      }
  }
}
