package connect;


import android.util.Log;

import com.example.mroot.filesharing.MainActivity;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
    private Timer switchTimer;

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
            if (isOpen) {
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

                    //最多只允许连接5个
                    if (mySockets.size() >= 3) {
                        socket.close();
                        Log.e("hanhai","MyServerSocket数量已超过5，拒绝再次连接");
                        continue;
                    }

                    MySocket mySocket = new MySocket(socket, MySocket.SOCKET_SERVER);
                    mySockets.add(mySocket);

                    MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), mySocket.ip + "已连接");
                    //向连接的客户端发送xml配置文件
                    File file = new File(EncodeFile.getSingleton().getXmlFilePath());
                    if (!file.exists()) {
                        Log.e("hanhai", "严重错误：server端xml配置文件不存在");
                        continue;
                    }
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
        switchTimer = new Timer();
        // 加入随机值
        Random random = new Random();
        int rnd = random.nextInt(15);

        Timer timer=switchTimer;
        switchTimer.schedule(new TimerTask() {
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
                // 10 到 20 秒之间开始切换
                // 调整Server等待时长为20+    2019/2/20
                if (mySockets.size() == 0 &&
                        System.currentTimeMillis() - acceptStartTime > (10 + rnd) * 1000) {
                    //切换向client
                    MainActivity.sendMsg2UIThread(MsgType.SERVER_2_CLIENT.ordinal(), "");
                    //switchTimer.cancel();
                    try {
                        timer.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 1000);
    }


    // 需要加入释放锁操作
    // 防止手动点击client按钮后  还会自动切换向
    public void cancelSwitchTimer() {
        try {
            switchTimer.cancel();
        } catch (Exception e) {
            System.out.println("MyServerSocket 取消锁异常");
            e.printStackTrace();
        }
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
