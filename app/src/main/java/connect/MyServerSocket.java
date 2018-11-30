package connect;


import com.example.mroot.filesharing.MainActivity;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cache.EncodeFile;
import data.MsgType;
import utils.MyThreadPool;

public class MyServerSocket {
    private ServerSocket serverSocket;
    private List<MySocket> mySockets = new ArrayList<>();
    private boolean isOpen;
    private long acceptStartTime;

    public MyServerSocket() {
        isOpen = false;
        acceptStartTime = 0;
    }

    // 打开服务器
    // 只会开启一次监听端口 然后一直在后台监听
    public void openServer(int port) {

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            //是否设置超时  accept超时设置
            //serverSocket.setSoTimeout(10*1000);
            // 开始计时
            acceptStartTime = System.currentTimeMillis();
            trySwitch2Client();
        } catch (IOException e) {
            e.printStackTrace();
            if(isOpen){
                System.out.println("serversocket开启成功");
                // 开始计时
                acceptStartTime = System.currentTimeMillis();
                trySwitch2Client();
            }
            return;
        }
        System.out.println("serversocket开启成功，等待客户端连接");
        isOpen = true;
        //等待客户端连接
        Runnable runnable = () -> {
            while (true) {
                try {
                    // 计算已经等了多久没有客户端来连接了
                    Socket socket = serverSocket.accept();
                    acceptStartTime = System.currentTimeMillis();

                    MySocket mySocket = new MySocket(socket, MySocket.SOCKET_SERVER);
                    mySockets.add(mySocket);

                    //向连接的客户端发送xml配置文件
                    File file = new File(EncodeFile.getSingleton().getXmlFilePath());
                    mySocket.sendSocketMsgContent(
                            new SocketMsgContent(SocketMsgContent.SERVER_MSG, SocketMsgContent.XML_PRATNO, file));

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("等待客户端超时");
                    break;
                }
            }
        };
        MyThreadPool.execute(runnable);
    }

    /**
     * 尝试切换
     */
    public void trySwitch2Client() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Iterator<MySocket> it = mySockets.iterator();
                while (it.hasNext()) {
                    MySocket mySocket = it.next();
                    if (!mySocket.socketIsActive()) {
                        it.remove();
                    }
                }

                // 当没有client在线  且等待时间超过了10s 则开始尝试切换
                if(mySockets.size() == 0 &&
                        System.currentTimeMillis() - acceptStartTime > 10 * 1000){
                    //切换向client
                    MainActivity.sendMsg2UIThread(MsgType.SERVER_2_CLIENT.ordinal(),"");
                    timer.cancel();
                }
            }
        }, 0, 1000);
    }


    public void closeServer() {
        try {
            for (MySocket mySocket : mySockets) {
                mySocket.close();
            }
            mySockets.clear();
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("serverSocket关闭");
    }
}
