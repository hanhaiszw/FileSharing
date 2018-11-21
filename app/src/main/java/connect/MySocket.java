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
    private DataInputStream dis;
    private byte[] data;

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
                        receiveFile(dis, data);
                        //String msg = dis.readUTF();
                        //System.out.println(msg);
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("client端socket关闭");
            }
        }
    };

    private void receiveFile(DataInputStream dis, byte[] data) throws IOException {
        this.dis = dis;
        this.data = data;

        String fileName = dis.readUTF();
        File file = ToolUtils.createFile(CachePath.RECEIVE_TEMP_PATH, fileName);

        int fileLen = dis.readInt();
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

        //把文件送给解析类
        socketMsgParse.parse(this, file);
    }


    public void sendFile(File file) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(file.getName());
            dos.writeInt((int) file.length());

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
