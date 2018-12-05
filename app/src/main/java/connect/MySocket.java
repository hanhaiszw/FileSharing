package connect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;


import cache.SocketMsgParse;
import data.CachePath;
import utils.MyThreadPool;
import utils.ToolUtils;

public class MySocket {
    private Socket socket;
    private SocketMsgParse socketMsgParse;
    //private DataInputStream dis;
    // 0 表示是server
    // 1 表示是client
    public final static int SOCKET_SERVER = 0;
    public final static int SOCKET_CLIENT = 1;
    private int socketState;

    public String ip = "";
    public MySocket(Socket socket, int socketState) {
        this.socket = socket;
        this.socketState = socketState;
        this.ip = socket.getInetAddress().toString();
        socketMsgParse = new SocketMsgParse(this);
        init();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        try {
            //无论数据多少 都立即发送
            socket.setTcpNoDelay(true);
            //设置read的超时值
            //socket.setSoTimeout(3 * 1000);
            // 当超时15秒没有读到数据  就断开吧
            socket.setSoTimeout(15 * 1000);
            MyThreadPool.execute(receiveRunnable);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private Runnable receiveRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                byte[] data = new byte[4096];
                while (true) {
                    if (socketIsActive()) {
                        receiveSocketMsgContent(dis, data);
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("client端socket关闭");
            } finally {
                //关闭socket操作
                close();
            }
        }
    };

    private File receiveFile(DataInputStream dis, byte[] data, int fileLen) throws IOException {
        //String fileName = dis.readUTF();
        //存入随机文件名字下
        String randomFileName = ToolUtils.randomString(5);
        File file = ToolUtils.createFile(CachePath.RECEIVE_TEMP_PATH, randomFileName);

        //int fileLen = dis.readInt();
        int readLen = fileLen;
        int readBytes;

        FileOutputStream fos = new FileOutputStream(file, true);  //续写
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        while (readLen != 0) {
            readBytes = readLen > 4096 ? 4096 : readLen;
            dis.readFully(data, 0, readBytes);
            //存入文件
            //
            bos.write(data, 0, readBytes);
            readLen -= readBytes;
        }

        bos.close();
        fos.close();

        return file;
    }


    private void receiveSocketMsgContent(DataInputStream dis, byte[] data) throws IOException {

        SocketMsgContent socketMsgContent = new SocketMsgContent();
        socketMsgContent.serveOrClient = dis.readInt();
        socketMsgContent.code = dis.readInt();

        /**
         * 分支一  文件请求
         */
        if (socketMsgContent.code == SocketMsgContent.CODE_FILE_REQUEST) {
            socketMsgContent.requestLen = dis.readInt();
            byte[] bytes = new byte[socketMsgContent.requestLen];
            dis.readFully(bytes);
            socketMsgContent.requestBytes = bytes;

        } else if (socketMsgContent.code == SocketMsgContent.CODE_XML_PART_FILE) {
            /**
             * 分支二  接收xml文件或是partfile
             */
            socketMsgContent.partNo = dis.readInt();
            socketMsgContent.fileLen = dis.readInt();

            socketMsgContent.fileName = dis.readUTF();
            socketMsgContent.file = receiveFile(dis, data, socketMsgContent.fileLen);
        } else if (socketMsgContent.code == SocketMsgContent.CODE_LEAVE ||
                socketMsgContent.code == SocketMsgContent.CODE_ANSWER_END) {
            /**
             * 分支三 执行的是离开 或是 一次对方一次文件应答结束
             */
        }
        // 送去解析数据  这里不应该重开线程
        // MyThreadPool.execute(() -> socketMsgParse.parse(socketMsgContent));
        socketMsgParse.parse(socketMsgContent);
    }

    public void sendFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            //dos.writeUTF(file.getName());
            //dos.writeInt((int) file.length());
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] data = new byte[4096];
            int len;
            while ((len = bis.read(data)) != -1) {
                dos.write(data, 0, len);
            }
            bis.close();

            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendSocketMsgContent(SocketMsgContent socketMsgContent) {
        if (socketMsgContent == null) {
            return;
        }
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(socketMsgContent.serveOrClient);
            dos.writeInt(socketMsgContent.code);

            /**
             * 分支一  文件请求
             */
            if (socketMsgContent.code == SocketMsgContent.CODE_FILE_REQUEST) {
                dos.writeInt(socketMsgContent.requestLen);
                dos.write(socketMsgContent.requestBytes);

            } else if (socketMsgContent.code == SocketMsgContent.CODE_XML_PART_FILE) {
                /**
                 * 分支二  发送xml文件或是partfile
                 */
                dos.writeInt(socketMsgContent.partNo);
                dos.writeInt(socketMsgContent.fileLen);
                dos.writeUTF(socketMsgContent.fileName);
                sendFile(socketMsgContent.file);
            } else if (socketMsgContent.code == SocketMsgContent.CODE_LEAVE ||
                    socketMsgContent.code == SocketMsgContent.CODE_ANSWER_END) {
                /**
                 * 分支三  执行的是离开 或是 一次文件应答结束
                 */
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 判断是不是由client端创建的socket
    public boolean isClientSocket() {
        return socketState == SOCKET_CLIENT;
    }

    public void sendMsg(String msg) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // 检查socket是否还可用
    public boolean socketIsActive(){
        return socket.isConnected() && !socket.isClosed();
    }
}
