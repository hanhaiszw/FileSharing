package cache;

import android.util.Log;

import com.example.mroot.filesharing.MainActivity;
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

import connect.SocketMsgContent;
import data.CachePath;
import data.ConstantData;
import data.MsgType;
import utils.MyThreadPool;
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

    @XStreamOmitField
    private boolean initSuccess = false;  //判断当前恩encodefile是否可用

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
        encodeFileSingleton.initSuccess = true;
    }

    public static void updateSingleton(String xmlFilePath) {
        File file = new File(xmlFilePath);
        if (file.exists()) {
            encodeFileSingleton = xml2obj(xmlFilePath);
            encodeFileSingleton.initSuccess = true;
            String className = encodeFileSingleton.partFileVector.get(0).getClass().getName();
            Log.v("hanhai", className);
        }
    }

    //根据其他手机的encodeFile查找或生成本机的配置对象
    public static void updateSingleton(EncodeFile itsEncodeFile) {
        getLocalCache(itsEncodeFile);
        encodeFileSingleton.initSuccess = true;
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
            partFile.initPartFile(folderPath, i, K, AndroidId, file, startPos, len);
            partFileVector.add(partFile);
        }


        //写入配置文件
        object2xml();

        // 文件源初始化文件
        String msg = "文件源初始化文件: " + folderPath;
        String fileMsg = ToolUtils.getCurrentTime() + "\n"+ msg  + "\n";
        MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),msg);
        byte[] bt_fileMsg = fileMsg.getBytes();
        ToolUtils.writeToFile(CachePath.LOG_PATH, CachePath.LOG_FILE_NAME, bt_fileMsg, bt_fileMsg.length, true);

    }

    //把对象保存在xml文件中
    private synchronized void object2xml() {
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
                        "encodeFileSingleton",
                        "initSuccess"
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
                        "AndroidId",
                        "readWriteLock"
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
    private static EncodeFile getLocalCache(EncodeFile itsEncodeFile) {

        String folderPath = itsEncodeFile.folderPath;
        //当前就是此对象
        if (encodeFileSingleton.initSuccess &&
                encodeFileSingleton.folderPath.equals(folderPath)) {
            return encodeFileSingleton;
        }

        String xmlFilePath = folderPath + File.separator + xmlFileName;
        File xmlFile = new File(xmlFilePath);


        if (xmlFile.exists()) {
            encodeFileSingleton = xml2obj(xmlFilePath);
        } else {
            EncodeFile newEncodeFile = new EncodeFile();
            newEncodeFile.fileName = itsEncodeFile.fileName;
            newEncodeFile.fileLen = itsEncodeFile.fileLen;
            newEncodeFile.folderPath = itsEncodeFile.folderPath;
            ToolUtils.createFolder(newEncodeFile.folderPath);
            newEncodeFile.partNum = itsEncodeFile.partNum;
            newEncodeFile.AndroidId = itsEncodeFile.AndroidId;
            newEncodeFile.K = itsEncodeFile.K;
            newEncodeFile.runModeString = itsEncodeFile.runModeString;
            newEncodeFile.currentPieceNum = 0;
            //处理partFile
            for (PartFile partFile : itsEncodeFile.partFileVector) {
                PartFile newPartFile = PartFileFactory.createPartFile(newEncodeFile.runModeString);
                //创建存储路径
                newPartFile.initPartFile(partFile, newEncodeFile.folderPath);
                newEncodeFile.partFileVector.add(newPartFile);
            }
            //写入配置文件
            newEncodeFile.object2xml();
            encodeFileSingleton = newEncodeFile;


            // 第一次接收到文件信息:
            String msg = "第一次接收到文件信息: " + newEncodeFile.folderPath;
            String fileMsg = ToolUtils.getCurrentTime() + "\n"+ msg  + "\n";
            MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),msg);

            byte[] bt_fileMsg = fileMsg.getBytes();
            ToolUtils.writeToFile(CachePath.LOG_PATH, CachePath.LOG_FILE_NAME, bt_fileMsg, bt_fileMsg.length, true);


        }
        return encodeFileSingleton;
    }

    //恢复文件
    public synchronized void recover() {

        if (!initSuccess) return;

        //片文件数目不足以恢复原文件
        if (currentPieceNum < partNum * K) {
            return;
        }

        //如果本机就是文件源，则不执行解码
        if (AndroidId.equals(ConstantData.ANDROID_ID)) {
            MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),
                    "本机为文件源，不执行解码操作");
            return;
        }


        //文件已经被恢复了
        if (ToolUtils.isFileExists(folderPath, fileName)) {
            return;
        }

        // 接收到所有的文件片
        String msg = "文件接收完全，开始解码: " + folderPath;
        String fileMsg = ToolUtils.getCurrentTime() + "\n"+ msg  + "\n";
        MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),msg);

        byte[] bt_fileMsg = fileMsg.getBytes();
        ToolUtils.writeToFile(CachePath.LOG_PATH, CachePath.LOG_FILE_NAME, bt_fileMsg, bt_fileMsg.length, true);


        //开始解码恢复文件
        MyThreadPool.execute(() -> {
            Log.d("hanhai", "开始恢复数据");
            MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "开始恢复数据");
            File[] files = new File[partNum];

            for (PartFile partFile : partFileVector) {
                Log.i("hanhai", "开始解码");
                File file = partFile.decodePartFile();
                Log.i("hanhai", "解码结束");
                if (file == null)
                    return;
                int index = partFile.partNo - 1;
                files[index] = file;
            }

            Log.i("hanhai", "开始拼接数据");
            String outFilePath = folderPath + File.separator + fileName;
            ToolUtils.mergeFiles(outFilePath, files);
            Log.i("hanhai", "拼接数据完成");

            Log.d("hanhai", "恢复数据成功");
            MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "恢复数据成功");

            // 打开文件
            //ToolUtils.openFile(MainActivity.getContext(),outFilePath);
        });

    }

    // 实验测试方法
    public void test() {
        for (PartFile partFile : partFileVector) {
            partFile.reencodePartFile();
        }
    }

    //接收到文件后 存储文件
    public void savePartFile(SocketMsgContent socketMsgContent) {
        int partNo = socketMsgContent.partNo;
        for (PartFile partFile : partFileVector) {
            if (partFile.partNo == partNo) {
                boolean flag = partFile.saveFile(socketMsgContent.file, socketMsgContent.fileName);
                if (flag) {
                    updateCurrentPieceNum();
                    // 更新xml
                    object2xml();
                    //
                    recover();
                }
                //删除临时文件
                ToolUtils.deleteFile(socketMsgContent.file);
                break;
            }
        }

    }

    //获取文件请求信息
    public Vector<byte[]> getFileRequest(EncodeFile itsEncodeFile) {
        Vector<byte[]> requestVector = new Vector<>();

        // 为什么这里会为空
        if (itsEncodeFile == null) {
            return requestVector;
        }

        for (PartFile itsPartFile : itsEncodeFile.partFileVector) {
            for (PartFile partFile : partFileVector) {
                if (partFile.partNo == itsPartFile.partNo) {
                    //
                    byte[] bytes = partFile.getRequestCoef(itsPartFile);
                    if (bytes != null) {
                        requestVector.add(bytes);
                    }
                    break;
                }
            }
        }
        return requestVector;
    }


    /**
     * 获取到待发送的文件
     *
     * @return
     */
    public File getSendFile(int partNo, byte[] request) {
        PartFile partFile = null;
        for (PartFile partFile1 : partFileVector) {
            if (partFile1.partNo == partNo) {
                partFile = partFile1;
                break;
            }
        }
        File file = partFile.getSendFile(request);
        return file;
    }

    public void afterSendFile(int partNo, File file) {
        for (PartFile partFile : partFileVector) {
            if (partFile.partNo == partNo) {
                partFile.afterSendFile(file);
            }
        }
    }

    //更新文件片数
    private synchronized void updateCurrentPieceNum() {
        currentPieceNum += 1;
    }

    public boolean isInitSuccess() {
        return initSuccess;
    }

    public int getK() {
        return K;
    }

    public String getRunModeString() {
        return runModeString;
    }

    public int getCurrentPieceNum() {
        return currentPieceNum;
    }

    public int getTotalPieceNum() {
        return K * partNum;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getXmlFilePath() {
        return folderPath + File.separator + xmlFileName;
    }
}
