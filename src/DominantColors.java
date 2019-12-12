import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DominantColors {

	Map<String, Double> similarities = new HashMap<>();
	Map<Integer, Map<String, Integer>> dominantColorMapPerFrame = new HashMap<>();
	Map<Integer, Double> frameWiseSimilarity = new HashMap<>();
	Map<Integer, Map<String, Integer>> colorFreqMapPerFrame = new HashMap<>();
	Map<Integer, Map<String, Integer>> colorFreqMapPerFrameForDB = new HashMap<>();
	Map<String, HashMap<Integer, Double>> videoWiseFrameSimilarity = new HashMap<>();

	public Map<String, HashMap<Integer, Double>> getGraphMappingForAllVideos() {
		return videoWiseFrameSimilarity;
	}

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
		Map<Integer, Map<String, Integer>> dominantColorMapPerDBFrame;
		dominantColorMapPerDBFrame = (Map<Integer, Map<String, Integer>>) iis.readObject();
		iis.close();

		Map<Integer, Map<String, Integer>> dominantColorMapPerQueryFrame;
		dominantColorMapPerQueryFrame = findDominantColourPerChunk(
				Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 1);
		iis.close();

		return getSimilarityScore(dominantColorMapPerDBFrame, dominantColorMapPerQueryFrame, path);
	}

	private double getSimilarityScore(Map<Integer, Map<String, Integer>> dominantColorMapPerDBFrame,
			Map<Integer, Map<String, Integer>> dominantColorMapPerQueryFrame, String queryPath) {

		frameWiseSimilarity = new HashMap<>();

		for (int i = 0; i < Constants.DB_VIDEO_FRAME_SIZE; i++) {

			int count = 0;
			double similarity = Double.MIN_VALUE;
			double total = 0;
			int maxSimilarityFrame = -1;

			for (int j = 0; j < Constants.QUERY_VIDEO_FRAME_SIZE; j++) {
				count = 0;
				total = 0.0;
//				total += (double) dominantColorMapPerDBFrame.get(i).values().stream().reduce(0, Integer::sum);
				total += (double) dominantColorMapPerQueryFrame.get(j).values().stream().reduce(0, Integer::sum);
				Set<String> queryKeySet = dominantColorMapPerQueryFrame.get(j).keySet();

				for (Map.Entry<String, Integer> entry : dominantColorMapPerDBFrame.get(i).entrySet()) {
					if (queryKeySet.contains(entry.getKey())) {
						count += Math.min(entry.getValue(), dominantColorMapPerQueryFrame.get(j).get(entry.getKey()));
					}
				}

				double currentSimilarity = 0.0;
				currentSimilarity = count / total;
				if (similarity < currentSimilarity) {
					similarity = currentSimilarity;
					maxSimilarityFrame = j;
				}
			}
			frameWiseSimilarity.put(i, similarity);
		}
		double meanSimilarity = frameWiseSimilarity.values().stream().reduce(0.0, Double::sum);
		
		for (Map.Entry<Integer, Double> entry : frameWiseSimilarity.entrySet()) {
			frameWiseSimilarity.put(entry.getKey(), entry.getValue() * 100 * Constants.COLOR_PRIORITY);
		}
		
		videoWiseFrameSimilarity.put(queryPath, (HashMap<Integer, Double>) frameWiseSimilarity);
		meanSimilarity = meanSimilarity / Constants.DB_VIDEO_FRAME_SIZE;
		return meanSimilarity * 100 * Constants.COLOR_PRIORITY;
	}

	public void caluculateAndSerializeColorValue(String path) throws IOException {
		Map<Integer, Map<String, Integer>> dbColourMap = findDominantColourPerChunk(
				Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		FileOutputStream fos = new FileOutputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_color.txt");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(dbColourMap);
		oos.close();
	}

	private Map<Integer, Map<String, Integer>> findDominantColourPerChunk(String path, int type) throws IOException {
		int frameSize;
		if (type == 0) {
			frameSize = 600;
		} else {
			frameSize = 150;
		}

		Map<Integer, Map<String, Integer>> dominantColorMapPerFrame = new HashMap<>();
		for (int i = 0; i < frameSize; i++) {
			String framePath = path + getFileNameSuffix(i + 1, path) + (i + 1) + ".rgb";
			Map<String, Integer> dominantMap = new HashMap<>();
			readImageRGB(Constants.WIDTH, Constants.HEIGHT, framePath, dominantMap);
			dominantMap = sortByValues(dominantMap);
			dominantColorMapPerFrame.put(i, dominantMap);
		}
		return dominantColorMapPerFrame;
	}

	public void readImageRGB(int width, int height, String path, Map<String, Integer> dominantMap) throws IOException {
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
				String key = (int) (r / 16) + "_" + (int) (g / 16) + "_" + (int) (b / 16);
				dominantMap.put(key, dominantMap.getOrDefault(key, 0) + 1);
				ind++;
			}
		}
	}

	public boolean checkGrayness(int[] pixel) {
		int rgDiff = Math.abs(pixel[0] - pixel[1]);
		int rbDiff = Math.abs(pixel[0] - pixel[2]);
		if (rgDiff > 10 || rbDiff > 10)
			return true;
		return false;
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

	public int overcomeByteRangeError(byte b) {
		if (b < 0)
			return (int) b + 256;
		else
			return (int) b;
	}

	public Map<String, Integer> sortByValues(Map<String, Integer> map) {
		 Comparator<String> valueComparator = new Comparator<String>() {
			public int compare(String k1, String k2) {
				int compare = map.get(k1).compareTo(map.get(k2));
				if (compare == 0)
					return 1;
				else
					return -compare;
			}
		};
		Map<String, Integer> sortedByValues = new TreeMap<>(valueComparator);
		Map<String, Integer> temp = new HashMap<>();
		sortedByValues.putAll(map);
		int count = map.size();
		int cap = count / 8;
		int i = 0;
		for (Map.Entry<String, Integer> entry : sortedByValues.entrySet()) {
			if (i == cap)
				break;
			temp.put(entry.getKey(), entry.getValue());
			i++;
		}
		return temp;
	}
}
