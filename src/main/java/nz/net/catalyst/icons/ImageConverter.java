package nz.net.catalyst.icons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Interface for external process to transform an image
 */
public interface ImageConverter {
   
   public enum ImageType {
      SVG("SVG"), PNG("PNG"), JPEG("JPEG");
      
      private String type;
      
      private ImageType(String type) {
         this.type = type;
      }
      
      public static ImageType getImageType(String imageType) {
         for (ImageType tType : ImageType.values()) {
            if (tType.type.equals(imageType.toUpperCase())) {
              return tType;
            }
         }
         
         throw new IllegalArgumentException("unsupported image type " + imageType);
      }
   }
   
   public enum ImageOperation {
      SCALE, CROP_LEFT, CROP_CENTER, CROP_RIGHT;
   }
   
   /**
    * Crop an image
    * <p>
    * The new image will retain it's original height
    * </p>
    * @param origImage original File
    * @param newImage File to save transformed image to
    * @param newWidth the width the new image should have
    * @param imageOperation kind of convertion will done to the image
    * @return ImageInfo
    * @throws TimeoutException
    * @throws FileNotFoundException
    * @throws IOException
    */
   public ImageInfo convert(File origImage, File newImage, int newWidth, ImageOperation imageOperation) 
      throws TimeoutException, ExecutionException, FileNotFoundException, IOException;
   
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
      throws TimeoutException, ExecutionException, FileNotFoundException, IOException;
}