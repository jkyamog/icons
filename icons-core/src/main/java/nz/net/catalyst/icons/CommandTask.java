package nz.net.catalyst.icons;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A callable that runs a Process through a ProcessBuilder.  This returns 
 * the ExecResults
 * 
 * @author jun yamog
 */
public class CommandTask implements Callable<ExecResult> {

   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   private final List<String> commandList;
   private Process proc;

   public CommandTask(List<String> commandList) {
      this.commandList = commandList;
   }
   
   @Override
   public ExecResult call() throws Exception {
      logger.debug("command = " + StringUtils.join(commandList, " "));

      ExecResult execResult = null;
      int exitStatus = 0;
      try {
         ProcessBuilder procBuilder= new ProcessBuilder(commandList);
         proc = procBuilder.start();
         String output = IOUtils.toString(proc.getInputStream());
         String error = IOUtils.toString(proc.getErrorStream());

         exitStatus = proc.waitFor(); // blocking we want to wait for the process to finish

         execResult = new ExecResult(exitStatus, output, error);
      } catch (InterruptedException e) {
         logger.error("Process got interrupted.");
         halt();
         throw e;
      } catch (IOException e) {
         logger.error("Error getting process output and error");
         halt();
         throw e;
      }
      
      return execResult;
      
   }
   
   /**
    * use this to kill the process if needed during an exception
    */
   public void halt() {
      logger.warn("forcing to destroy a process");
      if (proc != null) {
         proc.destroy();
      }
   }

}
