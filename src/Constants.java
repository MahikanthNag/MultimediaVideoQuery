public final class Constants {

	private Constants() {
		// restrict instantiation
	}

	public static final int HEIGHT = 288;
	public static final int WIDTH = 352;

	public static final int PLAY = 1;
	public static final int PAUSE = 2;
	public static final int STOP = 3;

	public static final String BASE_DB_VIDEO_PATH = "/Users/mahikanthnag/Downloads/database_videos/";
	public static final String BASE_QUERY_VIDEO_PATH = "/Users/mahikanthnag/Downloads/query/";

	public static final String QUERY_VIDEO_NAME = "HQ3";
	public static final String DB_VIDEO_NAME = "flowers";

	public static final double COLOR_PRIORITY = 0.25;
	public static final double AUDIO_PRIORITY = 0.25;
	public static final double MOTION_VECTOR_PRIORITY = 0.25;
	public static final double CONTRAST_PRIORITY = 0.25;

	public static final int AUDIO_FRAME_RATE = 44100;
	public static final int FRAME_RATE = 30;
	public static final int FRAME_CHUNK_SIZE = 150;
	public static final int DB_AUDIO_FRAME_SIZE = 221184;

	public static final int DB_VIDEO_FRAME_SIZE = 600;
	public static final int QUERY_VIDEO_FRAME_SIZE = 150;
}