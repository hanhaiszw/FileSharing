package connect;

import android.util.Log;

import com.example.mroot.filesharing.MainActivity;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import data.MsgType;

public class MyClientSocket {
    private MySocket mySocket;

    public MyClientSocket() {

    }

    public boolean connect(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            //mySocket = new MySocket(socket,MySocket.SOCKET_CLIENT);
            //Socket socket = new Socket();
            //socket.connect(new InetSocketAddress(ip, port), 3000);//设置连接请求超时时间10 s
            mySocket = new MySocket(socket,MySocket.SOCKET_CLIENT);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("hanhai", "连接服务器失败");
            return false;
        }
        // 尝试切换
        trySwitch2Server();
        Log.e("hanhai", "连接服务器成功");
        return true;
    }

    /**
     * 尝试切换
     */
    public void trySwitch2Server() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 当断开连接时  尝试切换向server
                if(!mySocket.socketIsActive()){
                    //切换向server
                    MainActivity.sendMsg2UIThread(MsgType.CLIENT_2_SERVER.ordinal(),"");
                    timer.cancel();
                }
            }
        }, 0, 1000);
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
