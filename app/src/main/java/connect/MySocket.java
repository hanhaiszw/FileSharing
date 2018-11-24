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

    public MySocket(Socket socket) {
        this.socket = socket;
        socketMsgParse = new SocketMsgParse();
        init();
    }

    public void close() throws IOException {
        socket.close();
    }

    private void init() {
        try {
            //无论数据多少 都立即发送
            socket.setTcpNoDelay(true);
            //设置read的超时值
            //socket.setSoTimeout(3 * 1000);
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
                    if (socket.isConnected() && !socket.isClosed()) {
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


    private void receiveSocketMsgContent(DataInputStream dis, byte[] data) {
        try {
            SocketMsgContent socketMsgContent = new SocketMsgContent();
            socketMsgContent.serveOrClient = dis.readInt();
            socketMsgContent.code = dis.readInt();


            socketMsgContent.partNo = dis.readInt();
            socketMsgContent.fileLen = dis.readInt();
            if (socketMsgContent.fileLen != 0) {
                socketMsgContent.fileName = dis.readUTF();
                socketMsgContent.file = receiveFile(dis, data, socketMsgContent.fileLen);
            }
            socketMsgParse.parse(this, socketMsgContent);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(socketMsgContent.serveOrClient);
            dos.writeInt(socketMsgContent.code);
            dos.writeInt(socketMsgContent.partNo);
            dos.writeInt(socketMsgContent.fileLen);
            if (socketMsgContent.fileLen != 0) {
                sendFile(socketMsgContent.file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
}
