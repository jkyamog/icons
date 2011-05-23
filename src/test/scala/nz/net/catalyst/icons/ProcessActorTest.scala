package nz.net.catalyst.icons

import akka.actor._
import collection.JavaConversions._


object ProcessActorTest {
  
   def main(args: Array[String]) {

     val command = List("ps", "ax")
     val commandList: java.util.List[String] = command
     
     val procActor = Actor.actorOf[ProcessActor]
     
     procActor.start
     val test = procActor !! commandList
     
     println(test)
     
     //procActor.stop
     
   }
}