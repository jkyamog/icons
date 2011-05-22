package nz.net.catalyst.icons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
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
   
   private final ProcExec procExec;
   
   public ImageMagickImageConverterImpl(ProcExec processExecutor) {
      this.procExec = processExecutor;
   }

   
   public ImageInfo getImageInfo(File imageFile) 
         throws TimeoutException, ExecutionException, FileNotFoundException, IOException {

      logger.info("identifying image " + imageFile.getAbsolutePath());
      List<String> commandList = new ArrayList<String>();
      commandList.add("identify");
      commandList.add(imageFile.getAbsolutePath());

      ImageInfo imageInfo;
      ExecResult execResult = procExec.execute(commandList);
      if ((execResult != null) && (execResult.getExitStatus() == 0)) {
         String[] identifyResults = StringUtils.split(execResult.getOutput(), " ");
         logger.debug("identify output " + execResult.getOutput());
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
   
   public ImageInfo convert(File origImage, File newImage, int newWidth, ImageOperation imageOperation) 
      throws TimeoutException, ExecutionException, FileNotFoundException, IOException {
      
      if (imageOperation.equals(ImageOperation.SCALE))
         return scale(origImage, newImage, newWidth);
      else
         return crop(origImage, newImage, newWidth, imageOperation);
   }



   private ImageInfo scale(File origImage, File newImage, int newWidth)
         throws TimeoutException, ExecutionException, FileNotFoundException, IOException {

      ImageInfo origImageInfo = getImageInfo(origImage);
      checkArguments(origImage, newImage, newWidth);
      createDirIfNeeded(newImage);
      
      List<String> commandList = new ArrayList<String>();
      commandList.add("convert");

      // svg scales better with density param
      if (ImageType.SVG.equals(origImageInfo.getType())) {
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
      ExecResult execResult = procExec.execute(commandList);
      if ((execResult != null) && (execResult.getExitStatus() == 0)) {
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


   private ImageInfo crop(File origImage, File newImage, int newWidth, ImageOperation imageOperation) 
   		throws TimeoutException, ExecutionException, FileNotFoundException, IOException {

      ImageInfo origImageInfo = getImageInfo(origImage);
      checkArguments(origImage, newImage, newWidth);
      createDirIfNeeded(newImage);
      
      int crop = getCrop(origImageInfo, newWidth);
      
      List<String> commandList = new ArrayList<String>();
      
      commandList.add("convert");
      commandList.add(origImage.getAbsolutePath());
      
      if (imageOperation.equals(ImageOperation.CROP_LEFT)) {
         commandList.add("-chop");
         commandList.add(crop + "x0");
      } else if (imageOperation.equals(ImageOperation.CROP_CENTER)) {
         commandList.add("-shave");
         commandList.add(String.valueOf(crop / 2) + "x0");
      } else if (imageOperation.equals(ImageOperation.CROP_RIGHT)) {
         commandList.add("-gravity");
         commandList.add("East");
         commandList.add("-chop");
         commandList.add(crop + "x0");
      }

      commandList.add(newImage.getAbsolutePath());

      ImageInfo newImageInfo;
      ExecResult execResult = procExec.execute(commandList);
      if ((execResult != null) && (execResult.getExitStatus() == 0)) {
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




}
