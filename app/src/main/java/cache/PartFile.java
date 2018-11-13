package cache;

import java.io.File;
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

    public PartFile() {
        coefMatrix = new Vector<>();
    }

    //从文件的第几个字节开始读取到第几个字节
    public void initPartFile(String folderPath, int partNo, File file, int startPos, int len, int K) {
        this.partNo = partNo;
        this.partFilePath = ToolUtils.createFolder(folderPath, partNo + "");
        this.pieceFileLen = len % K == 0 ? len / K : (len / K + 1);
        this.K = K;
        try {
            RandomAccessFile af = new RandomAccessFile(file.getAbsoluteFile(), "r");
            af.seek(startPos);

            //分为K个文件写入
            for (int i = 1; i <= K; i++) {
                //写入系数矩阵
                int[] intCoef = new int[K];
                intCoef[i - 1] = 1;
                coefMatrix.add(intCoef);

                //单位矩阵作为系数
                byte[] coef = new byte[K + 1];
                coef[0] = (byte) K;
                coef[i] = 1;
                //写入文件
                String randFileName = partNo + "_" + ToolUtils.randomString(5) + ".nc";
                ToolUtils.writeToFile(partFilePath, randFileName,
                        coef, coef.length, true);

                int readLen = pieceFileLen;
                int readBytes;
                byte[] bytes = new byte[4096];
                //当最后一段不够长时，自动补全0
                while (readLen != 0) {
                    readBytes = readLen > 4096 ? 4096 : readLen;
                    af.read(bytes, 0, readBytes);
                    ToolUtils.writeToFile(partFilePath, randFileName,
                            bytes, readBytes, true);
                    readLen -= readBytes;
                }
            }
            af.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
