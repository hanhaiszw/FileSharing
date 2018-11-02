package data;

import utils.ToolUtils;

public class CachePath {
    public final static String APP_PATH;  //主目录
    public final static String TEMP_PATH;  //文件缓存目录
    public final static String CRASH_PATH; //程序异常信息目录
    public final static String LOG_PATH;   //文件接收时间等信息目录

    static {
        APP_PATH = ToolUtils.creatFolder(ToolUtils.getSDCardPath(), "0FileSharing");
        TEMP_PATH = ToolUtils.creatFolder(APP_PATH, "Temp");
        CRASH_PATH = ToolUtils.creatFolder(APP_PATH, "Crash");
        LOG_PATH = ToolUtils.creatFolder(APP_PATH, "Log");
    }

    private CachePath() {

    }
}
