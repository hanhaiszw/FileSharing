package utils;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

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
        return createFolder(folderPath);
    }
    public static String createFolder(String folderPath) {
        File tempFolder = new File(folderPath);
        if (!tempFolder.exists()) {
            //若不存在，则创建
            tempFolder.mkdir();
        }

        return folderPath;
    }

    //判断文件是否存在
    public static boolean isFileExists(String path, String fileName){
        File myFile = new File(path + File.separator + fileName);
        return myFile.exists();
    }

    public static File createFile(String path, String fileName){
        return createFile(path+File.separator+fileName);
    }
    public static File createFile(String filePath){
        File myFile = new File(filePath);
        if(!myFile.exists()){
            try {
                myFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return myFile;
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
    public static void writeToFile(String path, String fileName, byte[] inputData){
        writeToFile(path, fileName,inputData,inputData.length,false);
    }


    //读取文件
    public static byte[] readFile(String filePath){
        File file = new File(filePath);
        if(!file.exists()){
            return null;
        }
        byte[] buffer = null;
        try {
            RandomAccessFile af = new RandomAccessFile(file.getAbsoluteFile(), "r");
            buffer = new byte[(int) file.length()];
            //读取整个文件放在buffer字节数组中
            af.readFully(buffer);
            af.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return buffer;
    }


    /**
     * 获取指定目录下文件
     */
    public static Vector<File> getUnderFiles(String folderPath) {
        File directory = new File(folderPath);

        Vector<File> fileList = new Vector<>();
        if (directory.exists() && directory.isDirectory()) {
            File[] fileArr = directory.listFiles();
            for (int i = 0; i < fileArr.length; ++i) {
                File fileOne = fileArr[i];
                if (fileOne.isFile()) {
                    fileList.add(fileOne);
                }
            }
        }

        return fileList;
    }


    /**
     * 合并文件
     *
     * @param outFile 输出路径,含文件名
     * @param files   需要合并的文件路径 可是File[]，也可是String[]
     */
    public static final int BUFSIZE = 1024 * 8;

    public static void mergeFiles(String outFile, File[] files) {
        FileChannel outChannel = null;
        try {
            outChannel = new FileOutputStream(outFile).getChannel();
            for (File f : files) {
                FileChannel fc = new FileInputStream(f).getChannel();
                ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);
                while (fc.read(bb) != -1) {
                    bb.flip();
                    outChannel.write(bb);
                    bb.clear();
                }
                fc.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException ignore) {
            }
        }
    }


    /**
     * copy文件
     * @param source
     * @param target
     */
    public static void copyFile(File source, File target) {
        try {
            FileInputStream inStream = new FileInputStream(source);
            FileOutputStream outStream = new FileOutputStream(target);
            FileChannel in = inStream.getChannel();
            FileChannel out = outStream.getChannel();
            in.transferTo(0, in.size(), out);

            inStream.close();
            in.close();
            outStream.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //删除文件或文件夹
    public static void deleteFile(String filePath){
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();// 删除
        }
    }
    public static void deleteFile(File file){
        deleteFile(file.getPath());
    }

    public static void deleteDir(String folderPath){
        com.example.zpc.file.util.Utils.deleteDir(folderPath);
    }
    public static void deleteDir(File folder){
        deleteDir(folder.getPath());
    }
}
