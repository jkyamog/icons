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
      String command = StringUtils.join(commandList.iterator(), " ");
      logger.debug("command = " + command);

      ExecResult execResult = null;
      int exitStatus = 0;
      try {
         ProcessBuilder procBuilder= new ProcessBuilder(commandList);
         proc = procBuilder.start();

         exitStatus = proc.waitFor(); // blocking we want to wait for the process to finish

      } catch (InterruptedException e) {
         logger.error("Process got interrupted.", e);
         // Stop the process from running
         halt();
      }
      
      try {
         String output = IOUtils.toString(proc.getInputStream());
         String error = IOUtils.toString(proc.getErrorStream());

         execResult = new ExecResult(exitStatus, output, error);
      } catch (IOException e) {
         logger.error("error getting process output and error", e);
      }

      if (execResult.getExitStatus() != 0) {
         logger.warn("Error executing command: " + command + " STDERR follows");
         logger.warn(execResult.getError());
      }
      
      return execResult;
      
   }
   
   /**
    * use this to kill the process if needed during an exception
    */
   public void halt() {
      if (proc != null) {
         proc.destroy();
      }
   }

}
