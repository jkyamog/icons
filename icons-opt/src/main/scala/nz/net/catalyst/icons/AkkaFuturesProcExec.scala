package nz.net.catalyst.icons

import akka.event.EventHandler
import akka.dispatch.{FutureTimeoutException, Future}
import java.util.concurrent.{TimeoutException, ExecutionException}
import scala.collection.JavaConversions._
import scala.io.Source._

/**
 * This implemetation uses akka's Future.  Its simple and straight forward
 */
class AkkaFuturesProcExec extends ProcExec {

  override def execute(commandList: java.util.List[String]): ExecResult = {
    val commandSeq: Seq[String] = commandList
    var proc: Process = null

    val future = Future[ExecResult] ({
      val procBuilder = new ProcessBuilder(commandList)
      proc = procBuilder.start
      val output: String = fromInputStream(proc.getInputStream).mkString
      val error: String = fromInputStream(proc.getErrorStream).mkString
      val exitStatus = proc.waitFor

      new ExecResult(exitStatus, output, error)
    }, ProcExec.DEFAULT_TIMEOUT) onComplete { f => 
        val execResult = f.get
        if (execResult.getExitStatus != 0) {
          EventHandler.warning(this, "Error executing command: " + commandSeq.mkString(" ") + " STDERR follows")
          EventHandler.warning(this, execResult.getError)
        }
    } onTimeout { _ =>
        if (proc != null) proc.destroy
        throw new TimeoutException("No results for command: " + commandSeq.mkString(" "))
    } failure {
      case e: Exception => {
        if (proc != null) proc.destroy
        throw new ExecutionException("Exception for command: " + commandSeq.mkString(" "), e)
      }
    }

    return future.get
  }

}

