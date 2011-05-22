package nz.net.catalyst.icons;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import nz.net.catalyst.icons.ImageConverter.ImageOperation;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ImageMagickImageConverterImplTest {

   ImageMagickImageConverterImpl icon;
   ProcessExecutor procExec;
   File testDir = new File("target/test-images");
   
   @Before
   public void setup() {
      procExec = new ProcessExecutor();
      icon = new ImageMagickImageConverterImpl(procExec);
      
      testDir.mkdir();
   }
   
   @After
   public void cleanup() throws IOException {
      procExec.cleanup();

      FileUtils.deleteDirectory(testDir);
}
   
   @Test
   public void scaleImage() {
      File origImage = new File("src/test/resources/Catalyst-IT_Logo.svg");
      File newImage = new File("target/test-images/catalyst-log.png");
      
      try {
         icon.convert(origImage, newImage, 200, ImageOperation.SCALE);

         ImageInfo ii = icon.getImageInfo(newImage);
         System.out.println("ImageInfo = " + ii);
         
         BufferedImage bi = ImageIO.read(newImage);
         System.out.println("BufferedImage = " + bi);
         
         assertEquals(bi.getWidth(), ii.getWidth());
      } catch (Exception e) {
         fail("no exceptions expected");
      }
   }

}