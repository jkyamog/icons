package nz.net.catalyst.icons;

/**
 * Class to hold the result of an executed process capturing 
 * the stdout and stderr output as well as the exit status.
 */
public class ExecResult {
   private final Integer exitStatus;

   private final String output;

   private final String error;
   
   public ExecResult(int exitStatus, String output, String error) {
      this.exitStatus = exitStatus;
      this.output = output;
      this.error = error;
   }

   public int getExitStatus() {
      return exitStatus;
   }

   public String getOutput() {
      return output;
   }

   public String getError() {
      return error;
   }
   
   @Override
   public String toString() {
      return (new StringBuilder())
         .append(exitStatus).append(" ")
         .append(output).append(" ")
         .append(error).toString();
   }

}
