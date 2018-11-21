package cache;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import nc.NCUtils;
import utils.MyByteBuffer;
import utils.ToolUtils;

@XStreamAlias("Subfile")
class PartFile {
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

    //从文件的第几个字节开始读取到第几个字节
    public void initPartFile(String folderPath, int partNo, File file, int startPos, int len, int K) {
        this.partNo = partNo;
        this.partFilePath = ToolUtils.createFolder(folderPath, partNo + "");

        this.pieceFilePath = ToolUtils.createFolder(partFilePath, "pieceFilePath");

        this.pieceFileLen = (len % K == 0 ? len / K : (len / K + 1)) + (1 + K); //记得加上系数矩阵的长度
        this.K = K;
        this.partFileLen = len;
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
        File orgFile = new File(partFilePath, partNo + ".org");
        if(orgFile.exists()){
            return orgFile;
        }
        orgFile=ToolUtils.createFile(partFilePath, partNo + ".org");

        Vector<File> files = ToolUtils.getUnderFiles(pieceFilePath);
        if (files.size() < K) {
            return null;
        }
        //int len = pieceFileLen * K;
        byte[] result = null;
        byte[] buffer = null;
        try {
            buffer = MyByteBuffer.getBuffer();
            //byte[] buffer = new byte[len];
            for (int i = 0; i < K; i++) {
                File file = files.get(i);
                RandomAccessFile af = new RandomAccessFile(file, "r");
                //读取整个文件放在buffer字节数组中
                af.readFully(buffer, i * pieceFileLen, pieceFileLen);
                af.close();
            }
            //解码   带有K + 单位矩阵 的数据
            result = NCUtils.decode(buffer, K, pieceFileLen);

            RandomAccessFile af = new RandomAccessFile(orgFile.getAbsoluteFile(), "rw");
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
            //读取整个文件放在buffer字节数组中
            //af.write(originData, 0, partFileLen);
            af.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            MyByteBuffer.releaseBuffer(buffer);
            NCUtils.releaseBuffer(result);
        }


        return orgFile;
    }


}
