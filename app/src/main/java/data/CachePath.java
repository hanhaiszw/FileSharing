package data;

import utils.ToolUtils;

public class CachePath {
    public final static String APP_PATH;  //主目录
    public final static String TEMP_PATH;  //文件缓存目录
    public final static String CRASH_PATH; //程序异常信息目录
    public final static String LOG_PATH;   //文件接收时间等信息目录
    public final static String RECEIVE_TEMP_PATH; //接收文件暂存地址

    public final static String LOG_FILE_NAME = "log.txt";
    static {
        APP_PATH = ToolUtils.createFolder(ToolUtils.getSDCardPath(), "0FileSharing");
        TEMP_PATH = ToolUtils.createFolder(APP_PATH, "Temp");
        RECEIVE_TEMP_PATH = ToolUtils.createFolder(APP_PATH,"ReceiveTemp");
        CRASH_PATH = ToolUtils.createFolder(APP_PATH, "Crash");
        LOG_PATH = ToolUtils.createFolder(APP_PATH, "Log");
    }

    private CachePath() {

    }
}
