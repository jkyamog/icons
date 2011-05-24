package nz.net.catalyst.icons

import akka.actor.Actor
import akka.event.EventHandler

import scala.io.Source.fromInputStream
import scala.sys.process.ProcessIO
import scala.sys.process.Process

import java.io.InputStreamReader
import java.io.IOException

class ProcessActor extends Actor {
  
  def receive = {
    case command: Seq[String] => {

      try {
        var output = ""
        var error = ""
        val pio = new ProcessIO(_ => (),
          stdout => output = fromInputStream(stdout).mkString,
          stderr => error = fromInputStream(stderr).mkString)

        val procBuilder = Process(command)
        val proc = procBuilder.run(pio)
        val exitStatus = proc.exitValue

        val result = new ExecResult(exitStatus, output, error)

        EventHandler.debug(this, "result = " + result);
        self.channel ! result
      } catch {
        case e: Exception => {
          self.channel ! e
        }
      }

/*      
      try {
        
         val procBuilder = new ProcessBuilder(command);
         
         val proc = procBuilder.start;
         val exitStatus = proc.waitFor;
         
         val error = fromInputStream(proc.getErrorStream()).mkString
         val output = fromInputStream(proc.getInputStream()).mkString
         
         EventHandler.debug(this, "error = " + error);
         EventHandler.debug(this, "output = " + output);
         
         val result = new ExecResult(exitStatus, output, error)
         
         self.channel ! result
      } catch {
        case e: InterruptedException => {
          EventHandler.error(e, this, "Process got interrupted.")
          self.channel ! None
        }
        case e: IOException => {
          EventHandler.error(e, this, "Error getting process output and error")
          self.channel ! None
        }
      }
*/      
      
    }
    case _ => EventHandler.warning(this, "received unknown message")
  }

}