package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.Vector;

import nc.NCUtils;
import utils.MyByteBuffer;
import utils.ToolUtils;


@XStreamAlias("Subfile")
abstract class PartFile {
    @XStreamAsAttribute
    @XStreamAlias("subfileNo")
    int partNo;

    int pieceFileLen;

    @XStreamAlias("subfileLen")
    int partFileLen;

    Vector<int[]> coefMatrix = new Vector<>();


    @XStreamOmitField
    int K;

    @XStreamOmitField
    String partFilePath;

    @XStreamOmitField
    String pieceFilePath;  //用来存储片文件

    @XStreamOmitField
    String reencodeFilePath; //用来存储再编码文件

    @XStreamOmitField
    String AndroidId;

    public PartFile() {
    }

    //初始化系数矩阵
    private void initCoefMatrix() {
        //系数矩阵为一个单位矩阵
        for (int i = 1; i <= K; i++) {
            //写入系数矩阵
            int[] intCoef = new int[K];
            intCoef[i - 1] = 1;
            coefMatrix.add(intCoef);
        }
    }


    //创建文件存储路径
    private void createCachePath(String folderPath) {
        this.partFilePath = ToolUtils.createFolder(folderPath, partNo + "");
        this.pieceFilePath = ToolUtils.createFolder(partFilePath, "pieceFilePath");
        this.reencodeFilePath = ToolUtils.createFolder(partFilePath, "reencodeFilePath");
    }

    public void recoverOmitField(String folderPath, int K, String AndroidId) {
        this.K = K;
        this.partFilePath = folderPath + File.separator + partNo;
        this.pieceFilePath = partFilePath + File.separator + "pieceFilePath";
        this.reencodeFilePath = partFilePath + File.separator + "reencodeFilePath";
        this.AndroidId = AndroidId;
    }

    //从文件的第几个字节开始读取到第几个字节
    public void initPartFile(String folderPath, int partNo, File file, int startPos, int len, int K, String AndroidId) {
        this.partNo = partNo;
        this.pieceFileLen = (len % K == 0 ? len / K : (len / K + 1)) + (1 + K); //记得加上系数矩阵的长度
        this.K = K;
        this.partFileLen = len;
        this.AndroidId = AndroidId;

        createCachePath(folderPath);
        initCoefMatrix();
        try {
            RandomAccessFile af = new RandomAccessFile(file, "r");
            af.seek(startPos);
            byte[] bytes = new byte[4096];
            //分为K个文件写入
            for (int i = 1; i <= K; i++) {
                //单位矩阵作为系数
                byte[] coef = new byte[K + 1];
                coef[0] = (byte) K;
                coef[i] = 1;
                //写入文件
                String randFileName = partNo + "_" + ToolUtils.randomString(5) + ".nc";

                File pieceFile = ToolUtils.createFile(pieceFilePath, randFileName);
                FileOutputStream fos = new FileOutputStream(pieceFile, true);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(coef);
                //需要读取的字节数
                int readLen = pieceFileLen - 1 - K;
                //本次读的字节数
                int readBytes;
                while (readLen != 0 && len != 0) {
                    readBytes = readLen > 4096 ? 4096 : readLen;
                    if (readBytes > len) {
                        readBytes = len;
                    }
                    af.readFully(bytes, 0, readBytes);
                    bos.write(bytes, 0, readBytes);

                    readLen -= readBytes;
                    len -= readBytes;
                }
                //补充不够的0部分
                if (len == 0 && readLen != 0) {
                    byte[] bs = new byte[readLen];
                    bos.write(bs);
                    //_0Num = readLen;
                }
                bos.close();
                fos.close();
            }
            af.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //解码  恢复出这部分文件
    public File recoverPartFile() {
        //写入文件
        String filePath = partFilePath + File.separator + partNo + ".org";

        File orgFile = new File(filePath);
        if (orgFile.exists()) {
            return orgFile;
        }

        Vector<File> files = ToolUtils.getUnderFiles(pieceFilePath);
        if (files.size() < K) {
            return null;
        }

        return encodeCore(NCUtils.DECODE_METHOD_NAME,
                pieceFileLen * K, filePath, Encode_Core_Mode.RECOVER_MODE);
    }

    //再编码功能
    public File reencodePartFile() {
        Vector<File> files = ToolUtils.getUnderFiles(pieceFilePath);
        if (files.size() == 1) {
            return files.get(0);
        } else if (files.size() == 0) {
            return null;
        }

        String filePath = reencodeFilePath + File.separator +
                (partNo + "_" + ToolUtils.randomString(5) + ".nc");
        return encodeCore(NCUtils.REENCODE_METHOD_NAME,
                pieceFileLen, filePath, Encode_Core_Mode.REENCODE_MODE);
    }


    private File encodeCore(String NCUtilsMethodsName, int retLen, String filePath, Encode_Core_Mode encode_core_mode) {
        Vector<File> files = ToolUtils.getUnderFiles(pieceFilePath);
        byte[] result = null;
        byte[] buffer = null;
        try {
            int size = files.size();
            if (size > K) size = K;
            buffer = MyByteBuffer.getBuffer(pieceFileLen * size);
            //byte[] buffer = new byte[len];
            for (int i = 0; i < size; i++) {
                File file = files.get(i);
                RandomAccessFile af = new RandomAccessFile(file, "r");
                //读取整个文件放在buffer字节数组中
                af.readFully(buffer, i * pieceFileLen, pieceFileLen);
                af.close();
            }
            //解码   带有K + 单位矩阵 的数据
            result = MyByteBuffer.getBuffer(retLen);

            //
            //NCUtils.decode(buffer, K, pieceFileLen,result);
            //反射
            String className = NCUtils.CLASS_NAME; //这里注意了，是：包名.类名，只写类名会出问题的哦
            Class<?> testClass = Class.forName(className);
            //byte[] encodeData, int row, int col, byte[] result
            Method saddMethod2 = testClass.getMethod(NCUtilsMethodsName,
                    byte[].class, int.class, int.class, byte[].class);
            saddMethod2.invoke(null, buffer, K, pieceFileLen, result);

            //
            File retFile = ToolUtils.createFile(filePath);
            RandomAccessFile af = new RandomAccessFile(retFile, "rw");

            //写入文件方式分为两种
            // 1 恢复文件写入方式
            if (encode_core_mode == Encode_Core_Mode.RECOVER_MODE) {
                int realLen = partFileLen;
                for (int i = 0; i < K; i++) {
                    //为了去除预处理文件时补充的零
                    int writeLen = pieceFileLen - 1 - K;
                    if (writeLen > realLen) {
                        writeLen = realLen;
                    }
                    af.write(result, i * pieceFileLen + 1 + K, writeLen);
                    realLen -= writeLen;
                }
            } else if (encode_core_mode == Encode_Core_Mode.REENCODE_MODE) {
                //再编码文件写入方式  一定要写清操作的数据范围
                af.write(result, 0, retLen);
            }

            af.close();
            return retFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            //释放buffer
            MyByteBuffer.releaseBuffer(buffer);
            MyByteBuffer.releaseBuffer(result);
        }
    }

    public abstract File getSendFile();
}

//encodeCore方法运行的两种方式
enum Encode_Core_Mode {
    RECOVER_MODE, REENCODE_MODE
}