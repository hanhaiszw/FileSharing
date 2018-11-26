package connect;

import java.io.File;

public class SocketMsgContent {
    /**
     * 总支
     */
    // serveOrClient = 0 服务器信息
    // serverOrClient = 1 客户端信息
    // serverOrClient = 2 统一信息
    public final static int SERVER_MSG = 0;
    public final static int CLIENT_MSG = 1;
    public final static int SERVER_CLIENT_MSG = 2;
    public int serveOrClient;
    // code = 0 xml配置文件
    // code = 1 文件请求
    // code = 2 partFile分片文件
    // code = 3 leave 或者其他信息
    // code = 4 对方一次文件应答完成
    public final static int CODE_FILE_REQUEST = 1;
    public final static int CODE_XML_PART_FILE = 2;
    public final static int CODE_LEAVE = 3;
    public final static int CODE_ANSWER_END = 4;
    public int code;


    /**
     * code = 1
     * 分支一  发送文件请求
     */
    // 添加一个byte数组 用来标明请求的是哪个文件
    // 在这里 OD, RS, NC 处理方式不同
    // 每一段由 partNo 编码系数 组成，每段长 (1 + K) 个字节
    // 这个字段在文件请求时使用
    public int requestLen;        //请求的长度
    public byte[] requestBytes;


    /**
     * code = 0 或者 2
     * 分支二  发送文件   xml配置文件和partfile文件
     */
    // partNo = 0 xml配置文件
    // partNo = 1 -- partNum
    public final static int XML_PRATNO = 0;
    public int partNo;

    // 文件
    public File file;

    public String fileName;
    public int fileLen;
    //文件暂时存储名字
    //public String tempFileName;

    /**
     * code = 3 4
     * 分支三 执行的是离开 或者 对方一次文件应答完成
     */


    /**
     * 分支一 构造方法
     * 文件请求
     *
     * @param requestLen
     * @param requestBytes
     */
    public SocketMsgContent(int requestLen, byte[] requestBytes) {
        this.serveOrClient = SERVER_CLIENT_MSG;   // 2 不再区分服务器还是客户端
        this.code = CODE_FILE_REQUEST;            // 1 表示是文件请求
        this.requestLen = requestLen;
        this.requestBytes = requestBytes;
    }


    /**
     * 分支二 信息构造方法
     *
     * @param serveOrClient 发送xml文件时，需要区分是服务器还是客户端发送的
     * @param partNo
     * @param file          不可为null
     */
    public SocketMsgContent(int serveOrClient, int partNo, File file) {
        this.serveOrClient = serveOrClient;
        this.code = CODE_XML_PART_FILE;
        this.partNo = partNo;
        this.file = file;

        this.fileLen = (int) file.length();
        this.fileName = file.getName();
    }


    /**
     * 分支三  执行的是离开 或者 对方一次文件应答完成
     *
     * @param serveOrClient
     * @param code
     */
    public SocketMsgContent(int serveOrClient, int code) {
        this.serveOrClient = serveOrClient;
        this.code = code;
    }

    public SocketMsgContent() {

    }
}
