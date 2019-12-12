import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioSemantics {
	
	
	private int minValue = Integer.MAX_VALUE;
	private int maxValue = Integer.MIN_VALUE;
	ArrayList<Double> audioVariance;

	public HashMap<String, HashMap<Integer, Double>> framewiseAudioValues = new HashMap<>();
	Map<String, Integer> maxIndices = new HashMap<>();
	private int frameLength = 10;
	
	Map<String, Double> similarities = new HashMap<>();
	
	public HashMap<String, HashMap<Integer, Double>> getFramewiseAudioValues() {
		return framewiseAudioValues;
	}

	@SuppressWarnings("unlikely-arg-type")
	public Map<String, Double> calculateStatsOfAllPairs(String queryPath) throws ClassNotFoundException, IOException {
		Map<Integer, Double> distances = new HashMap<>();
		String[] videoNames = {"flowers", "interview", "movie", "musicvideo", "sports", "starcraft", "traffic"};
		distances.put(0, calculateSimilarity("flowers", queryPath));
		distances.put(1, calculateSimilarity("interview", queryPath));
		distances.put(2, calculateSimilarity("movie", queryPath));
		distances.put(3, calculateSimilarity("musicvideo", queryPath));
		distances.put(4, calculateSimilarity("sports", queryPath));
		distances.put(5, calculateSimilarity("starcraft", queryPath));
		distances.put(6, calculateSimilarity("traffic", queryPath));
		
		double maxDist = 0, minDist = Double.MAX_VALUE;

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
			double simVal = (100 - ((distances.get(i) - minDist)/(maxDist - minDist)*100))*0.9;
			similarities.put(videoNames[i], Constants.CONTRAST_PRIORITY * simVal);
		}
		Entry<String, Double> maxEntry = calculateMaxEntryString(similarities);
		calculateHighestValues(framewiseAudioValues.get(maxEntry.getKey()), maxIndices.get(maxEntry.getKey()));
		return similarities;
	}
	
	private void calculateHighestValues(Map<Integer, Double> map, Integer index) {
		for(int i = index; i < index + 150; i++) {
			map.put(i, (map.get(i) / 15) * 22);
		}
		
	}

	public ArrayList<Double> audioAnalysis(String filePath) {
		File file = new File(filePath);
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
			AudioFormat audioFormat = audioInputStream.getFormat();
			
			int totalSize = audioFormat.getFrameSize() * (int)audioInputStream.getFrameLength();
//			int numOfChannels = audioFormat.getChannels();
			
			byte[] audioBuffer = new byte[totalSize];
			
			int readBytes = 0;
			double framesRead = 0;
			ArrayList<Double> frameValues = new ArrayList<>();
			ArrayList<Double> frameValuesWithRespectToVideo = new ArrayList<>();

			while((readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length)) != -1) {
				readBytes = readBytes/audioFormat.getFrameSize();
				framesRead += readBytes;
			}
			
			for(int i = 0; i < framesRead; i++) {
				double value = (double) ((audioBuffer[i * 2] & 0xff) + (audioBuffer[i * 2 + 1] << 8));
				frameValues.add(value);
			}
			
			int ratio = (int)audioFormat.getFrameRate() / 30; 
			
			int lengthOfAudio =(int) Math.ceil(framesRead /(audioFormat.getFrameRate()) * 30);
            for (int i = 0; i < lengthOfAudio - 1; i++) {
                double sum = 0;
                for (int j = 0; j < ratio; j++) {
                    sum += Math.abs(frameValues.get(i * ratio + j));
                }
                frameValuesWithRespectToVideo.add((sum));
            }
			
			return frameValuesWithRespectToVideo;
			
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	

	@SuppressWarnings("unchecked")
	public double calculateSimilarity(String databaseAudioPath, String queryPath) throws IOException, ClassNotFoundException {
		ArrayList<Double> queryFrames = audioAnalysis(Constants.BASE_QUERY_VIDEO_PATH + queryPath + "/" + queryPath + ".wav");
		FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + databaseAudioPath + "_audio.txt");
        ObjectInputStream iis = new ObjectInputStream(fis);
        ArrayList<Double> databaseAudioFrames = (ArrayList<Double>) iis.readObject();
        iis.close();
		
        Map<Integer, Double> minErrorValues = calculateMinError(queryFrames, databaseAudioFrames);
        Entry<Integer, Double> minError = calculateMinEntry(minErrorValues);
        
        // calculating the graph values 
        ArrayList<Double> graphValues = new ArrayList<>(databaseAudioFrames.subList(minError.getKey(), minError.getKey() + 150));
        calculateGraphValues(graphValues, queryFrames, minError.getKey(), databaseAudioPath);
        
        // calculating percentage
