package cache;

import android.util.Log;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.SortableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;

import java.io.File;
import java.util.Vector;

import data.CachePath;
import data.ConstantData;
import utils.ToolUtils;

//做成单例类
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
    //唯一识别码
    private String AndroidId;

    //运行模式
    private String runModeString;

    //文件片的信息
    @XStreamAlias("subfileInfor")
    private Vector<PartFile> partFileVector = new Vector<>();

    @XStreamOmitField
    private final static String xmlFileName = "xml.txt";  //配置文件的名称

    @XStreamOmitField
    private static EncodeFile encodeFileSingleton = new EncodeFile();

    private EncodeFile(File file, int K, String runModeString) {
        init(file, K, runModeString);
    }

    private EncodeFile() {
    }


    public static EncodeFile getSingleton() {
        return encodeFileSingleton;
    }

    public static void updateSingleton(File file, int K, String runModeString) {
        encodeFileSingleton = new EncodeFile(file, K, runModeString);
    }

    public static void updateSingleton(String xmlFilePath) {
        File file = new File(xmlFilePath);
        if (file.exists()) {
            encodeFileSingleton = xml2obj(xmlFilePath);
            String className = encodeFileSingleton.partFileVector.get(0).getClass().getName();
            Log.e("hanhai", className);
        }
    }

    //根据其他手机的encodeFile查找或生成本机的配置对象
    public static void updateSingleton(EncodeFile itsEncodeFile) {
        getLocalCache(itsEncodeFile);
    }


    //对文件进行分片预处理
    private void init(File file, int K, String runModeString) {
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
        //
        this.runModeString = runModeString;

        //每一部分的长度
        for (int i = 1; i <= partNum; i++) {
            int len = partLen;
            if (i == partNum) {
                len = fileLen - partLen * (partNum - 1);
            }
            int startPos = (i - 1) * partLen;
            PartFile partFile = PartFileFactory.createPartFile(runModeString);
            partFile.initPartFile(folderPath, i, file, startPos, len, K, AndroidId);
            partFile.reencodePartFile();
            partFileVector.add(partFile);
        }


        //写入配置文件
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
                        "AndroidId",
                        "runModeString",
                        "partFileVector",
                        "xmlFileName",
                        "encodeFileSingleton"
                });

        sorter.registerFieldOrder(PartFile.class,
                new String[]{
                        "partNo",
                        "pieceFileLen",
                        "partFileLen",
                        "coefMatrix",
                        "K",
                        "partFilePath",
                        "pieceFilePath",
                        "reencodeFilePath",
                        "AndroidId"
                });

        XStream xStream = new XStream(new Sun14ReflectionProvider(new FieldDictionary(sorter)));
        xStream.setMode(XStream.NO_REFERENCES);
        //使用注解
        xStream.processAnnotations(EncodeFile.class);
        xStream.processAnnotations(PartFile.class);
        xStream.processAnnotations(RSPartFile.class);
        xStream.processAnnotations(NCPartFile.class);
        xStream.processAnnotations(ODPartFile.class);
        //继承关系   不可以使用下面语句省略父类的class子类属性，否则xml2obj获取到的子类类型会出错
        //xStream.addDefaultImplementation(ODPartFile.class, PartFile.class);
        //xStream.addDefaultImplementation(NCPartFile.class, PartFile.class);
        //xStream.addDefaultImplementation(RSPartFile.class, PartFile.class);

        //转化为String，并保存入文件
        String xml = xStream.toXML(this);
        ToolUtils.writeToFile(folderPath, xmlFileName, xml.getBytes());
    }


    //把xml恢复为对象
    public static EncodeFile xml2obj(String xmlFilePath) {
        //
        byte[] bt_xml = ToolUtils.readFile(xmlFilePath);
        String xml = new String(bt_xml);
        XStream xStream = new XStream(new DomDriver("UTF-8"));
        //使用注解
        xStream.processAnnotations(EncodeFile.class);
        xStream.processAnnotations(PartFile.class);

        xStream.processAnnotations(RSPartFile.class);
        xStream.processAnnotations(NCPartFile.class);
        xStream.processAnnotations(ODPartFile.class);
        //这个blog标识一定要和Xml中的保持一直，否则会报错
        xStream.alias("EncodeFile", EncodeFile.class);

        //继承关系
        //xStream.addDefaultImplementation(NCPartFile.class, PartFile.class);
        //xStream.addDefaultImplementation(RSPartFile.class, PartFile.class);
        //xStream.addDefaultImplementation(ODPartFile.class, PartFile.class);

        EncodeFile encodeFile = null;
        try {
            encodeFile = (EncodeFile) xStream.fromXML(xml);
        } catch (Exception e) {
            e.printStackTrace();
            //解析出错
            return null;
        }

        //为了保持xml文件的简练  partFile类中有些信息没有存入
        //在此恢复
        for (PartFile partFile : encodeFile.partFileVector) {
            partFile.recoverOmitField(encodeFile.folderPath, encodeFile.K, encodeFile.AndroidId);
        }

        return encodeFile;
    }

    //如果本地不存在此文件信息则创建
    private static EncodeFile getLocalCache(EncodeFile encodeFile) {
        String folderPath = encodeFile.folderPath;
        //当前就是此对象
        if (encodeFileSingleton.folderPath.equals(folderPath)) {
            return encodeFileSingleton;
        }
        File folder = new File(folderPath);
        if (folder.exists()) {
            String xmlFilePath = folderPath + File.separator + xmlFileName;
            encodeFileSingleton = xml2obj(xmlFilePath);
        } else {
            EncodeFile newEncodeFile = new EncodeFile();
            newEncodeFile.fileName = encodeFile.fileName;
            newEncodeFile.fileLen = encodeFile.fileLen;
            newEncodeFile.folderPath = encodeFile.folderPath;
            ToolUtils.createFolder(newEncodeFile.folderPath);
            newEncodeFile.partNum = encodeFile.partNum;
            newEncodeFile.AndroidId = encodeFile.AndroidId;
            newEncodeFile.K = encodeFile.K;
            newEncodeFile.runModeString = encodeFile.runModeString;
            newEncodeFile.object2xml();
            encodeFileSingleton = newEncodeFile;
        }
        return encodeFileSingleton;
    }

    //恢复文件
    public void recover() {
        //如果本机就是文件源，则不执行解码
        if (AndroidId.equals(ConstantData.ANDROID_ID)) {
            return;
        }
        //片文件数目不足以恢复原文件
        if (currentPieceNum < partNum * K) {
            return;
        }
        File[] files = new File[partNum];
        synchronized (EncodeFile.class) {
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

    public String getXmlFilePath() {
        return folderPath + File.separator + xmlFileName;
    }
}
