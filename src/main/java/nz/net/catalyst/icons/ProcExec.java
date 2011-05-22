package nz.net.catalyst.icons;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface ProcExec {
   
   public ExecResult execute(List<String> commandList) throws TimeoutException, ExecutionException;
   
}
