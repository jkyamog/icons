package nz.net.catalyst.icons

import akka.actor._
import collection.JavaConversions._
import org.junit.Test


class ProcessActorTest {
  
  @Test
  def testProcessActor {

     val command = Seq("ps", "ax")

     val procActor = Actor.actorOf[ProcessActor]
     
     procActor.start
     val test = procActor !! command
     
     println(test)
     
     //procActor.stop
     
  }
}