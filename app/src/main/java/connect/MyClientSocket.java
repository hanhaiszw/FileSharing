package connect;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MyClientSocket {
    private MySocket mySocket;

    public MyClientSocket() {

    }

    public void connect(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            mySocket = new MySocket(socket,MySocket.SOCKET_CLIENT);
            //Socket socket = new Socket();
            //socket.connect(new InetSocketAddress(ip, port), 3000);//设置连接请求超时时间10 s

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("hanhai", "连接服务器失败");
            return;
        }

        Log.e("hanhai", "连接服务器成功");
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
