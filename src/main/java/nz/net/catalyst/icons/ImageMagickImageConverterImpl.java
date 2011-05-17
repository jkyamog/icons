package nz.net.catalyst.icons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of ImageConverter using ImageMagick
 * 
 * @author jun yamog
 * 
 */
public class ImageMagickImageConverterImpl implements ImageConverter {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   
   private final ExecutorService threadExecutor;
   
   /**
    * Time in seconds before the executed command gets interrupted
    */
   public static final long TIMEOUT = 20;
   
   /**
    * max number of image magick processes - this is set to low number for 2 big reasons:
    * - less use concurrent process, less OS resources (file handle, etc.)
    * - block more concurrent request the better the cache is utilized, as blocked
    * threads will get the cached copy
    */
   public static final int DEFAULT_PROCESS_MAX = 20;
   
   
   
   public ImageMagickImageConverterImpl() {
      threadExecutor = Executors.newFixedThreadPool(DEFAULT_PROCESS_MAX);
   }
   
   /**
    * you can override the maximum number of processes if needed for tuning
    * @param processMax
    */
   public ImageMagickImageConverterImpl(int processMax) {
      threadExecutor = Executors.newFixedThreadPool(processMax);
   }
   
   public void cleanup() {
      threadExecutor.shutdown();
   }

   /* (non-Javadoc)
    * @see nz.co.telecom.mobile.wap.mdl.api.ImageConverter#getImageInfo(java.io.File)
    */
   public ImageInfo getImageInfo(File imageFile) 
         throws TimeoutException, FileNotFoundException, IOException {

      logger.info("identifying image " + imageFile.getAbsolutePath());
      ArrayList<String> commandList = new ArrayList<String>();
      commandList.add("identify");
      commandList.add(imageFile.getAbsolutePath());

      ImageInfo imageInfo;
      ExecResult execResult = execute(commandList);
      if (execResult.getExitStatus() == 0) {
         String[] identifyResults = StringUtils.split(execResult.getStdOutput(), " ");
         logger.debug("identify output " + execResult.getStdOutput());
         if (identifyResults.length > 3) {
            String[] dimensions = StringUtils.split(identifyResults[2], "x");
            imageInfo = new ImageInfo(
                  imageFile.getAbsolutePath(),
                  identifyResults[1],
                  Integer.valueOf(dimensions[0]),
                  Integer.valueOf(dimensions[1]),
                  imageFile.lastModified()
                  );
         } else {
            throw new IOException("failed to identify image "
                  + imageFile.getAbsolutePath());
         }
      } else {
         throw new IOException("failed to identify image "
               + imageFile.getAbsolutePath());
      }
      return imageInfo;
   }

   /* (non-Javadoc)
    * @see nz.co.telecom.mobile.wap.mdl.api.ImageConverter#scale(java.io.File, java.io.File, int)
    */
   public ImageInfo scale(File origImage, File newImage, int newWidth)
         throws TimeoutException, FileNotFoundException, IOException, IllegalArgumentException {

      ImageInfo origImageInfo = getImageInfo(origImage);
      checkArguments(origImage, newImage, newWidth);
      createDirIfNeeded(newImage);
      
      ArrayList<String> commandList = new ArrayList<String>();
      commandList.add("convert");

      // svg scales better with density param
      if (ImageConverter.SVG.equals(origImageInfo.getType())) {
         newWidth = getDensity(newWidth, origImageInfo);
         commandList.add("-density");
         commandList.add(String.valueOf(newWidth));
         commandList.add("-background");
         commandList.add("none");
      } else {
         commandList.add("-scale");
         commandList.add(String.valueOf(newWidth));
         commandList.add("-density"); // this is added for the palm treo
         commandList.add("50.4");
      }

      commandList.add("-quality");
      commandList.add("100");
      commandList.add(origImage.getAbsolutePath());
      commandList.add(newImage.getAbsolutePath());

      ImageInfo newImageInfo;
      ExecResult execResult = execute(commandList);
      if (execResult.getExitStatus() == 0) {
         // get the image info from the actual scaled image
         // this is to make sure that the dimensions matches
         // what is actually in the file system
         newImageInfo = getImageInfo(newImage);
      } else {
         throw new IOException("failed to scale image "
               + origImage.getAbsolutePath());
      }
      return newImageInfo;
   }


