package nz.net.catalyst.icons

import akka.actor.Actor
import akka.event.EventHandler

import scala.io.Source.fromInputStream
import scala.sys.process.ProcessIO
import scala.sys.process.Process

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

    }
    case _ => EventHandler.warning(this, "received unknown message")
  }

}