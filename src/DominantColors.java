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

	Map<String, Double> similarities = new HashMap<>();
	Map<Integer, List<String>> dominantColorMapPerChunk = new HashMap<>();
	Map<String, Double> frameWiseSimilarity = new HashMap<>();
	Map<Integer, Map<String, Integer>> colorFreqMapPerFrame = new HashMap<>();
	Map<Integer, Map<String, Integer>> colorFreqMapPerFrameForDB = new HashMap<>();

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

		FileInputStream fis = new FileInputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_color.txt");
		ObjectInputStream iis = new ObjectInputStream(fis);
		Map<Integer, List<String>> dominantColorMapPerDBFrame;
		dominantColorMapPerDBFrame = (Map<Integer, List<String>>) iis.readObject();
		iis.close();
		
		fis = new FileInputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_colorCache.txt");
		iis = new ObjectInputStream(fis);
		colorFreqMapPerFrameForDB = (Map<Integer, Map<String, Integer>>) iis.readObject();

		Map<Integer, List<String>> dominantColorMapPerQueryFrame;
		dominantColorMapPerQueryFrame = findDominantColourPerChunk(
				Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 1);
		iis.close();

		return getSimilarityScore(dominantColorMapPerDBFrame, dominantColorMapPerQueryFrame);
	}

	private double getSimilarityScore(Map<Integer, List<String>> dominantColorMapPerDBFrame,
			Map<Integer, List<String>> dominantColorMapPerQueryFrame) {

		int numOfIterations = 0;
		frameWiseSimilarity = new HashMap<>();
		for (int i = 0; i < Constants.QUERY_VIDEO_FRAME_SIZE; i += Constants.FRAME_CHUNK_SIZE) {
			Set<String> dominantColorsInQuery = new HashSet<>();
			Set<String> dominantColorsInDb = new HashSet<>();

			dominantColorsInQuery.addAll(dominantColorMapPerQueryFrame.get(i));

			double similarity = Double.MIN_VALUE;
			int maxSimilarityStartFrame = 0;
			double total = (double) colorFreqMapPerFrame.get(i).values().stream().reduce(0, Integer::sum);

			for (int j = 0; j < Constants.DB_VIDEO_FRAME_SIZE; j += Constants.FRAME_CHUNK_SIZE) {
				dominantColorsInDb = new HashSet<>();
				dominantColorsInDb.addAll(dominantColorMapPerDBFrame.get(j));

				Object[] temp = dominantColorsInDb.toArray();
				int count = 0;
				for (int k = 0; k < dominantColorsInDb.size(); k++) {
					if (dominantColorsInQuery.contains((String) temp[k])) {
						int freq = Math.min(colorFreqMapPerFrame.get(i).get((String) temp[k])
								, colorFreqMapPerFrameForDB.get(j).get((String)temp[k]));
						count = count + freq;
					}
				}
//				double currentSimilarity = (double)count;
				double currentSimilarity = count / total;
				if (similarity < currentSimilarity) {
					similarity = currentSimilarity;
					maxSimilarityStartFrame = j;
				}
			}

			frameWiseSimilarity.put(i+"_"+maxSimilarityStartFrame, similarity);
			numOfIterations++;
		}

		double sum = frameWiseSimilarity.values().stream().reduce(0.0, Double::sum);
		return Constants.COLOR_PRIORITY * 100 * sum / numOfIterations;
	}

	public void caluculateAndSerializeColorValue(String path) throws IOException {
		Map<Integer, List<String>> dbColourMap = findDominantColourPerChunk(
				Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		FileOutputStream fos = new FileOutputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_color.txt");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(dbColourMap);
		oos.close();
		
		fos = new FileOutputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_colorCache.txt");
		oos = new ObjectOutputStream(fos);
		oos.writeObject(colorFreqMapPerFrame);		
		oos.close();
	}

	private Map<Integer, List<String>> findDominantColourPerChunk(String path, int type) throws IOException {
		int frameSize;
		if (type == 0) {
			frameSize = 600;
		} else {
			frameSize = 150;
		}

		dominantColorMapPerChunk = new HashMap<>();
		List<String> dominantColors = new ArrayList<>();
		Map<String, Integer> dominantMap = new HashMap<>();
		int i;
		colorFreqMapPerFrame = new HashMap<>();
		for (i = 0; i < frameSize; i++) {
			String framePath = path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb";
			readImageRGB(Constants.WIDTH, Constants.HEIGHT, framePath, i, dominantMap);

			if (i % Constants.FRAME_CHUNK_SIZE == 0 && i != 0) {
				getDominantColors(dominantMap, dominantColors);
				dominantColorMapPerChunk.put(i - Constants.FRAME_CHUNK_SIZE, dominantColors);
				dominantColors = new ArrayList<>();
				colorFreqMapPerFrame.put(i - Constants.FRAME_CHUNK_SIZE, dominantMap);
				dominantMap = new HashMap<>();
			}
		}
		if (i == frameSize) {
			getDominantColors(dominantMap, dominantColors);
			dominantColorMapPerChunk.put(i - Constants.FRAME_CHUNK_SIZE, dominantColors);
			dominantColors = new ArrayList<>();
			colorFreqMapPerFrame.put(i - Constants.FRAME_CHUNK_SIZE, dominantMap);
			dominantMap = new HashMap<>();
		}
		return dominantColorMapPerChunk;
	}

	public void readImageRGB(int width, int height, String path, int frameNum, Map<String, Integer> dominantMap)
			throws IOException {
		byte[] bytes = new byte[Constants.HEIGHT * Constants.WIDTH * 3];

		File file = new File(path);
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(0);
		raf.read(bytes);
		raf.close();

		int ind = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int r = overcomeByteRangeError(bytes[ind]);
				int g = overcomeByteRangeError(bytes[ind + height * width]);
				int b = overcomeByteRangeError(bytes[ind + height * width * 2]);
				String key = (int) (r / 64) + "_" + (int) (g / 64) + "_" + (int) (b / 64);
				dominantMap.put(key, dominantMap.getOrDefault(key, 0) + 1);
				ind++;
			}
		}
	}

	public void getDominantColors(Map<String, Integer> dominantMap, List<String> dominantColors) {
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

		int cap = dominantMap.size() / 10;
//		int cap = 100;
		int count = 0;
		for (Map.Entry<String, Integer> item : results) {
			if (count == cap)
				break;
			dominantColors.add(item.getKey());
			count++;
		}

	}

	public boolean checkGrayness(int[] pixel) {
		int rgDiff = Math.abs(pixel[0] - pixel[1]);
		int rbDiff = Math.abs(pixel[0] - pixel[2]);
		if (rgDiff > 10 || rbDiff > 10)
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

	public int overcomeByteRangeError(byte b) {
		if (b < 0)
			return (int) b + 256;
		else
			return (int) b;
	}
}
