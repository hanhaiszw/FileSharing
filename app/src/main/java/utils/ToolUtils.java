package utils;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ToolUtils {
    private ToolUtils() {
    }

    private final static String seed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" + "0123456789";

    /**
     *
     * @param size 生成的随机字符串的长度
     * @return
     */
    public static String randomString(int size) {
        StringBuffer ret = new StringBuffer();
        int len = seed.length();
        for (int i = 0; i < size; i++) {
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
    public static String createFolder(String path, String folderName) {
        String folderPath = path + File.separator + folderName;
        File tempFolder = new File(folderPath);
        if (!tempFolder.exists()) {
            //若不存在，则创建
            tempFolder.mkdir();
        }

        return folderPath;
    }

    /**
     *
     * @param path
     * @param fileName
     * @param inputData
     * @param append  true:续写， false：覆盖写
     * @return
     */
    public static void writeToFile(String path, String fileName, byte[] inputData,int len,boolean append) {
        File myFile = new File(path + File.separator + fileName);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        if (!myFile.exists()) {   //不存在则创建
            try {
                myFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            //传递一个true参数，代表不覆盖已有的文件。并在已有文件的末尾处进行数据续写,false表示覆盖写
            //fos = new FileOutputStream(myFile);  //覆盖写
            fos = new FileOutputStream(myFile, append);  //续写
            bos = new BufferedOutputStream(fos);
            bos.write(inputData,0,len);
            bos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
