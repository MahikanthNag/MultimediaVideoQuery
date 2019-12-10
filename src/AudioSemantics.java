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
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioSemantics {
	
	
	private int minValue = Integer.MAX_VALUE;
	private int maxValue = Integer.MIN_VALUE;
	double audioVariance;
	
	public double audioAnalysis(String filePath) {
		File file = new File(filePath);
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
			AudioFormat audioFormat = audioInputStream.getFormat();
			
//			int totalSize = audioFormat.getFrameSize() * (int)audioInputStream.getFrameLength();
//			int numOfChannels = audioFormat.getChannels();
			
			byte[] audioBuffer = new byte[524288];
			
			int readBytes = 0;
			int framesRead = 0;
			
			while (readBytes != -1) {
				readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
				framesRead += readBytes/audioFormat.getFrameSize();
			}
			int sum = 0;
			double avgValue = 0.0;
			double sumValue = 0.0;
			
			for(int i = 0; i < audioBuffer.length; i++) {
				sum += audioBuffer[i];
			}
			avgValue = sum/audioBuffer.length;
			
			for(int j = 0; j < audioBuffer.length; j++) {
				sumValue += Math.pow(audioBuffer[j] - avgValue, 2); 
			}
			
			return sumValue/audioBuffer.length;
			
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	

	public double calculateSimilarity(String queryPath, String databaseAudioPath) throws IOException, ClassNotFoundException {
		double queryVariance = audioAnalysis(Constants.BASE_DB_VIDEO_PATH + queryPath + "/" + queryPath + ".wav");
		FileInputStream fis = new FileInputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + databaseAudioPath + "_audio.txt");
        ObjectInputStream iis = new ObjectInputStream(fis);
        double databaseAudioVariance = (double) iis.readObject();
		
		double similarity = Math.min(queryVariance, databaseAudioVariance)/Math.max(queryVariance, databaseAudioVariance);
		
		return Math.round(similarity*10000.0)/100.0;
				
 		
	}
	
	public void caluculateAndSerializeColorValue(String path) throws IOException {
		audioVariance = audioAnalysis(Constants.BASE_DB_VIDEO_PATH + path + "/" + path + ".wav");
		FileOutputStream fos = new FileOutputStream(Constants.BASE_DB_VIDEO_PATH + "serialized_video_data/" + path + "_audio.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(audioVariance);        
        oos.close();
	}

}
