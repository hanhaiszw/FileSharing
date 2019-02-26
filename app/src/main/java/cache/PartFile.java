package cache;

import android.util.Log;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nc.NCUtils;
import utils.MyThreadPool;
import utils.ToolUtils;


@XStreamAlias("Subfile")
abstract class PartFile {
    @XStreamAsAttribute
    @XStreamAlias("subfileNo")
    int partNo;

    private int pieceFileLen;

    @XStreamAlias("subfileLen")
    private int partFileLen;

    private Vector<int[]> coefMatrix = new Vector<>();

    @XStreamOmitField
    private int K;

    @XStreamOmitField
    private String partFilePath;

    @XStreamOmitField
    private String pieceFilePath;  //用来存储片文件

    @XStreamOmitField
    private String reencodeFilePath; //用来存储再编码文件


    @XStreamOmitField
    String AndroidId;

    /**
     * 用以同步读写操作
     */
    @XStreamOmitField
    private ReentrantReadWriteLock readWriteLock;

    PartFile() {
        readWriteLock = new ReentrantReadWriteLock(true);
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


    // 只创建文件存储路径
    private void createCachePath(String folderPath) {
        this.partFilePath = ToolUtils.createFolder(folderPath, partNo + "");
        this.pieceFilePath = ToolUtils.createFolder(partFilePath, "pieceFilePath");
        this.reencodeFilePath = ToolUtils.createFolder(partFilePath, "reencodeFilePath");
    }

    // 只恢复隐藏的成员变量值
    public void recoverOmitField(String folderPath, int K, String AndroidId) {
        this.K = K;
        this.AndroidId = AndroidId;
        this.partFilePath = folderPath + File.separator + partNo;
        this.pieceFilePath = partFilePath + File.separator + "pieceFilePath";
        this.reencodeFilePath = partFilePath + File.separator + "reencodeFilePath";
        if(readWriteLock == null){
            readWriteLock = new ReentrantReadWriteLock(true);
        }
    }

    // 恢复隐藏的成员变量值，创建存储文件夹
    public void initPartFile(PartFile partFile, String folderPath) {
        this.partNo = partFile.partNo;
        this.pieceFileLen = partFile.pieceFileLen;
        this.partFileLen = partFile.partFileLen;
        this.K = partFile.K;
        this.AndroidId = partFile.AndroidId;

        //创建存储文件夹
        createCachePath(folderPath);
    }

    //从文件的第几个字节开始读取到第几个字节
    public void initPartFile(String folderPath, int partNo, int K, String AndroidId,
                             File file, int startPos, int len) {
        this.partNo = partNo;
        this.pieceFileLen = (len % K == 0 ? len / K : (len / K + 1)) + (1 + K); //记得加上系数矩阵的长度
        this.partFileLen = len;

        recoverOmitField(folderPath, K, AndroidId);
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
                }
                bos.close();
                fos.close();
            }
            af.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 重写再编码功能
     * 把文件分开载入编码
     * 再分别写入再编码文件
     *
     * @return
     */
    public File reencodePartFile() {
        Log.i("hanhai","再编码开始");

        Vector<File> files = null;
        try {
            readWriteLock.readLock().lock();
            files = ToolUtils.getUnderFiles(pieceFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
        }

        int fileSize = files.size();
        if (files.size() == 1) {
            return files.get(0);
        } else if (files.size() == 0) {
            return null;
        }

        // 分为1M 1M 编码
        int len = 1 * 1024 * 1024;
        if (fileSize > K) fileSize = K;
        //每次从每个文件中读取的长度
        int perLen = (int) Math.ceil((float) len / fileSize);
        // 一次处理的数据总长
        len = perLen * fileSize;
        // 可以对每个文件读取几次
        int parts = (int) Math.ceil((float) pieceFileLen / perLen);

        // 随机矩阵
        byte[] randomMatrix = new byte[fileSize];
        Random random = new Random();
        random.nextBytes(randomMatrix);

        // 一个存数据  一个存结果
        byte[] data = new byte[len];
        byte[] result = new byte[perLen];

        // 结果文件
        File retFile = ToolUtils.createFile(reencodeFilePath,
                partNo + "_" + ToolUtils.randomString(5) + ".nc");
        try {
            RandomAccessFile retAf = new RandomAccessFile(retFile, "rw");

            int restLen = pieceFileLen;
            for (int i = 0; i < parts; i++) {
                //可读取字节数
                int readLen = perLen > restLen ? restLen : perLen;
                int startPos = i * perLen;
                for (int j = 0; j < fileSize; j++) {
                    // 从一个文件中每次读取perLen长度的内容
                    RandomAccessFile af = new RandomAccessFile(files.get(j), "r");
                    af.seek(startPos);
                    int startData = j * readLen;
                    af.readFully(data, startData, readLen);
                    af.close();
                }
                // 再编码
                NCUtils.Multiply2(randomMatrix, 1, fileSize, data, fileSize, readLen, result);
                // 剩下的长度
                restLen -= readLen;
                // 将结果写入结果文件
                retAf.write(result, 0, readLen);
            }

            // 写入K值
            retAf.seek(0);
            retAf.writeByte(K);

            retAf.close();
        } catch (IOException e) {
            e.printStackTrace();
            ToolUtils.deleteFile(retFile);
            return null;
        }

        Log.i("hanhai","再编码结束");
        return retFile;
    }

    public synchronized File decodePartFile() {
        //写入文件
        String filePath = partFilePath + File.separator + partNo + ".org";

        File orgFile = new File(filePath);
        if (orgFile.exists()) {
            return orgFile;
        }

        Vector<File> files = null;
        try {
            readWriteLock.readLock().lock();
            files = ToolUtils.getUnderFiles(pieceFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
        }

        if (files.size() < K) {
            return null;
        }


        // 从文件中读取编码系数
        byte[] coef = new byte[K * K];
        for (int i = 0; i < K; i++) {
            try {
                RandomAccessFile af = new RandomAccessFile(files.get(i), "r");
                af.seek(1);
                af.readFully(coef, i * K, K);
                af.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        //计算系数矩阵的逆矩阵
        byte[] invMatrix = NCUtils.InverseMatrix(coef, K);

        // 分为1M 1M 编码
        int len = 1 * 1024 * 1024;
        //每次从每个文件中读取的长度
        int perLen = (int) Math.ceil((float) len / K);
        // 一次处理的数据总长
        len = perLen * K;
        // 可以对每个文件读取几次
        int realFileLen = pieceFileLen - 1 - K;
        int parts = (int) Math.ceil((float) realFileLen / perLen);

        // 一个存数据  一个存结果
        byte[] data = new byte[len];
        byte[] result = new byte[len];


        // 结果文件
        File retFile = ToolUtils.createFile(filePath);
        try {
            RandomAccessFile retAf = new RandomAccessFile(retFile, "rw");
            retAf.setLength(K * realFileLen);

            int restLen = realFileLen;
            for (int i = 0; i < parts; i++) {
                //可读取字节数
                int readLen = perLen > restLen ? restLen : perLen;
                int startPos = i * perLen;
                for (int j = 0; j < K; j++) {
                    // 从一个文件中每次读取perLen长度的内容
                    RandomAccessFile af = new RandomAccessFile(files.get(j), "r");
                    af.seek(startPos + 1 + K);  //直接除去编码系数部分
                    int startData = j * readLen;
                    af.readFully(data, startData, readLen);
                    af.close();
                }
                // 再编码
                NCUtils.Multiply2(invMatrix, K, K, data, K, readLen, result);
                // 剩下的长度
                restLen -= readLen;
                // 将结果写入结果文件
                for (int j = 0; j < K; j++) {
                    int start = startPos + j * realFileLen;
                    retAf.seek(start);
                    retAf.write(result, j * readLen, readLen);
                }
            }

            // 截断文件长度  去除尾部的0
            retAf.setLength(partFileLen);

            retAf.close();
        } catch (IOException e) {
            e.printStackTrace();
            ToolUtils.deleteFile(retFile);
            return null;
        }

        return retFile;
    }


    //保存文件
    public boolean saveFile(File file, String fileName) {
        // 1 判断文件长度
        int fileLen = (int) file.length();
        if (fileLen != pieceFileLen) return false;

        // 2 再计算秩
        //获取文件的编码系数
        byte[] bytes = getFileCoef(file);

        // 计算秩
        int row = coefMatrix.size();
        byte[][] bt_coef = new byte[row + 1][K];
        for (int i = 0; i < row; i++) {
            int[] array = coefMatrix.get(i);
            for (int j = 0; j < K; j++) {
                bt_coef[i][j] = (byte) array[j];
            }
        }
        for (int i = 0; i < K; i++) {
            bt_coef[row][i] = bytes[i];
        }

        int rank = NCUtils.getRank(bt_coef);
        // rank == row + 1
        if (rank > row) {
            try {
                readWriteLock.writeLock().lock();
                File newFile = ToolUtils.createFile(pieceFilePath, fileName);
                ToolUtils.copyFile(file, newFile);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                readWriteLock.writeLock().unlock();
            }
            //更新系数数组
            updateCoefMatrix(bytes);
            return true;
        }

        return false;
    }

    //更新系数数组
    private synchronized void updateCoefMatrix(byte[] bytes) {
        int[] intArray = new int[K];
        for (int i = 0; i < K; i++) {
            int temp = bytes[i];
            if (temp < 0)
                temp += 256;
            intArray[i] = temp;
        }
        coefMatrix.add(intArray);
    }

    //向对方请求文件  获取需要请求的文件编码系数
    public synchronized byte[] getRequestCoef(PartFile itsPartFile) {
        int row = coefMatrix.size();
        if (row == K)
            return null;
        //判断哪个编码系数对自己有用
        //把 int 系数数组转化为byte数组
        Vector<int[]> itsCoefMat = itsPartFile.coefMatrix;
        int[] ret = null;
        if (coefMatrix.size() == 0) {
            ret = itsCoefMat.get(0);
        } else {
            Vector<int[]> vCoef = new Vector<>(row + 1);
            for (int i = 0; i < row; i++) {
                int[] array = coefMatrix.get(i);
                vCoef.add(array);
            }
            for (int[] array : itsCoefMat) {
                vCoef.add(array);
                byte[][] bt_coef = intArrVector2byteArray(vCoef);
                int rank = NCUtils.getRank(bt_coef);
                if (rank == row + 1) {
                    ret = array;
                    break;
                }
                vCoef.remove(row);
            }
        }
        if (ret == null) {
            return null;
        } else {
            byte[] requestCoef = new byte[K + 1];
            //第一个字节存放partNo
            requestCoef[0] = (byte) partNo;
            for (int i = 0; i < K; i++) {
                requestCoef[i + 1] = (byte) ret[i];
            }
            return requestCoef;
        }
    }

    /**
     * 获取待发送的文件
     *
     * @param request
     * @return
     */
    //因为OD下 RS下都会用到这种方法 所以在父类定义
    public File getODSendFile(byte[] request) {
        byte[] itsCoef = new byte[K];
        for (int i = 0; i < K; i++) {
            itsCoef[i] = request[i + 1];
        }

        Vector<File> files = null;
        try {
            readWriteLock.readLock().lock();
            files = ToolUtils.getUnderFiles(pieceFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            readWriteLock.readLock().unlock();
        }

        for (File file : files) {
            byte[] coef = getFileCoef(file);
            if (Arrays.equals(itsCoef, coef)) {
                return file;
            }
        }
        return null;
    }

    //因为NC下 RS下都会用到这种方法 所以在父类定义
    //此方法需要同步
    //需要保证取到的文件不同，文件发送后，还需要删除
    public File getNCSendFile(byte[] request) {
//        Vector<File> files = ToolUtils.getUnderFiles(reencodeFilePath);
//        File file = null;
//        if (files.size() == 0) {
//            file = reencodePartFile();
//        } else {
//            //对文件进行排序  取最新的文件
//            ToolUtils.fileDesSort(files);
//            file = files.get(0);
//        }
        //再生成一个再编码文件
        //recoverPartFile();
        //return file;
        return reencodePartFile();
    }

    /**
     * 发送后处理已经发送的文件
     *
     * @param file
     */
    public void afterODSendFile(File file) {
        //什么都不做
    }

    public void afterNCSendFile(File file) {
        // 删除再编码文件
        ToolUtils.deleteFile(file);
        // 重开线程 再编码
        //MyThreadPool.execute(() -> reencodePartFile());
//        Vector<File> files = ToolUtils.getUnderFiles(reencodeFilePath);
//        if(files.size()==0){
//            MyThreadPool.execute(() -> reencodePartFile());
//        }
    }


    private byte[][] intArrVector2byteArray(Vector<int[]> intArrVector) {
        int row = intArrVector.size();
        int col = intArrVector.get(0).length;
        byte[][] ret = new byte[row][col];
        for (int i = 0; i < row; i++) {
            int[] array = intArrVector.get(i);
            for (int j = 0; j < col; j++) {
                ret[i][j] = (byte) array[j];
            }
        }
        return ret;
    }

    /**
     * 获取文件的编码系数
     *
     * @param file
     * @return
     */
    private byte[] getFileCoef(File file) {
        //获取文件的编码系数
        byte[] bytes = new byte[K];
        try {
            RandomAccessFile af = new RandomAccessFile(file, "r");
            //跳过第一个字节
            af.seek(1);  //K值
            af.readFully(bytes);
            af.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }


    // 获取发送文件 和 发送后处理已经发送的文件
    // OD 保留
    // NC 删除
    // RS 删除或者保留
    public abstract File getSendFile(byte[] coef);

    public abstract void afterSendFile(File file);
    public abstract String getMode();
}
