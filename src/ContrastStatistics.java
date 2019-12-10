import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class ContrastStatistics {

	Map<String, Double> similarities = new HashMap<>();
	
	public Map<String, Double> calculateStatsOfAllPairs(String queryPath) throws ClassNotFoundException, IOException {
		similarities.put("flowers", calculateStats("flowers", queryPath));
		similarities.put("interview", calculateStats("interview", queryPath));
		similarities.put("movie", calculateStats("movie", queryPath));
		similarities.put("musicvideo", calculateStats("musicvideo", queryPath));
		similarities.put("sports", calculateStats("sports", queryPath));
		similarities.put("starcraft", calculateStats("starcraft", queryPath));
		similarities.put("traffic", calculateStats("traffic", queryPath));
		
		return similarities;
	}
	
	public double calculateStats(String path, String queryPath) throws IOException, ClassNotFoundException {		
//		caluculateAndSerializeContrastValue(path);
        
        FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_contrast.txt");
        ObjectInputStream iis = new ObjectInputStream(fis);
        double avgContrast = (double) iis.readObject();
		double avgQueryContrast = findAverageContrastOfAllFrames(Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 0);
		iis.close();
		
		return getSimilarityScore(avgContrast, avgQueryContrast);		
	}

	public void caluculateAndSerializeContrastValue(String path) throws IOException, FileNotFoundException {
		double avgContrast = findAverageContrastOfAllFrames(Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		
		FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_contrast.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(avgContrast);
        
        oos.close();
	}

	private double getSimilarityScore(double avgContrast, double avgQueryContrast) {
		return Math.min(avgContrast, avgQueryContrast) / Math.max(avgContrast, avgQueryContrast);
	}

	private double findAverageContrastOfAllFrames(String path, int type) throws IOException {
		int numOFrames  = 0;
		if(type == 0) {
			numOFrames = 600;
		}
		else {
			numOFrames = 150;
		}
		double totalBrightnessOfAllFrames = 0;
		double averageContrast = 0;
		
		for(int i = 0; i < numOFrames; i++) {
			File file = new File(path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb");
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);
			byte[] bytes = new byte[Constants.HEIGHT * Constants.WIDTH * 3];
			raf.read(bytes);
			raf.close();			
			int ind = 0;
			double brightness = 0.0;
			for(int y = 0; y < Constants.HEIGHT; y++) {
				for(int x = 0; x < Constants.WIDTH; x++) {					
					int r = overcomeByteRangeeError(bytes[ind]);
					int g = overcomeByteRangeeError(bytes[ind + Constants.HEIGHT * Constants.WIDTH]);
					int b = overcomeByteRangeeError(bytes[ind + Constants.HEIGHT * Constants.WIDTH * 2]);
					brightness += (0.2126*r + 0.7152*g  + 0.0722*b);
					ind++;
				}
			}
			
			totalBrightnessOfAllFrames += brightness / (Constants.HEIGHT * Constants.WIDTH);			
		}
		
		averageContrast = totalBrightnessOfAllFrames / numOFrames;
		return averageContrast;
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
	public int overcomeByteRangeeError(byte b) {
		if(b < 0) return (int)b + 256;
		else return (int)b;
	}
}
