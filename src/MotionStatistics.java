import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

import java.util.List;

public class MotionStatistics {
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
		
		FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_motion.txt");
        ObjectInputStream iis = new ObjectInputStream(fis);
		HashMap<Integer, Integer> dbMotionStatistics = (HashMap<Integer, Integer>) iis.readObject();
		HashMap<Integer, Integer> dominantMotionInQuery = findMotionForAllFrames(Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath, 0);
		iis.close();
		
		return getSimilarityScore(dbMotionStatistics, dominantMotionInQuery);
	}
	
	private double getSimilarityScore(HashMap<Integer, Integer> dbMotionStatistics, HashMap<Integer, Integer> dominantMotionInQuery) {
		return 100.0;
	}

	public void caluculateAndSerializeMotionValue(String path) throws IOException {
		HashMap<Integer, Integer> framewiseMotionStatistics = findMotionForAllFrames(Constants.BASE_DB_VIDEO_PATH + path + "/" + path, 0);
		FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_motion.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(framewiseMotionStatistics);
        oos.close();
	}
	
	private HashMap<Integer, Integer> findMotionForAllFrames(String path, int type) throws IOException {
		int frameSize;
		HashMap<Integer, Integer> framewiseMotionStatistics = new HashMap<>();
		if(type == 0) {
			frameSize = 600;
		}
		else {
			frameSize = 150;
		}
		
		BufferedImage prevFrame = null;
		for(int i = 0; i < frameSize; i++) {
			String framePath = path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb";
			BufferedImage curFrame = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);
			readImageRGB(Constants.WIDTH, Constants.HEIGHT, framePath, curFrame);
			int imageDiff = findFrameDifference(curFrame, prevFrame);
			if (i == 0) {
				framewiseMotionStatistics.put(i, 0);
			}
			else {
				framewiseMotionStatistics.put(i, imageDiff);				
			}
			prevFrame = curFrame;
		}
		return framewiseMotionStatistics;
	}

	public int findFrameDifference(BufferedImage curImage, BufferedImage prevImage)
	{
		int pRED, pGREEN, pBLUE, rRED, rGREEN, rBLUE;
		List<Float> diff = new ArrayList<>();
		int width = curImage.getWidth();
		int height = curImage.getHeight();
		int[][][] curFrame = new int[width][height][3];
		int[][][] prevFrame = new int[width][height][3];

		for(int y = 0; y < width; y++)
		{
			for(int x = 0; x < height; x++)
			{
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
		
		for(int y = 0; y < width; y++)
		{
			for(int x = 0; x < height; x++)
			{	
                pRED = Math.abs(curFrame[y][x][0] - prevFrame[y][x][0]);
                pGREEN = Math.abs(curFrame[y][x][1] - prevFrame[y][x][1]);
                pBLUE = Math.abs(curFrame[y][x][2] - prevFrame[y][x][2]);
                
                rRED = (pRED < 0) ? 0 : pRED;
                rGREEN = (pGREEN < 0) ? 0 : pGREEN;
                rBLUE = (pBLUE < 0) ? 0 : pBLUE;

                if (rRED < 10 && rGREEN < 10 && rBLUE < 10)
                	diff.add(0.0f);
                else
                	diff.add((rRED + rGREEN + rBLUE)/3.0f);
			}
		}
		float imageDiff = 0;
		int total = 0;
		for (Float d : diff) {
			imageDiff += d;
			total ++;
		}
		imageDiff = imageDiff / total;
		System.out.println("Image difference " + imageDiff);

		return (int)imageDiff;
	}
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
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