import java.awt.Color;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MotionStatistics {
	Map<String, Double> similarities = new HashMap<>();

	public Map<String, Double> calculateStatsOfAllPairs(String queryPath) throws ClassNotFoundException, IOException {
		Map<Integer, Double> distances = new HashMap<>();
		String[] videoNames = { "flowers", "interview", "movie", "musicvideo", "sports", "starcraft", "traffic" };
		distances.put(0, calculateStats("flowers", queryPath));
		distances.put(1, calculateStats("interview", queryPath));
		distances.put(2, calculateStats("movie", queryPath));
		distances.put(3, calculateStats("musicvideo", queryPath));
		distances.put(4, calculateStats("sports", queryPath));
		distances.put(5, calculateStats("starcraft", queryPath));
		distances.put(6, calculateStats("traffic", queryPath));

		double maxDist = 0, minDist = Double.MAX_VALUE;
		for (int i = 0; i < distances.size(); i++) {
			double val = distances.get(i);
			if (val < minDist)
				minDist = val;
			if (val > maxDist)
				maxDist = val;
		}
		for (int i = 0; i < distances.size(); i++) {
			double simVal = (100 - ((distances.get(i) - minDist) / (maxDist - minDist) * 100))*0.9;
			similarities.put(videoNames[i], Constants.MOTION_VECTOR_PRIORITY * simVal);
		}
		return similarities;
	}

	@SuppressWarnings("unchecked")
	public double calculateStats(String path, String queryPath) throws IOException, ClassNotFoundException {

		FileInputStream fis = new FileInputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_motion.txt");
		ObjectInputStream iis = new ObjectInputStream(fis);
		HashMap<Integer, Integer> dbMotionStatistics = (HashMap<Integer, Integer>) iis.readObject();
		HashMap<Integer, Integer> dominantMotionInQuery = findMotionForAllFrames(
				Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 1);
		iis.close();

		return getSimilarityScore(dbMotionStatistics, dominantMotionInQuery);
	}

	private double getSimilarityScore(HashMap<Integer, Integer> dbMotionStatistics,
			HashMap<Integer, Integer> dominantMotionInQuery) {
		double leastDiff = 99999999;
		for (int i = 0; i <= (dbMotionStatistics.size() - dominantMotionInQuery.size()); i++) {
			double curDiff = 0;
			for (int j = 0; j < dominantMotionInQuery.size(); j++) {
				curDiff += Math.abs(dbMotionStatistics.get(i + j) - dominantMotionInQuery.get(j)) / 1000;
			}
			if (curDiff < leastDiff) {
				leastDiff = curDiff;
			}
		}
		return leastDiff;
	}
	public HashMap<String, HashMap<Integer, Double>> getGraphMappingForAllVideos(String queryPath)
			throws ClassNotFoundException, IOException {
		HashMap<String, HashMap<Integer, Double>> mapping = new HashMap<>();
		mapping.put("flowers", calculateGraphStats("flowers", queryPath));
		mapping.put("interview", calculateGraphStats("interview", queryPath));
		mapping.put("movie", calculateGraphStats("movie", queryPath));
		mapping.put("musicvideo", calculateGraphStats("musicvideo", queryPath));
		mapping.put("sports", calculateGraphStats("sports", queryPath));
		mapping.put("starcraft", calculateGraphStats("starcraft", queryPath));
		mapping.put("traffic", calculateGraphStats("traffic", queryPath));
		return mapping;
	}

	private HashMap<Integer, Double> calculateGraphStats(String path, String queryPath)
			throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_contrast.txt");
		ObjectInputStream iis = new ObjectInputStream(fis);
		HashMap<Integer, Double> dbMotionStatistics = (HashMap<Integer, Double>) iis.readObject();
		HashMap<Integer, Integer> dominantMotionInQuery = findMotionForAllFrames(
				Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 1);
		iis.close();
		return getSimilarityForGraph(dbMotionStatistics, dominantMotionInQuery);
	}

	private HashMap<Integer, Double> getSimilarityForGraph(HashMap<Integer, Double> dbMotionStatistics,
			HashMap<Integer, Integer> dominantMotionInQuery) {
		HashMap<Integer, Double> allFramesSimilarity = new HashMap<>();
	    for (int i = 0; i < Constants.DB_VIDEO_FRAME_SIZE; i++) {
	        allFramesSimilarity.put(i, Double.MAX_VALUE);
	    }
	    double minDiff = Double.MAX_VALUE, maxDiff = Double.MIN_VALUE;
		for (int i = 0; i <= (Constants.DB_VIDEO_FRAME_SIZE - Constants.QUERY_VIDEO_FRAME_SIZE); i++) {
			for (int j = 0; j < Constants.QUERY_VIDEO_FRAME_SIZE; j++) {
				double dbVal = dbMotionStatistics.get(i + j);
				double queryVal = dominantMotionInQuery.get(j);
				double curDiff = Math.abs(dbVal - queryVal);
				if (curDiff < allFramesSimilarity.get(i + j))
					allFramesSimilarity.replace(i + j, curDiff);
			}
		}
		for (int i = 0; i < Constants.DB_VIDEO_FRAME_SIZE; i++) {
			double curDiff = allFramesSimilarity.get(i);
			if (curDiff < minDiff)
				minDiff = curDiff;
			if (curDiff > maxDiff)
				maxDiff = curDiff;			
		}
	    // Replacing diff by similarity
	    for (int i = 0; i < Constants.DB_VIDEO_FRAME_SIZE; i++) {
	        double diff = allFramesSimilarity.get(i);
			double simVal = 100 - (diff);
			allFramesSimilarity.replace(i, simVal);
	    }
		return allFramesSimilarity;
	}

	
	public void caluculateAndSerializeMotionValue(String path) throws IOException {
		HashMap<Integer, Integer> framewiseMotionStatistics = findMotionForAllFrames(
				Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		FileOutputStream fos = new FileOutputStream(
				Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_motion.txt");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(framewiseMotionStatistics);
		oos.close();
	}

	private HashMap<Integer, Integer> findMotionForAllFrames(String path, int type) throws IOException {
		int frameSize;
		HashMap<Integer, Integer> framewiseMotionStatistics = new HashMap<>();
		if (type == 0) {
			frameSize = 600;
		} else {
			frameSize = 150;
		}

		BufferedImage prevFrame = null;
		for (int i = 0; i < frameSize; i++) {
			String framePath = path + getFileNameSuffix(i + 1, path) + (i + 1) + ".rgb";
			BufferedImage curFrame = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);
			readImageRGB(Constants.WIDTH, Constants.HEIGHT, framePath, curFrame);

			int imageDiff;
			if (i == 0) {
				framewiseMotionStatistics.put(i, 0);
			} else {
				imageDiff = findFrameDifference(curFrame, prevFrame);
				framewiseMotionStatistics.put(i, imageDiff);
			}
			prevFrame = curFrame;
		}
		return framewiseMotionStatistics;
	}

	public int findFrameDifference(BufferedImage curImage, BufferedImage prevImage) {
		int pRED, pGREEN, pBLUE, rRED, rGREEN, rBLUE;
		List<Float> diff = new ArrayList<>();
		int width = curImage.getWidth();
		int height = curImage.getHeight();
		int[][][] curFrame = new int[width][height][3];
		int[][][] prevFrame = new int[width][height][3];

		for (int y = 0; y < width; y++) {
			for (int x = 0; x < height; x++) {
				Color curColor = new Color(curImage.getRGB(y, x));
				Color prevColor = new Color(prevImage.getRGB(y, x));
				curFrame[y][x][0] = curColor.getRed();
				curFrame[y][x][1] = curColor.getGreen();
				curFrame[y][x][2] = curColor.getBlue();
				prevFrame[y][x][0] = prevColor.getRed();
				prevFrame[y][x][1] = prevColor.getGreen();
				prevFrame[y][x][2] = prevColor.getBlue();
			}
		}

		for (int y = 0; y < width; y++) {
			for (int x = 0; x < height; x++) {
				pRED = Math.abs(curFrame[y][x][0] - prevFrame[y][x][0]);
				pGREEN = Math.abs(curFrame[y][x][1] - prevFrame[y][x][1]);
				pBLUE = Math.abs(curFrame[y][x][2] - prevFrame[y][x][2]);

				rRED = (pRED < 0) ? 0 : pRED;
				rGREEN = (pGREEN < 0) ? 0 : pGREEN;
				rBLUE = (pBLUE < 0) ? 0 : pBLUE;

				if (rRED < 10 && rGREEN < 10 && rBLUE < 10)
					diff.add(0.0f);
				else
					diff.add((rRED + rGREEN + rBLUE) / 3.0f);
			}
		}
		float imageDiff = 0;
		int total = 0;
		for (Float d : diff) {
			imageDiff += d;
			total++;
		}
		// System.out.println("Image difference " + imageDiff);

		return (int) imageDiff;
	}

	private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
		try {
			int frameLength = width * height * 3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x, y, pix);
					ind++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
}