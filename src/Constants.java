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
    
    public static final String QUERY_VIDEO_NAME = "first";
    public static final String DB_VIDEO_NAME = "movie";
    
    public static final double COLOR_PRIORITY = 0.25;
    public static final double AUDIO_PRIORITY = 0.25;
    public static final double MOTION_VECTOR_PRIORITY = 0.25;
    public static final double CONTRAST_PRIORITY = 0.25;
    
    public static final int FRAME_RATE = 30;
}