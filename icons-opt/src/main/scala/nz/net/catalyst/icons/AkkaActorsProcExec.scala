package nz.net.catalyst.icons

import akka._
import actor.Actor
import event.EventHandler
import routing.Routing._
import routing.CyclicIterator

import scala.collection.JavaConversions._

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

/**
 * This implementation uses Actors and loadbalancers.  This is not a good case of using actors, as its a blocking
 * implementation which is similar to the ExecutorServiceProcExecImpl.  Actors should normally be none blocking.
 * This implementation is also not complete, as when a process gets an exception or timesout it never forcefully
 * destroys the process.  There is potential resource leak if processes are not executing properly.
 *
 * @author jun yamog
 */
class AkkaActorsProcExec(noOfActors: Int) extends ProcExec {
  
  val actors = for(i <- 1 to noOfActors) yield Actor.actorOf[ProcessActor].start()
  val loadBalancer = loadBalancerActor(new CyclicIterator(actors)) 

  def this() = this(ProcExec.DEFAULT_PROCESS_MAX)

  override def execute(commandList: java.util.List[String]): ExecResult = {

    val commandSeq: Seq[String] = commandList

    val result = loadBalancer !! (commandSeq, ProcExec.DEFAULT_TIMEOUT)

    result match {
      case Some(execResult: ExecResult) => {
        if (execResult.getExitStatus != 0) {
          EventHandler.warning(this, "Error executing command: " + commandSeq.mkString(" ") + " STDERR follows")
          EventHandler.warning(this, execResult.getError)
        }
        EventHandler.debug(this, "execResult = " + execResult)
        return execResult
      }
      case Some(e: Exception) => throw new ExecutionException("Exception for command: " + commandSeq.mkString(" "), e)
      case None => throw new TimeoutException("Timeout no results for command: " + commandSeq.mkString(" "))
    }

  }
}

