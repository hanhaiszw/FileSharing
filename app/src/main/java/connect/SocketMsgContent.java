package connect;

import java.io.File;

public class SocketMsgContent {
    /**
     * 总支
     */
    // serveOrClient = 0 服务器信息
    // serverOrClient = 1 客户端信息
    // serverOrClient = 2 统一信息
    public int serveOrClient;
    // code = 0 xml配置文件
    // code = 1 文件请求
    // code = 2 partFile分片文件
    // code = 3 leave 或者其他信息
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
    public int partNo;

    // 文件
    public File file;

    public String fileName;
    public int fileLen;
    //文件暂时存储名字
    //public String tempFileName;

    /**
     * code = 3
     * 分支三 执行的是离开
     */




    /**
     * 分支一 构造方法
     *
     * @param requestLen
     * @param requestBytes
     */
    public SocketMsgContent(int requestLen, byte[] requestBytes) {
        this.serveOrClient = 2;   // 2 不再区分服务器还是客户端
        this.code = 1;            // 1 表示是文件请求
        this.requestLen = requestLen;
        this.requestBytes = requestBytes;
    }


    /**
     * 分支二 信息构造方法
     *
     * @param serveOrClient 发送xml文件时，需要区分是服务器还是客户端发送的
     * @param code
     * @param partNo
     * @param file          不可为null
     */
    public SocketMsgContent(int serveOrClient, int code, int partNo, File file) {
        this.serveOrClient = serveOrClient;
        this.code = code;
        this.partNo = partNo;
        this.file = file;

        this.fileLen = (int) file.length();
        this.fileName = file.getName();
    }

    public SocketMsgContent() {

    }
}
