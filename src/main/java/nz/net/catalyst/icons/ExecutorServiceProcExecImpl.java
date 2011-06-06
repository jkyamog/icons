package nz.net.catalyst.icons;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crude way of binding 1 thread to 1 process.  This would mean we can use the
 * thread executor to limit and manage processes
 * 
 * @author jun yamog
 *
 */
public class ExecutorServiceProcExecImpl implements ProcExec {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   private final ExecutorService threadExecutor;

   public ExecutorServiceProcExecImpl() {
      this(DEFAULT_PROCESS_MAX);
   }
   
   /**
    * you can override the maximum number of processes if needed for tuning
    * @param processMax
    */
   public ExecutorServiceProcExecImpl(int processMax) {
      threadExecutor = Executors.newFixedThreadPool(processMax);
   }
   
   public void cleanup() {
      threadExecutor.shutdown();
   }

   
   /**
    * execute the commandList using a thread pool which is used as a process pool.
    * threads are blocked for executing more processes there is no more slots in the pool
    * 
    * @param commandList the ArrayList with commands to execute
    * @return ExecResult
    * @throws TimeoutException
    */
   @Override
   public ExecResult execute(List<String> commandList)
         throws TimeoutException, ExecutionException {

      CommandTask commandTask = new CommandTask(commandList);
      Future<ExecResult> future = threadExecutor.submit(commandTask);
      try {
         ExecResult execResult = future.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
         
         if (execResult.getExitStatus() != 0) {
            logger.warn("Error executing command: " + StringUtils.join(commandList, " ") + " STDERR follows");
            logger.warn(execResult.getError());
         }
         return execResult;
      } catch (InterruptedException e) {
         logger.error("Process got interrupted.", e);
         throw new TimeoutException("Process got interrupted.");
      } catch (ExecutionException e) {
         // Stop the process from running
         commandTask.halt();
         throw e;
      }
   }

   


}
