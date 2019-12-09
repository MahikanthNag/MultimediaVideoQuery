import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.List;

public class DominantColors {
	
	private HashMap<Integer, List<String>> framewiseDominantListOfColors = new HashMap<>();
	
	public HashMap<Integer, List<String>> getFramewiseDominantListOfColors() {
		return framewiseDominantListOfColors;
	}

	public void setFramewiseDominantListOfColors(HashMap<Integer, List<String>> framewiseDominantListOfColors) {
		this.framewiseDominantListOfColors = framewiseDominantListOfColors;
	}
	
	public void readImageRGB(int width, int height, byte[][][] bytes, String frameNum)
	{
		HashMap<String, Integer> dominantMap = new HashMap<String, Integer>();
		int[][][] buff = new int[height][width][3];
		
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{	
				buff[y][x][0] = rgbChecks(bytes[y][x][0]);
				buff[y][x][1] = rgbChecks(bytes[y][x][1]);
				buff[y][x][2] = rgbChecks(bytes[y][x][2]);
				if(checkGrayness(buff[y][x])) {
					String key = buff[y][x][0] + "_" + buff[y][x][1] + "_" + buff[y][x][2];
					if(dominantMap.get(key) != null)
						dominantMap.put(key, dominantMap.get(key) + 1);
					else
						dominantMap.put(key, 1);
				}
			}
		}
		ArrayList<String> dominantColors = new ArrayList<>();
		
		getDominantColors(dominantMap, dominantColors);
		
		framewiseDominantListOfColors.put(Integer.parseInt(frameNum), dominantColors);
	}
	
	public int rgbChecks(byte val) {
		
		if(val < 0) {
			return (int)val + 256;
		} else {
			return (int)val;
		}
	}

	public void getDominantColors(HashMap<String, Integer> dominantMap, ArrayList<String> dominantColors) {
		ArrayList<Map.Entry<String, Integer>> results = new ArrayList<>(dominantMap.entrySet());
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
		for(Map.Entry<String, Integer> item : results.subList(0, 10)) {
			dominantColors.add(item.getKey());
		}
	}

	public boolean checkGrayness(int[] pixel) {
		int rgDiff = Math.abs(pixel[0] - pixel[1]);
		int rbDiff = Math.abs(pixel[0] - pixel[2]);
		if(rgDiff > 10 || rbDiff > 10)
			return true;
		return false;
	}

}
