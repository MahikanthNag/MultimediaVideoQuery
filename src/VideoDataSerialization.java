import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class VideoDataSerialization {
	
	ArrayList<byte[][][]> frames;
	
	public static void main(String[] args) throws IOException {
		
		// Store Contrast Values of DB videos
		ContrastStatistics contrastStatistics = new ContrastStatistics();
		contrastStatistics.caluculateAndSerializeContrastValue("flowers");
		contrastStatistics.caluculateAndSerializeContrastValue("interview");
		contrastStatistics.caluculateAndSerializeContrastValue("movie");
		contrastStatistics.caluculateAndSerializeContrastValue("musicvideo");
		contrastStatistics.caluculateAndSerializeContrastValue("sports");
		contrastStatistics.caluculateAndSerializeContrastValue("starcraft");
		contrastStatistics.caluculateAndSerializeContrastValue("traffic");
		
		// Store Dominant Color Values of DB videos
		DominantColors dominantColors = new DominantColors();
		dominantColors.caluculateAndSerializeColorValue("flowers");
		dominantColors.caluculateAndSerializeColorValue("interview");
		dominantColors.caluculateAndSerializeColorValue("movie");
		dominantColors.caluculateAndSerializeColorValue("musicvideo");
		dominantColors.caluculateAndSerializeColorValue("sports");
		dominantColors.caluculateAndSerializeColorValue("starcraft");
		dominantColors.caluculateAndSerializeColorValue("traffic");

		// Store Dominant Motion Values of DB videos
		MotionStatistics motionStatistics = new MotionStatistics();
		motionStatistics.caluculateAndSerializeMotionValue("flowers");
		motionStatistics.caluculateAndSerializeMotionValue("interview");
		motionStatistics.caluculateAndSerializeMotionValue("movie");
		motionStatistics.caluculateAndSerializeMotionValue("musicvideo");
		motionStatistics.caluculateAndSerializeMotionValue("sports");
		motionStatistics.caluculateAndSerializeMotionValue("starcraft");
		motionStatistics.caluculateAndSerializeMotionValue("traffic");

		// Store Audio Values of DB videos
		AudioSemantics audioSemantics = new AudioSemantics();
		audioSemantics.caluculateAndSerializeColorValue("flowers");
		audioSemantics.caluculateAndSerializeColorValue("interview");
		audioSemantics.caluculateAndSerializeColorValue("movie");
		audioSemantics.caluculateAndSerializeColorValue("musicvideo");
		audioSemantics.caluculateAndSerializeColorValue("sports");
		audioSemantics.caluculateAndSerializeColorValue("starcraft");
		audioSemantics.caluculateAndSerializeColorValue("traffic");
	}
}
