package nz.net.catalyst.icons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Interface for external process to transform an image
 */
public interface ImageConverter {
   
   public static final String SVG = "SVG";
   public static final String PNG = "PNG";
   public static final String CROP_LEFT = "left";
   public static final String CROP_CENTER = "center";
   public static final String CROP_RIGHT = "right";

   /**
    * Scale an image
    * <p>
    * The new image will retain it's original ratio
    * </p>
    * @param origImage original File
    * @param newImage File to save transformed image to
    * @param newWidth the width the new image should have
    * @return ImageInfo
    * @throws TimeoutException
    * @throws FileNotFoundException
    * @throws IOException
    */
   public ImageInfo scale(File origImage, File newImage, int newWidth) 
      throws TimeoutException, FileNotFoundException, IOException;

   /**
    * Crop an image
    * <p>
    * The new image will retain it's original height
    * </p>
    * @param origImage original File
    * @param newImage File to save transformed image to
    * @param newWidth the width the new image should have
    * @param cropSide 
    *             "left" for crop from left, "center" for crop from left and right, "right" for crop
    *             from right
    * @return ImageInfo
    * @throws TimeoutException
    * @throws FileNotFoundException
    * @throws IOException
    */
   public ImageInfo crop(File origImage, File newImage, int newWidth, String cropSide) 
      throws TimeoutException, FileNotFoundException, IOException;
   
   /**
    * This method will retrieve the information needed to populate a ImageInfo object:
    * <ul>
    * <li>path
    * <li>type - (E.g. SVG, PNG, etc.)
    * <li>width
    * <li>height
    * <li>lastModified
    * </ul>
    * @param imageFile The file to retrieve the information from
    * @return
    * @throws TimeoutException
    * @throws FileNotFoundException
    * @throws IOException
    * 
    * @see ImageInfo
    */
   public ImageInfo getImageInfo(File imageFile) 
      throws TimeoutException, FileNotFoundException, IOException;
}