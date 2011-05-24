package nz.net.catalyst.icons

import akka._
import actor.Actor
import event.EventHandler
import routing.Routing._
import routing.CyclicIterator

import scala.collection.JavaConversions._

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class AkkaProcExec(noOfActors: Integer) extends ProcExec {
  
  val actors = for(i <- 1 to noOfActors) yield Actor.actorOf[ProcessActor].start()
  val loadBalancer = loadBalancerActor(new CyclicIterator(actors)) 

  def this() = this(AkkaProcExec.DefaultNoOfActors)

  override def execute(commandList: java.util.List[String]): ExecResult = {

    val commandSeq: Seq[String] = commandList

    val result = loadBalancer !! (commandSeq, AkkaProcExec.DefaultTimeout)

    result match {
      case Some(execResult: ExecResult) => {
        if (execResult.getExitStatus != 0) {
          EventHandler.warning(this, "Error executing command: " + commandSeq.mkString(" ") + " STDERR follows")
          EventHandler.warning(this, execResult.getError())
        }
        return execResult
      }
      case Some(e: Exception) => throw new ExecutionException("Exception for command: " + commandSeq.mkString(" "), e)
      case None => throw new TimeoutException("Timeout no results for command: " + commandSeq.mkString(" "))
    }

  }
}

object AkkaProcExec {
  val DefaultTimeout = 20 * 1000l; // 20 secs
  val DefaultNoOfActors = 20;
}