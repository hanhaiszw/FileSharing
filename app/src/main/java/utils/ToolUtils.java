package utils;

import android.os.Environment;

import java.io.File;

public class ToolUtils {
    private ToolUtils() {
    }

    private final static String seed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" + "0123456789";

    public static String randomString() {
        StringBuffer ret = new StringBuffer();
        int len = seed.length();
        for (int i = 0; i < 5; i++) {
            int index = (int) Math.round(Math.random() * (len - 1));
            ret.append(seed.charAt(index));
        }
        return ret.toString();
    }


    /**
     * 获取SDcard根路径
     *
     * @return
     */
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * 在指定目录下创建文件夹
     *
     * @param path       路径
     * @param folderName 文件夹名
     * @return 返回创建的文件夹路径
     */
    public static String creatFolder(String path, String folderName) {
        String folderPath = path + File.separator + folderName;
        File tempFolder = new File(folderPath);
        if (!tempFolder.exists()) {
            //若不存在，则创建
            tempFolder.mkdir();
        }

        return folderPath;
    }
}
