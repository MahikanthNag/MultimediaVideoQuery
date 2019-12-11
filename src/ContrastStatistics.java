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
		Map<Integer, Double> distances = new HashMap<>();
		String[] videoNames = {"flowers", "interview", "movie", "musicvideo", "sports", "starcraft", "traffic"};
		distances.put(0, calculateStats("flowers", queryPath));
		distances.put(1, calculateStats("interview", queryPath));
		distances.put(2, calculateStats("movie", queryPath));
		distances.put(3, calculateStats("musicvideo", queryPath));
		distances.put(4, calculateStats("sports", queryPath));
		distances.put(5, calculateStats("starcraft", queryPath));
		distances.put(6, calculateStats("traffic", queryPath));
		
		double maxDist = 0, minDist = 9999999;
		for (int i = 0; i < distances.size(); i++)
		{
			double val = distances.get(i);
			if (val < minDist)
				minDist = val;
			if (val > maxDist)
				maxDist = val;
		}
		for (int i = 0; i < distances.size(); i++)
		{
			double simVal = 100 - ((distances.get(i) - minDist)/(maxDist - minDist)*100);
			similarities.put(videoNames[i], Constants.CONTRAST_PRIORITY * simVal);
		}
		return similarities;
	}
	
	public double calculateStats(String path, String queryPath) throws IOException, ClassNotFoundException {		
		FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_contrast.txt");
        ObjectInputStream iis = new ObjectInputStream(fis);
        HashMap<Integer, Double> dbVideoContrast = (HashMap<Integer, Double>) iis.readObject();
        
        HashMap<Integer, Double> queryContrast = findContrastOfAllFrames(Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 1);
		iis.close();
		
		return getSimilarityScore(dbVideoContrast, queryContrast);		
	}

	public void caluculateAndSerializeContrastValue(String path) throws IOException, FileNotFoundException {
		HashMap<Integer, Double> framewistContrastStatistics = findContrastOfAllFrames(Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		
		FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_contrast.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(framewistContrastStatistics);
        
        oos.close();
	}

	private double getSimilarityScore(HashMap<Integer, Double> dbVideoContrast, HashMap<Integer, Double> queryContrast) {
		double leastDiff = 99999999;
		for (int i = 0; i < (dbVideoContrast.size() - queryContrast.size()); i++)
		{
			double curDiff = 0;
			for (int j = 0; j < queryContrast.size(); j++)
			{
				curDiff += Math.abs(dbVideoContrast.get(i+j) - queryContrast.get(j));
			}
			if (curDiff < leastDiff)
			{
				leastDiff = curDiff;
			}
		}
		return leastDiff;
	}

	private HashMap<Integer, Double> findContrastOfAllFrames(String path, int type) throws IOException {
		int numOFrames  = 0;
		
		if(type == 0) {
			numOFrames = 600;
		}
		else {
			numOFrames = 150;
		}		

		HashMap<Integer, Double> allFramesContrast = new HashMap<>();

		for(int i = 0; i < numOFrames; i++) {
			File file = new File(path + getFileNameSuffix(i + 1, path) + (i + 1) + ".rgb");
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
			allFramesContrast.put(i, brightness / (Constants.HEIGHT * Constants.WIDTH));		
		}
		return allFramesContrast;
	}

	private String getFileNameSuffix(int num, String path) {
		String prefix = "";
		File file = new File(path + "001.rgb");
		if (!file.exists())
			prefix = "_";
		if (num < 10) {
			return prefix + "00";
		} else if (num < 100) {
			return prefix + "0";
		} else {
			return prefix;
		}
	}
	public int overcomeByteRangeeError(byte b) {
		if(b < 0) return (int)b + 256;
		else return (int)b;
	}
}
