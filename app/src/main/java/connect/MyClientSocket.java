package connect;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class MyClientSocket {
    private MySocket mySocket;

    public MyClientSocket() {

    }

    public void connect(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            mySocket = new MySocket(socket);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("连接服务器失败");
            return;
        }
        System.out.println("连接服务器成功");
    }

//    public void sendMsg(String msg) {
//        if (mySocket != null) {
//            mySocket.sendMsg(msg);
//        }
//    }
//
//    public void sendFile(File file) {
//        if (mySocket != null) {
//            mySocket.sendFile(file);
//        }
//    }
}
