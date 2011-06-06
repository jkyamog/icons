package nz.net.catalyst.icons;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface ProcExec {

   /**
     * Time in seconds before the executed command gets interrupted
     */
   public static final long DEFAULT_TIMEOUT = 1000l * 20; // 20 secs

   /**
     * max number of processes to limit the use of concurrent process and
     * lessen the OS resources (file handle, etc.)
     */
   public static final int DEFAULT_PROCESS_MAX = 20;

   public ExecResult execute(List<String> commandList) throws TimeoutException, ExecutionException;
   
}
