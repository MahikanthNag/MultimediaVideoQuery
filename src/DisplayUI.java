import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class DisplayUI extends Frame implements ActionListener {    
	Button playVideo;
	Button playQueryVideo;
	Button pauseVideo;
	Button pauseQueryVideo;
	Button stopVideo;
	Button stopQueryVideo;
    
	Panel videoOps;
	Panel queryVideoOps;
	Panel videoPanel;
	Panel queryVideoPanel;
	List<BufferedImage> images;
	List<BufferedImage> queryImages;
	
	AudioPlayer audio;
	AudioPlayer queryAudio;
	
	JLabel videoLabel;
	JLabel queryLabel;
	Label queryVideoName;
	
	ImageIcon videoIcon;
	ImageIcon queryIcon;
	
	Panel rootPanel;
	Panel gridPanel;
	
	Thread videoThread;
	Thread audioThread;
	Thread queryVideoThread;
	Thread queryAudioThread;
	
	int currentFrame;
	int currentQueryFrame;
	int currentAudioFrame;
	int currentQueryAudioFrame;	
	
	int videoState;
	int queryVideoState;
	
	long start1;
	long start2;
	
	
	DisplayUI() throws IOException {		
		rootPanel = new Panel();
		
		playVideo = new Button("Play");
		playQueryVideo = new Button("Play");
		pauseVideo = new Button("Pause");
		pauseQueryVideo = new Button("Pause");
		stopVideo = new Button("Stop");
		stopQueryVideo = new Button("Stop");
		
		playVideo.addActionListener(this);
		playQueryVideo.addActionListener(this);
		pauseVideo.addActionListener(this);
		pauseQueryVideo.addActionListener(this);
		stopVideo.addActionListener(this);
		stopQueryVideo.addActionListener(this);
		
		videoOps = new Panel();
		queryVideoOps = new Panel();
		videoPanel = new Panel();
		queryVideoPanel = new Panel();
		
		// TODO : Need to change these initial frames
		BufferedImage initialDisplayImageFrame = getBufferedImageFromFile(new File("/Users/mahikanthnag/Downloads/database_videos/flowers/flowers001.rgb"));
		videoIcon = new ImageIcon(initialDisplayImageFrame);
		queryIcon = new ImageIcon(initialDisplayImageFrame);
		videoLabel = new JLabel();
		queryLabel = new JLabel();
		videoLabel.setIcon(videoIcon);
		queryLabel.setIcon(queryIcon);
		videoPanel.add(videoLabel);
		queryVideoPanel.add(queryLabel);
				
		gridPanel = new Panel(new GridLayout(2, 2));
		
		Panel queryOpsPanel = new Panel();
		queryOpsPanel.setLayout(new GridLayout(2, 2));
		queryOpsPanel.add(playQueryVideo);
		queryOpsPanel.add(pauseQueryVideo);
		queryOpsPanel.add(stopQueryVideo);
		Panel queryVideoNamePanel = new Panel();
		queryVideoName = new Label(Constants.QUERY_VIDEO_NAME);
		queryVideoNamePanel.add(queryVideoName);
		queryOpsPanel.add(queryVideoNamePanel);
				
		
		Panel opsPanel = new Panel();
		opsPanel.setLayout(new GridLayout(2, 2));
		opsPanel.add(playVideo);
		opsPanel.add(pauseVideo);
		opsPanel.add(stopVideo);

		gridPanel.add(videoPanel);
		gridPanel.add(queryVideoPanel);
		gridPanel.add(opsPanel);
		gridPanel.add(queryOpsPanel);
		
		rootPanel.add(gridPanel);
//		rootPanel.add( new JLabel(Constants.QUERY_VIDEO_NAME));		
		
		add(rootPanel, BorderLayout.SOUTH);
		
		images = new ArrayList<>();
		queryImages = new ArrayList<>();
		
		setupVideo(Constants.DB_VIDEO_NAME);
		setupQueryVideo(Constants.QUERY_VIDEO_NAME);
	}
	
	private String getFileNameSuffix(int num) {
		if(num < 10) {
			return "00";
		}
		else if(num < 100) {
			return "0";
		}
		else {
			return "";
		}
	}
	
	private BufferedImage getBufferedImageFromFile(File file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(0);
		byte[] bytes = new byte[Constants.HEIGHT * Constants.WIDTH * 3];
		raf.read(bytes);
		raf.close();
		
		BufferedImage img = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);
		int ind = 0;
		for(int y = 0; y < Constants.HEIGHT; y++) {
			for(int x = 0; x < Constants.WIDTH; x++) {					
				int r = bytes[ind];
				int g = bytes[ind + Constants.HEIGHT * Constants.WIDTH];
				int b = bytes[ind + Constants.HEIGHT * Constants.WIDTH * 2];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				img.setRGB(x,y,pix);
				ind++;
			}
		}
		return img;
	}
	
	public void setupVideo(String path) throws IOException {
		for(int i = 0; i < 600; i++) {
			File file = new File(Constants.BASE_DB_VIDEO_PATH + path + "/" + path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb");
			BufferedImage img = getBufferedImageFromFile(file);
			images.add(img);
		}
		audio = new AudioPlayer();
		audio.play(Constants.BASE_DB_VIDEO_PATH + path + "/" + path + ".wav");
	}
	
	public void setupQueryVideo(String path) throws IOException {
		for(int i = 0; i < 150; i++) {
			File file = new File(Constants.BASE_QUERY_VIDEO_PATH + path + "/" + path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb");
			BufferedImage img = getBufferedImageFromFile(file);
			queryImages.add(img);
		}
		queryAudio = new AudioPlayer();
		queryAudio.play(Constants.BASE_QUERY_VIDEO_PATH + path + "/" + path + ".wav");
	}
	
	public void playVideo() {
		audioThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					audio.audioClip.setFramePosition(currentAudioFrame);
					audio.audioClip.start();
//					try {
////						Thread.sleep(20000 / audio.audioFrameSize);
//					} catch (InterruptedException e) {						
//						// TODO : Think what to write here
//						// e.printStackTrace();
//						currentAudioFrame = audio.audioClip.getFramePosition();
//						return;
//					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}				
			}
		});		
		videoThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = currentFrame; i < 600; i++) {
					start1 = System.currentTimeMillis();
					updateFrameInVideoAndRepaint(videoIcon, videoLabel, videoPanel, gridPanel, i, 0, 0);
					long diff = System.currentTimeMillis() - start1;					
			        try {
			        	// Subtracted 2 just to roughly synch up audio and video output without
			        	// considerable delay.
						Thread.sleep(1000 / Constants.FRAME_RATE - diff - 2);
					} catch (InterruptedException e) {						
						// TODO : Think what to write here
						currentFrame = i;
						break;
					}
				}
				if(videoState == Constants.PLAY) {
					videoState = Constants.STOP;
					currentFrame = 0;
					currentAudioFrame = 0;
				}
			}
		});
		videoThread.start();
		audioThread.start();
	}
	
	
	public void playQueryVideo() {
		queryAudioThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					queryAudio.audioClip.setFramePosition(currentQueryAudioFrame);
					queryAudio.audioClip.start();
				}
				catch(Exception e) {
					e.printStackTrace();
				}				
			}
		});
		
		queryVideoThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(int i = currentQueryFrame; i < 150; i++) {
					start1 = System.currentTimeMillis();
					updateFrameInVideoAndRepaint(queryIcon, queryLabel, queryVideoPanel, gridPanel, i, 1, 1);					
					long diff = System.currentTimeMillis() - start1;
			        try {
						Thread.sleep(1000 / Constants.FRAME_RATE - diff);
					} catch (InterruptedException e) {						
						currentQueryFrame = i;
						break;
						
					}
				}
				if(queryVideoState == Constants.PLAY) {
					queryVideoState = Constants.STOP;
					currentQueryFrame = 0;
					currentQueryAudioFrame = 0;
				}
			}
		});
		queryVideoThread.start();
		queryAudioThread.start();
	}
	
	public void display() {
		pack();
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == playVideo) {
			if(videoState != Constants.PLAY) {
				try {					
					videoState = Constants.PLAY;
					playVideo();
				} catch (Exception e1) {
					e1.printStackTrace();
				}	
			}			
		}
		else if(e.getSource() == pauseVideo) {
			if(videoState == Constants.PLAY) {
				videoState = Constants.PAUSE;
				if(videoThread != null && audioThread != null) {
					videoThread.interrupt();
					audioThread.interrupt();
					currentAudioFrame = audio.audioClip.getFramePosition();
					audio.audioClip.stop();
				}				
			}			
		}
		else if(e.getSource() == stopVideo && videoState != Constants.STOP) {
			videoState = Constants.STOP;
			if(videoThread != null && audioThread != null) {
				videoThread.interrupt();
				audioThread.interrupt();				
				audio.audioClip.stop();
				currentFrame = 0;
				currentAudioFrame = 0;
				audio.audioClip.setFramePosition(0);
				updateFrameInVideoAndRepaint(videoIcon, videoLabel, videoPanel, gridPanel, currentFrame, 0, 0);
			}
			
		}
		else if(e.getSource() == playQueryVideo) {
			if(queryVideoState != Constants.PLAY) {
				try {				
					playQueryVideo();
					queryVideoState = Constants.PLAY;
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
		else if(e.getSource() == pauseQueryVideo) {
			if(queryVideoState == Constants.PLAY) {
				queryVideoState = Constants.PAUSE;
				if(queryVideoThread != null && queryAudioThread != null) {
					queryVideoThread.interrupt();
					queryAudioThread.interrupt();
					currentQueryAudioFrame = queryAudio.audioClip.getFramePosition();
					queryAudio.audioClip.stop();
				}				
			}
		}
		else if(e.getSource() == stopQueryVideo && queryVideoState != Constants.STOP) {
			queryVideoState = Constants.STOP;
			if(queryVideoThread != null && queryAudioThread != null) {
				queryVideoThread.interrupt();
				queryAudioThread.interrupt();				
				queryAudio.audioClip.stop();
				currentQueryFrame = 0;
				currentQueryAudioFrame = 0;
				queryAudio.audioClip.setFramePosition(0);
				updateFrameInVideoAndRepaint(queryIcon, queryLabel, queryVideoPanel, gridPanel, currentFrame, 1, 1);
			}
		}		
	}

	private void updateFrameInVideoAndRepaint(ImageIcon icon, JLabel label, Panel videoPanel, Panel gridPanel, int i, int index, int type) {
		BufferedImage image;
		if(type == 0) {
			image = images.get(i);			
		}
		else {
			image = queryImages.get(i);
		}
		icon.setImage(image);
		label.setIcon(icon);
		gridPanel.remove(videoPanel);
		videoPanel.remove(label);
		videoPanel.add(label, 0);
		gridPanel.add(videoPanel, index);
		repaint();
		revalidate();
	}

	public static void main(String[] args) throws ClassNotFoundException {
		try {
			ContrastStatistics contrastStatistics = new ContrastStatistics();
			contrastStatistics.calculateStats("flowers", "interview");
			DisplayUI ui = new DisplayUI();
			ui.display();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
