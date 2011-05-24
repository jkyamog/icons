package nz.net.catalyst.icons

import akka.actor._
import collection.JavaConversions._


object ProcessActorTest {
  
   def main(args: Array[String]) {

     val command = Seq("ps", "ax")

     val procActor = Actor.actorOf[ProcessActor]
     
     procActor.start
     val test = procActor !! command
     
     println(test)
     
     //procActor.stop
     
   }
}