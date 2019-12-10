import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DominantColors {
	
	Map<String, Integer> dominantMap = new HashMap<>();
	Set<String> dominantColors = new HashSet<>();
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
	
	@SuppressWarnings("unchecked")
	public double calculateStats(String path, String queryPath) throws IOException, ClassNotFoundException {
		
		FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_color.txt");
        ObjectInputStream iis = new ObjectInputStream(fis);
        dominantColors = (Set<String>) iis.readObject();
		Set<String> dominantColorsInQuery = findAverageColorOfAllFrames(Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 1);
		iis.close();
		
		return getSimilarityScore(dominantColors, dominantColorsInQuery);
	}
	
	private double getSimilarityScore(Set<String> dominantColors, Set<String> dominantColorsInQuery) {
		Object[] temp = dominantColorsInQuery.toArray();
		int count = 0;
		
		for(int i = 0; i < dominantColors.size(); i++) {
			if(dominantColors.contains((String)temp[i])) {
				count++;
			}
		}
		
		return Constants.COLOR_PRIORITY *  count * 100 / dominantColors.size();
	}

	public void caluculateAndSerializeColorValue(String path) throws IOException {
		dominantColors = findAverageColorOfAllFrames(Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_color.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(dominantColors);        
        oos.close();
	}
	
	private Set<String> findAverageColorOfAllFrames(String path, int type) throws IOException {
		int frameSize;
		if(type == 0) {
			frameSize = 600;
		}
		else {
			frameSize = 150;
		}
			
		for(int i = 0; i < frameSize; i++) {
			String framePath = path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb";
			readImageRGB(Constants.WIDTH, Constants.HEIGHT, framePath);
		}
		
		getDominantColors(dominantMap, dominantColors);
		return dominantColors;
	}

	public void readImageRGB(int width, int height, String path) throws IOException {		
		int[][][] buff = new int[height][width][3];
		byte[] bytes = new byte[Constants.HEIGHT * Constants.WIDTH * 3];

		File file = new File(path);
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(0);
		raf.read(bytes);

		raf.close();
		
		int ind = 0;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {	
				buff[y][x][0] = overcomeByteRangeeError(bytes[ind]);
				buff[y][x][1] = overcomeByteRangeeError(bytes[ind + height * width]);
				buff[y][x][2] = overcomeByteRangeeError(bytes[ind + height * width * 2]);
				if(checkGrayness(buff[y][x])) {
					String key = (int)(buff[y][x][0] / 8) + "_" + (int)(buff[y][x][1] / 8) + "_" + (int)(buff[y][x][2] / 8);
					dominantMap.put(key, dominantMap.getOrDefault(key, 0) + 1);
				}
				ind++;
			}
		}		
	}
	
	public void getDominantColors(Map<String, Integer> dominantMap, Set<String> dominantColors) {
		List<Map.Entry<String, Integer>> results = new ArrayList<>(dominantMap.entrySet());
		Collections.sort(results, new Comparator<Map.Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				if (o1.getValue() < o2.getValue()) {
		            return 1;
		        } else if (o1.getValue() > o2.getValue()) {
		            return -1;
		        }
				return 0;
			}
		});
		int cap = dominantMap.size() / 4;
		int count = 0;
		for(Map.Entry<String, Integer> item : results) {
			if(count == cap) break;
			dominantColors.add(item.getKey());
			count++;
		}
	}

	public boolean checkGrayness(int[] pixel) {
		int rgDiff = Math.abs(pixel[0] - pixel[1]);
		int rbDiff = Math.abs(pixel[0] - pixel[2]);
		if(rgDiff > 10 || rbDiff > 10)
			return true;
		return false;
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
