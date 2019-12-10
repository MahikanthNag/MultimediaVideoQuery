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
		similarities.put("flowers", calculateSimilarity("flowers", queryPath));
		similarities.put("interview", calculateSimilarity("interview", queryPath));
		similarities.put("movie", calculateSimilarity("movie", queryPath));
		similarities.put("musicvideo", calculateSimilarity("musicvideo", queryPath));
		similarities.put("sports", calculateSimilarity("sports", queryPath));
		similarities.put("starcraft", calculateSimilarity("starcraft", queryPath));
		similarities.put("traffic", calculateSimilarity("traffic", queryPath));
		
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
			ArrayList<Double> frameValues = new ArrayList<>();
			ArrayList<Double> frameValuesWithRespectToVideo = new ArrayList<>();
			
			while (readBytes != -1) {
				readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
				framesRead += readBytes/audioFormat.getFrameSize();
			}
//			int sum = 0;
//			double avgValue = 0.0;
//			double sumValue = 0.0;
//			
//			for(int i = 0; i < audioBuffer.length; i++) {
//				sum += audioBuffer[i];
//			}
//			avgValue = sum/audioBuffer.length;
//			
//			for(int j = 0; j < audioBuffer.length; j++) {
//				sumValue += Math.pow(audioBuffer[j] - avgValue, 2); 
//			}
//			
//			return sumValue/audioBuffer.length;
			
			for(int i = 0; i < framesRead; i++) {
				double value = (double) ((audioBuffer[i * 2] & 0xff) + (audioBuffer[i * 2 + 1] << 8));
				frameValues.add(value);
			}
			
			int ratio = Constants.AUDIO_FRAME_RATE / Constants.FRAME_RATE; 
			
			int lengthOfAudio = framesRead / Constants.AUDIO_FRAME_RATE * Constants.FRAME_RATE;
            for (int i = 0; i < lengthOfAudio; i++) {
                double sum = 0;
                for (int j = 0; j < ratio; j++) {
                    sum += Math.abs(frameValues.get(i * ratio + j));
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
        
        double matchPercentage = 100 - Math.round(minError.getValue()*10000.0)/100.0;
        
		return Constants.AUDIO_PRIORITY * matchPercentage;
				
 		
	}
	
	public Entry<Integer, Double> calculateMinError(ArrayList<Double> queryFrames, ArrayList<Double> databaseAudioFrames) {
		
		Map<Integer, Double> errorValues = new HashMap<>();
		for(int i = 0; i < databaseAudioFrames.size() - queryFrames.size(); i++) {
			double sum = 0.0;
			double databaseSum = 0.0;
			for(int j = 0; j < queryFrames.size(); j++) {
				sum += Math.abs(databaseAudioFrames.get(i + j) - queryFrames.get(j));
				databaseSum += databaseAudioFrames.get(i + j);
			}
			errorValues.put(i, Math.min(sum, databaseSum)/Math.max(sum, databaseSum));
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

}
