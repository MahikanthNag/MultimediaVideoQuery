import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class DisplayUI extends Frame implements ActionListener, ChangeListener, ListSelectionListener {
	private static HashMap<String, HashMap<Integer, Double>> contrastMap;
	private static HashMap<String, HashMap<Integer, Double>> motionMap;
	private static Map<String, HashMap<Integer, Double>> colorMap;
	Button playVideo;
	Button playQueryVideo;
	Button pauseVideo;
	Button pauseQueryVideo;
	Button stopVideo;
	Button stopQueryVideo;

	Panel sliderPanel;
	JSlider slider;

	Panel listPanel;
	JList listView;

	String currentdbVideoName;

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
	JPanel gridPanel;

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

	HashMap<Integer, Double> aggregatedGraphMap;

	DefaultCategoryDataset dataSet;
	ContrastStatistics contrastStatistics = new ContrastStatistics();
	Map<String, Double> contrastSimilarity;

	AudioSemantics audioSemantics = new AudioSemantics();
	Map<String, Double> audioSimilarity;

	DominantColors dominantColors = new DominantColors();
	Map<String, Double> colorSimilarity;

	MotionStatistics motionStatistics = new MotionStatistics();
	Map<String, Double> motionSimilarity;

	Map<String, Double> aggregateRankingMap = new TreeMap<>();

	DisplayUI() throws IOException {
		aggregatedGraphMap = new HashMap<>();
		initializeFrameMap(aggregatedGraphMap);

		rootPanel = new Panel(new BorderLayout());

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

		currentdbVideoName = Constants.DB_VIDEO_NAME;

		BufferedImage initialDisplayImageFrame = getBufferedImageFromFile(
				new File(Constants.BASE_DB_VIDEO_PATH + currentdbVideoName + "/" + currentdbVideoName + "001.rgb"));
		BufferedImage initialDisplayImageFrameForQuery = getBufferedImageFromFile(
				new File(Constants.BASE_QUERY_VIDEO_PATH + Constants.QUERY_VIDEO_NAME + "/" + Constants.QUERY_VIDEO_NAME
						+ "_001.rgb"));

		videoIcon = new ImageIcon(initialDisplayImageFrame);
		queryIcon = new ImageIcon(initialDisplayImageFrameForQuery);
		videoLabel = new JLabel();
		queryLabel = new JLabel();
		videoLabel.setIcon(videoIcon);
		queryLabel.setIcon(queryIcon);
		videoPanel.add(videoLabel);
		queryVideoPanel.add(queryLabel);

		gridPanel = new JPanel(new BorderLayout());

		Panel queryOpsPanel = new Panel(new BorderLayout());
		queryOpsPanel.setLayout(new BorderLayout());
		queryOpsPanel.add(playQueryVideo, "East");
		queryOpsPanel.add(pauseQueryVideo, "Center");
		queryOpsPanel.add(stopQueryVideo, "West");
//		Panel queryVideoNamePanel = new Panel();
		queryVideoName = new Label(Constants.QUERY_VIDEO_NAME);
//		queryVideoNamePanel.add(queryVideoName);
//		queryOpsPanel.add(queryVideoNamePanel);

		Panel opsPanel = new Panel(new BorderLayout());
		opsPanel.setLayout(new BorderLayout());
		opsPanel.add(playVideo, "East");
		opsPanel.add(pauseVideo, "Center");
		opsPanel.add(stopVideo, "West");
		opsPanel.setSize(100, 100);

		gridPanel.add(videoPanel, "East");
		gridPanel.add(queryVideoPanel, "West");

		Panel opsAndGraph = new Panel(new BorderLayout());
		Panel opsParentPanel = new Panel(new BorderLayout());
		opsParentPanel.add(opsPanel, "East");
		opsParentPanel.add(queryOpsPanel, "West");
//		rootPanel.add(opsParentPanel, "South");

		dataSet = new DefaultCategoryDataset();
		Panel graphParentPanel = new Panel(new BorderLayout());
		graphParentPanel.add(drawGraph(dataSet), "East");

		sliderPanel = new Panel(new BorderLayout());
		slider = new JSlider(0, 599, 0);
		slider.setPreferredSize(new Dimension(354, 20));
		slider.setPaintTrack(true);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.addChangeListener(this);
//		sliderPanel.setPreferredSize(new Dimension(354, 20));
		sliderPanel.add(slider, "East");

		Panel listParentPanel = new Panel(new BorderLayout());

		listPanel = new Panel();
		listView = new JList<String>();
		listView.addListSelectionListener(this);
		listPanel.add(listView);
		listParentPanel.add(listPanel, "East");
		sliderPanel.add(listParentPanel, "South");

		graphParentPanel.add(sliderPanel, "South");

		opsAndGraph.add(opsParentPanel, "North");
		opsAndGraph.add(graphParentPanel, "South");
		rootPanel.add(opsAndGraph, "South");

		rootPanel.add(gridPanel);
//		rootPanel.add( new JLabel(Constants.QUERY_VIDEO_NAME));

		add(rootPanel, BorderLayout.SOUTH);

		images = new ArrayList<>();
		queryImages = new ArrayList<>();

		setupVideo(currentdbVideoName);
		setupQueryVideo(Constants.QUERY_VIDEO_NAME);
	}

	private void initializeFrameMap(Map<Integer, Double> map) {
		for (int i = 0; i < 600; i++) {
			map.put(i, 0.0);
		}
	}

	private ChartPanel drawGraph(DefaultCategoryDataset dataSet) {
		JFreeChart lineChart = ChartFactory.createLineChart("", "", "", dataSet, PlotOrientation.VERTICAL, true, true,
				false);

		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new Dimension(354, 100));

		return chartPanel;
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

	private BufferedImage getBufferedImageFromFile(File file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(0);
		byte[] bytes = new byte[Constants.HEIGHT * Constants.WIDTH * 3];
		raf.read(bytes);
		raf.close();

		BufferedImage img = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);
		int ind = 0;
		for (int y = 0; y < Constants.HEIGHT; y++) {
			for (int x = 0; x < Constants.WIDTH; x++) {
				int r = bytes[ind];
				int g = bytes[ind + Constants.HEIGHT * Constants.WIDTH];
				int b = bytes[ind + Constants.HEIGHT * Constants.WIDTH * 2];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				img.setRGB(x, y, pix);
				ind++;
			}
		}
		return img;
	}

	public void setupVideo(String path) throws IOException {
		currentFrame = 0;
		currentAudioFrame = 0;
		images = new ArrayList<>();
		for (int i = 0; i < 600; i++) {
			File file = new File(Constants.BASE_DB_VIDEO_PATH + path + "/" + path
					+ getFileNameSuffix(i + 1, (Constants.BASE_DB_VIDEO_PATH + path + "/" + path)) + (i + 1) + ".rgb");

			BufferedImage img = getBufferedImageFromFile(file);
			images.add(img);
		}
		audio = new AudioPlayer();
		audio.play(Constants.BASE_DB_VIDEO_PATH + path + "/" + path + ".wav");
	}

	public void setupQueryVideo(String path) throws IOException {
		currentQueryFrame = 0;
		currentQueryAudioFrame = 0;
		queryImages = new ArrayList<>();
		for (int i = 0; i < 150; i++) {
			File file = new File(Constants.BASE_QUERY_VIDEO_PATH + path + "/" + path
					+ getFileNameSuffix(i + 1, Constants.BASE_QUERY_VIDEO_PATH + path + "/" + path) + (i + 1) + ".rgb");
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		videoThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = currentFrame; i < 600; i++) {
					start1 = System.currentTimeMillis();
					updateFrameInVideoAndRepaint(videoIcon, videoLabel, videoPanel, gridPanel, i, 0, 1);
					long diff = System.currentTimeMillis() - start1;

					try {
						// Subtracted 2 just to roughly synch up audio and video output without
						// considerable delay.
						Thread.sleep(Math.max(0, 1000 / Constants.FRAME_RATE - diff - 2));
					} catch (InterruptedException e) {
						// TODO : Think what to write here
						currentFrame = i;
						break;
					}
				}
				if (videoState == Constants.PLAY) {
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		queryVideoThread = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = currentQueryFrame; i < 150; i++) {
					start1 = System.currentTimeMillis();
					updateFrameInVideoAndRepaint(queryIcon, queryLabel, queryVideoPanel, gridPanel, i, 1, 0);
					long diff = System.currentTimeMillis() - start1;
					try {
						Thread.sleep(1000 / Constants.FRAME_RATE - diff);
					} catch (InterruptedException e) {
						currentQueryFrame = i;
						break;

					}
				}
				if (queryVideoState == Constants.PLAY) {
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
		if (e.getSource() == playVideo) {
			if (videoState != Constants.PLAY) {
				try {
					videoState = Constants.PLAY;
					playVideo();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		} else if (e.getSource() == pauseVideo) {
			if (videoState == Constants.PLAY) {
				videoState = Constants.PAUSE;
				if (videoThread != null && audioThread != null) {
					videoThread.interrupt();
					audioThread.interrupt();
					currentAudioFrame = audio.audioClip.getFramePosition();
					audio.audioClip.stop();
				}
			}
		} else if (e.getSource() == stopVideo && videoState != Constants.STOP) {
			videoState = Constants.STOP;
			if (videoThread != null && audioThread != null) {
				videoThread.interrupt();
				audioThread.interrupt();
				audio.audioClip.stop();
				currentFrame = 0;
				currentAudioFrame = 0;
				audio.audioClip.setFramePosition(0);
				updateFrameInVideoAndRepaint(videoIcon, videoLabel, videoPanel, gridPanel, currentFrame, 0, 1);
			}

		} else if (e.getSource() == playQueryVideo) {
			if (queryVideoState != Constants.PLAY) {
				try {
					queryVideoState = Constants.PLAY;
					playQueryVideo();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		} else if (e.getSource() == pauseQueryVideo) {
			if (queryVideoState == Constants.PLAY) {
				queryVideoState = Constants.PAUSE;
				if (queryVideoThread != null && queryAudioThread != null) {
					queryVideoThread.interrupt();
					queryAudioThread.interrupt();
					currentQueryAudioFrame = queryAudio.audioClip.getFramePosition();
					queryAudio.audioClip.stop();
				}
			}
		} else if (e.getSource() == stopQueryVideo && queryVideoState != Constants.STOP) {
			queryVideoState = Constants.STOP;
			if (queryVideoThread != null && queryAudioThread != null) {
				queryVideoThread.interrupt();
				queryAudioThread.interrupt();
				queryAudio.audioClip.stop();
				currentQueryFrame = 0;
				currentQueryAudioFrame = 0;
				queryAudio.audioClip.setFramePosition(0);
				updateFrameInVideoAndRepaint(queryIcon, queryLabel, queryVideoPanel, gridPanel, currentQueryFrame, 1,
						0);
			}
		}
	}

	private void updateFrameInVideoAndRepaint(ImageIcon icon, JLabel label, Panel videoPanel, JPanel gridPanel, int i,
			int index, int type) {
		BufferedImage image;
		if (type == 1) {
			image = images.get(i);
		} else {
			image = queryImages.get(i);
		}
		icon.setImage(image);
		label.setIcon(icon);
		gridPanel.remove(videoPanel);
		videoPanel.remove(label);
		videoPanel.add(label, 0);
		gridPanel.add(videoPanel, index);
		repaint();
//		revalidate();
	}

	public static void main(String[] args) throws ClassNotFoundException {
		try {
			DisplayUI ui = new DisplayUI();

			ui.contrastSimilarity = ui.contrastStatistics.calculateStatsOfAllPairs(Constants.QUERY_VIDEO_NAME);
			contrastMap = ui.contrastStatistics.getGraphMappingForAllVideos(Constants.QUERY_VIDEO_NAME);
			System.out.println("After contrast");

			ui.audioSimilarity = ui.audioSemantics.calculateStatsOfAllPairs(Constants.QUERY_VIDEO_NAME);
			System.out.println("After audio");

			ui.colorSimilarity = ui.dominantColors.calculateStatsOfAllPairs(Constants.QUERY_VIDEO_NAME);
			colorMap = ui.dominantColors.videoWiseFrameSimilarity;
			System.out.println("After color");
//
//			ui.motionSimilarity = ui.motionStatistics.calculateStatsOfAllPairs(Constants.QUERY_VIDEO_NAME);
//			motionMap = ui.motionStatistics.getGraphMappingForAllVideos(Constants.QUERY_VIDEO_NAME);
//			System.out.println("After motion");

			ui.totalSimilarity("flowers");
			ui.totalSimilarity("interview");
			ui.totalSimilarity("movie");
			ui.totalSimilarity("musicvideo");
			ui.totalSimilarity("sports");
			ui.totalSimilarity("starcraft");
			ui.totalSimilarity("traffic");

			ui.aggregateRankingMap = sortByValues(ui.aggregateRankingMap);

			ui.populateDataSet();

			ui.printResults();
			ui.display();
//			ui.aggregateRankingMap.put("flowers", 1.0);
//			ui.aggregateRankingMap.put("interview", 1.0);
//			ui.aggregateRankingMap.put("sports", 1.0);
			ui.initializePanel();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void populateDataSet() {

		HashMap<Integer, Double> selectedContrastMap = contrastMap.get(currentdbVideoName);
		HashMap<Integer, Double> selectedColorMap = colorMap.get(currentdbVideoName);
//		HashMap<Integer, Double> selectedMotionMap = motionMap.get(currentdbVideoName);

//		aggregatedGraphMap.forEach((k, v) -> selectedContrastMap.merge(k, v, (v1, v2) -> v1 + v2));
		aggregatedGraphMap.forEach((k, v) -> selectedColorMap.merge(k, v, (v1, v2) -> v1 + v2));

		for (int i = 0; i < Constants.DB_VIDEO_FRAME_SIZE; i++) {
			dataSet.addValue(selectedContrastMap.get(i), "", i + "");
		}
	}

	public void totalSimilarity(String query) {
//		double totalSimilarity = contrastSimilarity.get(query) + audioSimilarity.get(query) + colorSimilarity.get(query)
//				+ motionSimilarity.get(query);
		double totalSimilarity = contrastSimilarity.get(query) + audioSimilarity.get(query);

		aggregateRankingMap.put(query, totalSimilarity);
	}

	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k1).compareTo(map.get(k2));
				if (compare == 0)
					return 1;
				else
					return -compare;
			}
		};
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}

	public void printResults() {
		for (Map.Entry<String, Double> entry : aggregateRankingMap.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
	}

	public void initializePanel() {
		String[] topMatches = new String[3];
		getTop3VideosArray(topMatches);

		listPanel.remove(listView);
		listView = new JList<String>(topMatches);
		listView.addListSelectionListener(this);
		listPanel.add(listView);
		repaint();
	}

	private void getTop3VideosArray(String[] topMatches) {
		int i = 0;
		for (Map.Entry<String, Double> entry : aggregateRankingMap.entrySet()) {
			if (i == 3)
				break;
			topMatches[i] = entry.getKey();
			i++;
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == slider) {

			videoState = Constants.PAUSE;
			if (videoThread != null && audioThread != null) {
				videoThread.interrupt();
				audioThread.interrupt();
				audio.audioClip.stop();
			}
			currentFrame = slider.getValue();
			currentAudioFrame = (int) ((currentFrame / (double) Constants.DB_VIDEO_FRAME_SIZE)
					* Constants.DB_AUDIO_FRAME_SIZE * 4);
			BufferedImage scrubbedScreenshot = null;
			try {
				scrubbedScreenshot = getBufferedImageFromFile(
						new File(Constants.BASE_DB_VIDEO_PATH + currentdbVideoName + "/" + currentdbVideoName
								+ getFileNameSuffix(currentFrame + 1,
										Constants.BASE_DB_VIDEO_PATH + currentdbVideoName + "/" + currentdbVideoName)
								+ (currentFrame + 1) + ".rgb"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			videoIcon = new ImageIcon(scrubbedScreenshot);
			videoLabel.setIcon(videoIcon);
			repaint();
//			revalidate();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == listView) {
			String selectedVideo = (String) listView.getSelectedValue();
			videoState = Constants.PAUSE;
			if (videoThread != null && audioThread != null) {
				videoThread.interrupt();
				audioThread.interrupt();
				audio.audioClip.stop();
			}
			try {
				currentdbVideoName = selectedVideo;
				populateDataSet();

				setupVideo(selectedVideo);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
}