   /* (non-Javadoc)
    * @see nz.co.telecom.mobile.wap.mdl.api.ImageConverter#crop(java.io.File, java.io.File, int, java.lang.String)
    */
   public ImageInfo crop(File origImage, File newImage, int newWidth, String cropSide) 
   		throws TimeoutException, FileNotFoundException, IOException, IllegalArgumentException {

      ImageInfo origImageInfo = getImageInfo(origImage);
      checkArguments(origImage, newImage, newWidth, cropSide);
      createDirIfNeeded(newImage);
      
      int crop = getCrop(origImageInfo, newWidth);
      
      ArrayList<String> commandList = new ArrayList<String>();
      
      commandList.add("convert");
      commandList.add(origImage.getAbsolutePath());
      
      if (ImageConverter.CROP_LEFT.equals(cropSide)) {
         commandList.add("-chop");
         commandList.add(crop + "x0");
      } else if (ImageConverter.CROP_CENTER.equals(cropSide)) {
         commandList.add("-shave");
         commandList.add(String.valueOf(crop / 2) + "x0");
      } else if (ImageConverter.CROP_RIGHT.equals(cropSide)) {
         commandList.add("-gravity");
         commandList.add("East");
         commandList.add("-chop");
         commandList.add(crop + "x0");
      }

      commandList.add(newImage.getAbsolutePath());

      ImageInfo newImageInfo;
      ExecResult execResult = execute(commandList);
      if (execResult.getExitStatus() == 0) {
         // get the image info from the actual cropped image
         // this is to make sure that the dimensions matches
         // what is actually in the file system
         newImageInfo = getImageInfo(newImage);
      } else {
         throw new IOException("failed to crop image "
               + origImage.getAbsolutePath());
      }
      return newImageInfo;

   }

   /**
    * @param origImage
    * @param newImage
    * @param newWidth
    * @throws IllegalArgumentException
    * @throws FileNotFoundException 
    */
   private static void checkArguments(File origImage, File newImage,
         int newWidth) throws IllegalArgumentException, FileNotFoundException {
      if (newWidth < 1)
         throw new IllegalArgumentException(
               "invalid newWidth, must be positive");

      if (!origImage.canRead())
         throw new FileNotFoundException(
               "invalid origImage, file could not be read");

      if (StringUtils.isEmpty(newImage.getAbsolutePath()))
         throw new IllegalArgumentException(
               "invalid newImage, path must not be empty");
   }
   
   private static void checkArguments(File origImage, File newImage,
         int newWidth, String cropSide) throws IllegalArgumentException, FileNotFoundException {
      if (newWidth < 1)
         throw new IllegalArgumentException(
               "invalid newWidth, must be positive");

      if (!origImage.canRead())
         throw new FileNotFoundException(
               "invalid origImage, file could not be read");

      if (StringUtils.isEmpty(newImage.getAbsolutePath()))
         throw new IllegalArgumentException(
               "invalid newImage, path must not be empty");
      
      if (cropSide != null) {
         if (!ImageConverter.CROP_LEFT.equals(cropSide) && ! ImageConverter.CROP_CENTER.equals(cropSide) && !ImageConverter.CROP_RIGHT.equals(cropSide))
                throw new IllegalArgumentException(
                      "The crop command can only be called with: "
                            + ImageConverter.CROP_RIGHT + " or "
                            + ImageConverter.CROP_CENTER + " or "
                            + ImageConverter.CROP_LEFT);
      } else 
         throw new IllegalArgumentException(
               "The crop command can only be called with: "
                     + ImageConverter.CROP_RIGHT + " or "
                     + ImageConverter.CROP_CENTER + " or "
                     + ImageConverter.CROP_LEFT);
   }
   
   /**
    * <p>
    * Helper to calculate the value of the density parameter based on:
    * </p>
    * <p>
    * The default output resolution is 72DPI so "-density 144" results in an
    * image twice as large as the default.
    * </p>
    * 
    * <p>
    * get the original width; compare to new width; calculate density based on:
    * </p>
    * 72 -> originalWidth; x -> newWidth
    * 
    * @param newWidth
    * @param path
    * @return
    */
   private int getDensity(int newWidth, ImageInfo imageInfo) {
      int density = (72 * newWidth / imageInfo.getWidth());
      return Math.round(density);
   }

