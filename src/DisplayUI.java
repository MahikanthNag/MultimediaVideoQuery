import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class DisplayUI extends Frame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;	
	
	static final int HEIGHT = 288;
    static final int WIDTH = 352;
    
    static final int PLAY = 1;
    static final int PAUSE = 2;
    static final int STOP = 3;
    
    String baseDBVideoPath = "/Users/mahikanthnag/Downloads/database_videos/";
    String baseQueryVideoPath = "/Users/mahikanthnag/Downloads/query/";
    
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
	
	PlaySound audio;
	PlaySound queryAudio;
	
	JLabel videoLabel;
	JLabel queryLabel;
	
	ImageIcon videoIcon;
	ImageIcon queryIcon;
	
	Panel rootPanel;
	
	int currentFrame;
	int currentQueryFrame;
	
	int frameRate = 30;
	
	int videoState;
	
	enum State{		
		PLAY(1), PAUSE(2), STOP(3);
		int state;
		
		private State(int state) {
			this.state = state;
		}
	}
	
	DisplayUI() throws FileNotFoundException, IOException {		
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
		
		rootPanel.add(videoPanel);
		rootPanel.add(queryVideoPanel);
		
		Panel queryOpsPanel = new Panel();
		queryOpsPanel.setLayout(new GridLayout(2, 2));
		queryOpsPanel.add(playQueryVideo);
		queryOpsPanel.add(pauseQueryVideo);
		queryOpsPanel.add(stopQueryVideo);
		
		rootPanel.add(queryOpsPanel);
		
		Panel opsPanel = new Panel();
		opsPanel.setLayout(new GridLayout(2, 2));
		opsPanel.add(playVideo);
		opsPanel.add(pauseVideo);
		opsPanel.add(stopVideo);

		rootPanel.add(opsPanel);
		add(rootPanel, BorderLayout.SOUTH);
		
		images = new ArrayList<>();
		queryImages = new ArrayList<>();
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
	
	private BufferedImage getBufferedImageFromFile(File file) throws FileNotFoundException, IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(0);
		byte[] bytes = new byte[HEIGHT * WIDTH * 3];
		raf.read(bytes);
		raf.close();
		
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		int ind = 0;
		for(int y = 0; y < HEIGHT; y++) {
			for(int x = 0; x < WIDTH; x++) {					
				int r = bytes[ind];
				int g = bytes[ind+HEIGHT*WIDTH];
				int b = bytes[ind+HEIGHT*WIDTH*2];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				img.setRGB(x,y,pix);
				ind++;
			}
		}
		return img;
	}
	
	public void setupVideo(String path) throws IOException {
		for(int i = 0; i < 600; i++) {
			File file = new File(baseDBVideoPath + path + "/" + path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb");
			BufferedImage img = getBufferedImageFromFile(file);
			images.add(img);
		}
		
		currentFrame = 0;
		audio = new PlaySound(new FileInputStream(baseDBVideoPath + path + "/" + path + ".wav"));
	}
	
	public void setupQueryVideo(String path) throws IOException {
		for(int i = 0; i < 150; i++) {
			File file = new File(baseQueryVideoPath + path + "/" + path + getFileNameSuffix(i + 1) + (i + 1) + ".rgb");
			BufferedImage img = getBufferedImageFromFile(file);
			queryImages.add(img);
		}
		
		currentQueryFrame = 0;
		queryAudio = new PlaySound(new FileInputStream(baseQueryVideoPath + path + "/" + path + ".wav"));
	}
	
	public void playVideo() throws FileNotFoundException {
//		audio = new PlaySound(new FileInputStream(baseDBVideoPath + path + "/" + path + ".wav"));
		Thread audioThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					audio.play();
				}
				catch(Exception e) {
					e.printStackTrace();
				}				
			}
		});
		
		Thread videoThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = currentFrame; i < 600; i++) {
					BufferedImage image = images.get(i);
					videoIcon.setImage(image);
					videoLabel.setIcon(videoIcon);
					rootPanel.remove(videoPanel);
//					videoPanel = new Panel();
//					videoPanel.add(videoLabel, 0);	
					rootPanel.add(videoPanel, 0);
					repaint();
					revalidate();
			        
			        try {
						Thread.sleep(1000 / frameRate);
					} catch (InterruptedException e) {						
						// TODO : Think what to write here
						e.printStackTrace();
					}
				}
				if(videoState == PLAY) {
					videoState = STOP;
					currentFrame = 0;
				}
			}
		});
		videoThread.start();
		audioThread.start();
	}
	
	
	public void playQueryVideo() throws FileNotFoundException {
//		queryAudio = new PlaySound(new FileInputStream(baseQueryVideoPath + path + "/" + path + ".wav"));
		Thread audioThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					queryAudio.play();
				}
				catch(Exception e) {
					
				}				
			}
		});
		
		Thread videoThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(int i = currentQueryFrame; i < 150; i++) {
					BufferedImage image = queryImages.get(i);
					videoIcon.setImage(image);								        
//			        setVisible(true);
			        
			        try {
						Thread.sleep(1000 / frameRate);
					} catch (InterruptedException e) {						
						// TODO : Think what to write here
						e.printStackTrace();
					}
				}
				if(videoState == PLAY) {
					videoState = STOP;
					currentFrame = 0;
				}
			}
		});
		videoThread.start();
		audioThread.start();
	}
	
	public void display() {
		pack();
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == playVideo) {
			try {
				setupVideo("flowers");
				playVideo();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e.getSource() == pauseVideo) {
			
		}
		else if(e.getSource() == stopVideo) {
			
		}
		else if(e.getSource() == playQueryVideo) {
			
		}
		else if(e.getSource() == pauseQueryVideo) {
			
		}
		else if(e.getSource() == stopQueryVideo) {
			
		}		
	}
	
	public static void main(String[] args) {
		try {
			DisplayUI ui = new DisplayUI();
			ui.display();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
