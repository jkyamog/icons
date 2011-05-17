package nz.net.catalyst.icons;

import java.io.Serializable;

/**
 * a bean that holds image information, this passed around the ImageConverter.
 * This is normally just the return type.  
 * 
 * @author Jun Yamog
 *
 */

public class ImageInfo implements Serializable {

   private static final long serialVersionUID = 1L;
   private final String path;
   private final String type;
   private final int width;
   private final int height;
   private final long lastModified;
   
   public ImageInfo(String path, String type, int width, int height, long lastModified) {
      this.path = path;
      this.type = type.toUpperCase();
      this.width = width;
      this.height = height;
      this.lastModified = lastModified;
   }
   
   public String getPath() {
      return path;
   }
   
   public String getType() {
      return type;
   }

   public int getWidth() {
      return width;
   }
   
   public int getHeight() {
      return height;
   }
   
   public long getLastModified() {
      return lastModified;
   }

   public String toStringForDebug() {
      return new StringBuilder()
         .append(path).append(" ")
         .append(type).append(" ")
         .append(width).append(" ")
         .append(height).append(" ")
         .append(lastModified).append(" ")
         .toString();
   }

}
