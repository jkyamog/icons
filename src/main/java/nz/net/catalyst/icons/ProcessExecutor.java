package nz.net.catalyst.icons;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crude way of binding 1 thread to 1 process.  This would mean we can use the
 * thread executor to limit and manage processes
 * 
 * @author jun yamog
 *
 */
public class ProcessExecutor {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());

   private final ExecutorService threadExecutor;

   /**
    * Time in seconds before the executed command gets interrupted
    */
   public static final long TIMEOUT = 20;
   
   /**
    * max number of processes to limit the use of concurrent process and
    * lessen the OS resources (file handle, etc.)
    */
   public static final int DEFAULT_PROCESS_MAX = 20;

   public ProcessExecutor() {
      threadExecutor = Executors.newFixedThreadPool(DEFAULT_PROCESS_MAX);
   }
   
   /**
    * you can override the maximum number of processes if needed for tuning
    * @param processMax
    */
   public ProcessExecutor(int processMax) {
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
    * @throws IOException
    */
   public ExecResult execute(List<String> commandList)
         throws TimeoutException, ExecutionException {

      CommandTask commandTask = new CommandTask(commandList);
      Future<ExecResult> future = threadExecutor.submit(commandTask);
      try {
         return future.get(TIMEOUT, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         logger.error("Process got interrupted.", e);
         // Stop the process from running
         commandTask.halt();
         throw new TimeoutException("Process got interrupted.");
      } catch (ExecutionException e) {
         // Stop the process from running
         commandTask.halt();
         throw e;
      }
   }

   


}
