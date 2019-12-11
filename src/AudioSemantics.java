import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioSemantics {
	
	
	private int minValue = Integer.MAX_VALUE;
	private int maxValue = Integer.MIN_VALUE;
	ArrayList<Double> audioVariance;
	private int frameLength = 10;
	
	Map<String, Double> similarities = new HashMap<>();
	
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
	
	public ArrayList<Double> audioAnalysis(String filePath) {
		File file = new File(filePath);
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
			AudioFormat audioFormat = audioInputStream.getFormat();
			
			int totalSize = audioFormat.getFrameSize() * (int)audioInputStream.getFrameLength();
//			int numOfChannels = audioFormat.getChannels();
			
			byte[] audioBuffer = new byte[totalSize];
			
			int readBytes = 0;
			int framesRead = 0;
			ArrayList<Integer> frameValues = new ArrayList<>();
			ArrayList<Double> frameValuesWithRespectToVideo = new ArrayList<>();

			readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
			framesRead += readBytes/audioFormat.getFrameSize();
			
			for(int i = 0; i < readBytes; i++) {
//				double value = (double) ((audioBuffer[i * 2] & 0xff) + (audioBuffer[i * 2 + 1] << 8));
				frameValues.add(overcomeByteRangeeError(audioBuffer[i]));
			}
			
			int ratio = Constants.AUDIO_FRAME_RATE / 60; 
			
			int lengthOfAudio = readBytes / Constants.AUDIO_FRAME_RATE * 60;
            for (int i = 0; i < lengthOfAudio; i++) {
                double sum = 0;
                for (int j = 0; j < ratio; j++) {
                    sum += frameValues.get(i * ratio + j);
                }
                frameValuesWithRespectToVideo.add(sum);
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
		
        Entry<Integer, Double> minError = calculateMinError(queryFrames, databaseAudioFrames);
        
//        double matchPercentage = 100 - Math.round(minError.getValue()*10000.0)/100.0;
        
		return Constants.AUDIO_PRIORITY * minError.getValue();
				
 		
	}
	
	public Entry<Integer, Double> calculateMinError(ArrayList<Double> queryFrames, ArrayList<Double> databaseAudioFrames) {
		
		Map<Integer, Double> errorValues = new HashMap<>();
//		for(int i = 0; i < databaseAudioFrames.size() - queryFrames.size(); i++) {
//			double sum = 0.0;
//			double databaseSum = 0.0;
//			for(int j = 0; j < queryFrames.size(); j++) {
//				sum += Math.abs(databaseAudioFrames.get(i + j) - queryFrames.get(j));
//				databaseSum += Math.abs(databaseAudioFrames.get(i + j));
//			}
//			errorValues.put(i, Math.min(sum, databaseSum)/Math.max(sum, databaseSum));
//		}
		int chunckSize = 100;
		for(int i = 0; i < queryFrames.size() - chunckSize; i++) {
			ArrayList<Double> chunkOfQueryFrame = new ArrayList<Double>(queryFrames.subList(i, i + chunckSize));
			double minVal = Integer.MAX_VALUE;
			for(int j = 0; j < databaseAudioFrames.size() - chunckSize; j++) {
				ArrayList<Double> chunkOfDBFrame = new ArrayList<Double>(databaseAudioFrames.subList(j, j + chunckSize));
				double sum = 0.0;
				for(int k = 0; k < chunckSize; k++) {
					sum += Math.abs(chunkOfQueryFrame.get(k) - chunkOfDBFrame.get(k));
				}
				if(sum < minVal) {
					minVal = sum;
				}
			}
			errorValues.put(i, minVal);
		}
		
		
		return calculateMinEntry(errorValues);
	}

	private Entry<Integer, Double> calculateMinEntry(Map<Integer, Double> errorValues) {
		Entry<Integer, Double> min = null;
		for(Entry<Integer, Double> entry : errorValues.entrySet()) {
		    if (min == null || min.getValue() > entry.getValue()) {
		        min = entry;
		    }
		}
		return min;
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
