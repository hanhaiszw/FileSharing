package connect;

import java.io.File;

public class SocketMsgContent {
    // serveOrClient = 0 服务器信息
    // serverOrClient = 1 客户端信息
    public int serveOrClient;
    // code = 0 xml配置文件
    // code = 1 partFile分片文件
    // code = 2 文件请求
    // code = 3 leave 或者其他信息
    public int code;

    // 添加一个byte数组 用来标明请求的是哪个文件
    // 在这里 OD, RS, NC 处理方式不同
    // 每一段由 partNo 编码系数 组成，每段长 (1 + K) 个字节
    // 这个字段在文件请求时使用
    // byte[] requestBytes;

    // partNo = 1 -- partNum
    public int partNo;

    // 文件
    public File file;

    public String fileName;
    public int fileLen;
    //文件暂时存储名字
    //public String tempFileName;

    public SocketMsgContent(int serveOrClient,int code, int partNo, File file) {
        this.code = code;
        this.partNo = partNo;
        this.file = file;
        if (file == null) {
            this.fileLen = 0;
        } else {
            this.fileLen = (int) file.length();
            this.fileName = file.getName();
        }
    }

    public SocketMsgContent() {

    }
}