//        Entry<Integer, Double> maxError = calculateMaxEntry(minErrorValues);
//        Map<Integer, Double> framewisePercentage = new HashMap<>();
//        for(Entry<Integer, Double> entry : minErrorValues.entrySet()) {
//        	framewisePercentage.put(entry.getKey(), (100 - (entry.getValue()/maxError.getValue())*100)*Constants.AUDIO_PRIORITY);
//        }
//        framewiseAudioValues.put(databaseAudioPath, framewisePercentage);
        
        
		return minError.getValue();
	}
	
	public void calculateGraphValues(ArrayList<Double> graphValues, ArrayList<Double> queryFrames, int index, String databaseAudioPath) {
		ArrayList<Double> values = new ArrayList<>();
		for(int i = 0; i < queryFrames.size(); i++) {
			double value = Math.abs(graphValues.get(i) - queryFrames.get(i));
			values.add(value);	
		}
		
		Double maxVal = Collections.max(values);
		int maxIndex = values.indexOf(Collections.max(values)) + index;
		maxIndices.put(databaseAudioPath, maxIndex);
		for(int i = 0; i < queryFrames.size(); i++) {
			values.set(i, (values.get(i)/maxVal) * 15);
		}
		HashMap<Integer, Double> framewisePercentage = new HashMap<>();
		initializeFinalMap(framewisePercentage);
		
		for(int i = 0; i < queryFrames.size(); i++) {
			framewisePercentage.put(index + i, values.get(i));
		}
		
		framewiseAudioValues.put(databaseAudioPath, framewisePercentage);
		
	}
	
	private void initializeFinalMap(Map<Integer, Double> frameWiseSimilarity) {
		Random r = new Random();
		for(int i = 0; i < 600; i++) {
			double randomValue = 0 + (5 - 0) * r.nextDouble();
			frameWiseSimilarity.put(i,  randomValue);
		}
	}

	public Map<Integer, Double> calculateMinError(ArrayList<Double> queryFrames, ArrayList<Double> databaseAudioFrames) {
		Map<Integer, Double> errorValues = new HashMap<>();
		for(int i = 0; i < databaseAudioFrames.size() - queryFrames.size(); i++) {
			double sum = 0.0;
			for(int j = 0; j < queryFrames.size(); j++) {
				sum += Math.abs(databaseAudioFrames.get(i + j) - queryFrames.get(j));
			}
			
			errorValues.put(i, Math.abs(sum));
		}
//		int chunckSize = 35;
//		Map<Integer, Integer> minCount = new HashMap<>();
//		for(int i = 0; i < queryFrames.size() - chunckSize; i++) {
//			ArrayList<Double> chunkOfQueryFrame = new ArrayList<Double>(queryFrames.subList(i, i + chunckSize));
//			double minVal = Integer.MAX_VALUE;
//			int frameNo = 0;
//			for(int j = 0; j < databaseAudioFrames.size() - chunckSize; j++) {
//				ArrayList<Double> chunkOfDBFrame = new ArrayList<Double>(databaseAudioFrames.subList(j, j + chunckSize));
//				double sum = 0.0;
//				for(int k = 0; k < chunckSize; k++) {
//					sum += Math.abs(chunkOfDBFrame.get(k) - chunkOfQueryFrame.get(k));
//				}
//				
//				if(sum < minVal) {
//					minVal = sum;
//					frameNo = j;
//				}
//			}
//			if(errorValues.get(frameNo) == null) {
//				errorValues.put(frameNo, Math.abs(minVal));
//				minCount.put(frameNo, 1);
//			}
//			else {
//				errorValues.put(frameNo, errorValues.get(frameNo) + Math.abs(minVal));
//				minCount.put(frameNo, minCount.get(frameNo) + 1);
//			}
//		}
		
		
		return errorValues;
	}

	private Entry<Integer, Double> calculateMinEntry(Map<Integer, Double> values) {
		Entry<Integer, Double> min = null;
		for(Entry<Integer, Double> entry : values.entrySet()) {
		    if (min == null || min.getValue() > entry.getValue()) {
		        min = entry;
		    }
		}
		return min;
	}
	
	private Entry<String, Double> calculateMaxEntryString(Map<String, Double> values) {
		Entry<String, Double> max = null;
		for(Entry<String, Double> entry : values.entrySet()) {
		    if (max == null || max.getValue() < entry.getValue()) {
		        max = entry;
		    }
		}
		return max;
	}
	
	private Entry<Integer, Double> calculateMaxEntry(Map<Integer, Double> values) {
		Entry<Integer, Double> max = null;
		for(Entry<Integer, Double> entry : values.entrySet()) {
		    if (max == null || max.getValue() < entry.getValue()) {
		        max = entry;
		    }
		}
		return max;
	}

	public void caluculateAndSerializeColorValue(String path) throws IOException {
		audioVariance = audioAnalysis(Constants.BASE_DB_VIDEO_PATH + path + "/" + path + ".wav");
		FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_audio.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(audioVariance);        
        oos.close();
	}
	
	public int overcomeByteRangeeError(byte b) {
		if(b < 0) return (int)b + 256;
		else return (int)b;
	}

}
