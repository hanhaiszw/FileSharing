package cache;

import java.io.File;
import java.util.Vector;

import data.CachePath;
import utils.ToolUtils;


//单例类
public class EncodeFile {
    private String fileName;
    private String folderPath;
    private int K;
    private Vector<PartFile> partFileVector;

    private static EncodeFile encodeFile = new EncodeFile();

    private EncodeFile() {
        partFileVector = new Vector<>();
    }

    //单例
    public static EncodeFile getSingleton(){
        return encodeFile;
    }

    //对文件进行分片预处理
    public void init(File file, int K) {
        this.fileName = file.getName();
        this.K = K;
        this.folderPath = ToolUtils.createFolder(CachePath.TEMP_PATH, ToolUtils.randomString(5));
        //把文件按每部分10M划分
        int fileLen = (int) file.length();
        int partLen = 10 ; //10M
        int partNum = fileLen / partLen + (fileLen % partLen == 0 ? 0 : 1);

        //每一部分的长度
        for (int i = 1; i <= partNum; i++) {
            int len = partLen;
            if (i == partNum) {
                len = fileLen - partLen * (partNum - 1);
            }
            int startPos = (i-1) * partLen;
            PartFile partFile = new PartFile();
            partFile.initPartFile(folderPath,i,file,startPos,len,K);
            partFileVector.add(partFile);
        }

    }




}
