package cache;

import android.util.Log;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.SortableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;

import java.io.File;
import java.util.Vector;

import data.CachePath;
import data.ConstantData;
import utils.ToolUtils;


@XStreamAlias("EncodeFile")
public class EncodeFile {
    //主属性
    @XStreamAsAttribute
    private String fileName;

    private String folderPath;

    @XStreamAlias("GenerationSize")
    private int K;

    private int fileLen;

    private int partNum;
    private int currentPieceNum;  //现在片文件的数目总和


    //文件片的信息
    @XStreamAlias("subfileInfor")
    private Vector<PartFile> partFileVector = new Vector<>();


    //唯一识别码
    private String AndroidId;

    @XStreamOmitField
    private static String xmlFileName = "xml.txt";  //配置文件的名称


    public EncodeFile(File file, int K) {
        init(file, K);
    }


    //对文件进行分片预处理
    private void init(File file, int K) {
        this.AndroidId = ConstantData.ANDROID_ID;
        this.fileName = file.getName();
        this.K = K;
        this.folderPath = ToolUtils.createFolder(CachePath.TEMP_PATH, ToolUtils.randomString(5));
        //把文件按每部分10M划分
        this.fileLen = (int) file.length();
        int partLen = 10 * 1024 * 1024; //10M
        this.partNum = fileLen / partLen + (fileLen % partLen == 0 ? 0 : 1);
        //解码所需要的文件片数
        this.currentPieceNum = partNum * K;
        //每一部分的长度
        for (int i = 1; i <= partNum; i++) {
            int len = partLen;
            if (i == partNum) {
                len = fileLen - partLen * (partNum - 1);
            }
            int startPos = (i - 1) * partLen;
            PartFile partFile = new PartFile();
            partFile.initPartFile(folderPath, i, file, startPos, len, K);
            partFileVector.add(partFile);
        }


        object2xml();

        Log.d("hanhai", "文件预处理结束");
    }


    //把对象保存在xml文件中
    private void object2xml() {
        //设置xml字段顺序
        SortableFieldKeySorter sorter = new SortableFieldKeySorter();
        sorter.registerFieldOrder(EncodeFile.class,
                new String[]{
                        "fileName",
                        "folderPath",
                        "K",
                        "fileLen",
                        "partNum",
                        "currentPieceNum",
                        "partFileVector",
                        "AndroidId",
                        "xmlFileName"
                });

        sorter.registerFieldOrder(PartFile.class,
                new String[]{
                        "partNo",
                        "pieceFileLen",
                        "partFileLen",
                        "coefMatrix",
                        "K",
                        "partFilePath",
                        "pieceFilePath"
                });

        XStream xStream = new XStream(new Sun14ReflectionProvider(new FieldDictionary(sorter)));
        xStream.setMode(XStream.NO_REFERENCES);
        //使用注解
        xStream.processAnnotations(EncodeFile.class);
        xStream.processAnnotations(PartFile.class);
        //转化为String，并保存入文件
        String xml = xStream.toXML(this);
        ToolUtils.writeToFile(folderPath, xmlFileName, xml.getBytes());
    }

    //把xml恢复为对象
//    public static EncodeFile xml2obj(String xmlFilePath) {
//
//
//    }

    public void recover() {
        //如果本机就是文件源，则不执行解码
//        if (AndroidId.equals(ConstantData.ANDROID_ID)) {
//            return;
//        }
        //片文件数目不足以恢复原文件

        if (currentPieceNum < partNum * K) {
            return;
        }
        File[] files = new File[partNum];
        synchronized (EncodeFile.class){
            for (PartFile partFile : partFileVector) {
                File file = partFile.recoverPartFile();
                int index = partFile.partNo - 1;
                files[index] = file;
            }
        }

        String outFilePath = folderPath + File.separator + fileName;
        ToolUtils.mergeFiles(outFilePath, files);

        Log.d("hanhai", "恢复数据成功");
    }
}
