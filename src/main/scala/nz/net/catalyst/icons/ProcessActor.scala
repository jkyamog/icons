package nz.net.catalyst.icons

import akka.actor.Actor
import akka.event.EventHandler
import io.Source.fromInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class ProcessActor extends Actor {
  
  def receive = {
    case command: java.util.List[String] => {

      val procBuilder = new ProcessBuilder(command);
      
      val proc = procBuilder.start;
      val exitStatus = proc.waitFor;
      
      val error = fromInputStream(proc.getErrorStream()).mkString
      val output = fromInputStream(proc.getInputStream()).mkString
      
      EventHandler.debug(this, "error = " + error);
      EventHandler.debug(this, "output = " + output);
      
      val result = new ExecResult(exitStatus, output, error)
      
      self.channel ! result
      
    }
    case _ => EventHandler.warning(this, "received unknown message")
  }

}