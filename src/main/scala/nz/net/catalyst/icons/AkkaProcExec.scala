package nz.net.catalyst.icons

import akka.actor.Actor

class AkkaProcExec extends ProcExec {

  override def execute(commandList: java.util.List[String]): ExecResult = {

     val procActor = Actor.actorOf[ProcessActor]
     
     procActor.start
     val result = procActor !! commandList
     
     result match {
       case Some(execResult: ExecResult) => return execResult
       case _ => throw new ClassCastException
     }

  }
}