   /**
    * Helper to calculate the amount that needs to be cropped:
    * <p>
    * max_image_width - new_width
    * </p>
    * 
    * @param percentage
    * @param max_image_width
    * @param oldWidth
    * @return
    */
   private int getCrop(ImageInfo origImageInfo, int newWidth) {
      if (origImageInfo.getWidth() > newWidth) {
         /* The original image width is bigger than the new desired width */
         return origImageInfo.getWidth() - newWidth;
      }
      return 0; // If the image is already smaller than the space we want, do nothing
   }
   
   private void createDirIfNeeded(File newImage) throws IOException {
      File directory = new File(newImage.getParent());
      if (!directory.canRead()) {
         logger.debug("creating new directory " + directory.getAbsolutePath());
         FileUtils.forceMkdir(directory);
      }
   }



   /**
    * execute the commandList using a thread pool which is used as a process pool.
    * threads are blocked for executing more processes there is no more slots in the pool
    * note: anything below this gets a bit hairy and don't touch the code unless
    * you understand about concurrency.
    * 
    * @param commandList the ArrayList with commands to execute
    * @return ExecResult
    * @throws TimeoutException
    * @throws IOException
    */
   private ExecResult execute(ArrayList<String> commandList)
         throws TimeoutException, IOException {

      CommandTask commandTask = new CommandTask(commandList);
      Future<?> future = threadExecutor.submit(commandTask);
      try {
         future.get(TIMEOUT, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         logger.error("Process got interrupted.", e);
         // Stop the process from running
         commandTask.halt();
         throw new TimeoutException("Process got interrupted.");
      } catch (ExecutionException e) {
         logger.error("Problem in running process.", e);
         // Stop the process from running
         commandTask.halt();
         throw new IOException(e);
      }
      
      return commandTask.getExecResult();
      
   }

   /**
    * A runnable that runs a Process through a ProcessBuilder.  This holds 
    * ExecResults which is populated after run
    *
    */
   private class CommandTask implements Runnable {
      
      ArrayList<String> commandList;
      ExecResult execResult = new ExecResult();
      Process proc;

      public CommandTask(ArrayList<String> commandList) {
         this.commandList = commandList;
      }
      
      
      public void run() {

         String command = StringUtils.join(commandList.iterator(), " ");
         logger.debug("command = " + command);
   
         try {
            ProcessBuilder procBuilder= new ProcessBuilder(commandList);
            proc = procBuilder.start();

            int exitStatus = proc.waitFor(); // blocking we want to wait for the process to finish
            execResult.setExitStatus(exitStatus);
            execResult.setStdOutput(proc.getInputStream());
            execResult.setStdError(proc.getErrorStream());
         } catch (InterruptedException e) {
            logger.error("Process got interrupted.", e);
            // Stop the process from running
            halt();
         } catch (IOException e) {
            logger.error("error excecuting process", e);
         }


         if (execResult.getExitStatus() != 0) {
            logger.warn("Error executing command: " + command + " STDERR follows");
            logger.warn(execResult.getStdError());
         }
         
      }
      
      public ExecResult getExecResult() {
         return execResult;
      }
      
      /**
       * use this to kill the process if needed during an exception
       */
      public void halt() {
         proc.destroy();
      }
      
   }
   
   /**
    * Class to hold the result of an executed process capturing 
    * the stdout and stderr output as well as the exit status.
    */
   private class ExecResult {
      private int exitStatus = 1;

      private String stdOutput = "";

      private String stdError = "";

      public void setExitStatus(int exitStatus) {
         this.exitStatus = exitStatus;
      }

      public int getExitStatus() {
         return exitStatus;
      }

      public void setStdOutput(InputStream stdOutput) {
         try {
            this.stdOutput = IOUtils.toString(stdOutput);
         } catch (IOException e) {
            logger.error("error processing stdOutput", e);
         }
      }

      public String getStdOutput() {
         return stdOutput;
      }

      public void setStdError(InputStream stdError) {
         try {
            this.stdError = IOUtils.toString(stdError);
         } catch (IOException e) {
            logger.error("error processing stderr", e);
         }
      }

      public String getStdError() {
         return stdError;
      }

   }

}
