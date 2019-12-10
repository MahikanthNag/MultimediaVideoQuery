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
		VideoDataSerialization obj = new VideoDataSerialization();
		
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
	
	private void deserialize(String path) throws IOException, ClassNotFoundException {
	        FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path);
	        ObjectInputStream iis = new ObjectInputStream(fis);
	        frames = (ArrayList<byte[][][]>) iis.readObject();
	        System.out.println("hi");
	}
	
	private void serialize(String path) {
		try {
	        FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path);
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(frames);
	        oos.close();
	        System.out.println("Serialzed " + path);

	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	private void fillFramesList(String path) throws IOException {
		frames = new ArrayList<>();
		System.out.println("filling " + path + " started");
		for(int i = 0; i < 600; i++) {
			File file = new File(Constants.BASE_DB_VIDEO_PATH + path + "/" + path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb");
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);
			byte[] bytes = new byte[Constants.HEIGHT * Constants.WIDTH * 3];
			raf.read(bytes);
			raf.close();
			
			int ind = 0;
			byte[][][] rgb = new byte[Constants.HEIGHT][Constants.WIDTH][3]; 
			for(int y = 0; y < Constants.HEIGHT; y++) {
				for(int x = 0; x < Constants.WIDTH; x++) {
					int r = bytes[ind];
					int g = bytes[ind + Constants.HEIGHT * Constants.WIDTH];
					int b = bytes[ind + Constants.HEIGHT * Constants.WIDTH * 2];

					rgb[y][x][0] = (byte)r;
					rgb[y][x][1] = (byte)g;
					rgb[y][x][2] = (byte)b;
					ind++;
				}
			}
			
			frames.add(rgb);			
		}
		System.out.println("filled " + path);
	}
	
	private String getFileNameSuffix(int num) {
		if (num < 10) {
			return "00";
		} else if (num < 100) {
			return "0";
		} else {
			return "";
		}
	}
}
