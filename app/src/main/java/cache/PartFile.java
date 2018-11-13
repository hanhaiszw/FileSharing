package cache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import utils.ToolUtils;

class PartFile {
    int partNo;
    int K;
    String partFilePath;
    int pieceFileLen;

    Vector<int[]> coefMatrix;

    int _0Num; //文件末尾补充的0的个数

    public PartFile() {
        coefMatrix = new Vector<>();
    }

    private void initCoefMatrix(){
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
        this.pieceFileLen = len % K == 0 ? len / K : (len / K + 1);
        this.K = K;
        initCoefMatrix();
        try {
            RandomAccessFile af = new RandomAccessFile(file.getAbsoluteFile(), "r");
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

                File pieceFile = ToolUtils.createFile(partFilePath, randFileName);
                FileOutputStream fos = new FileOutputStream(pieceFile, true);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(coef);
                //需要读取的字节数
                int readLen = pieceFileLen;
                //本次读的字节数
                int readBytes;
                while (readLen != 0 && len != 0) {
                    readBytes = readLen > 4096 ? 4096 : readLen;
                    if(readBytes > len){
                        readBytes = len;
                    }
                    af.readFully(bytes, 0, readBytes);
                    bos.write(bytes, 0, readBytes);

                    readLen -= readBytes;
                    len -= readBytes;
                }
                //补充不够的0部分
                if(len == 0 && readLen != 0){
                    byte[] bs = new byte[readLen];
                    bos.write(bs);
                    _0Num = readLen;
                }
                bos.close();
                fos.close();
            }
            af.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recoverPartFile(){

    }
}
