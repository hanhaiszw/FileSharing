package connect;


import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import cache.EncodeFile;
import utils.MyThreadPool;

public class MyServerSocket {
    private ServerSocket serverSocket;
    private List<MySocket> mySockets = new ArrayList<>();

    public MyServerSocket() {

    }

    //打开服务器
    public void openServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            //是否设置超时  accept超时设置
            //serverSocket.setSoTimeout(10*1000);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("serversocket开启失败");
            return;
        }
        System.out.println("serversocket开启成功，等待客户端连接");
        //等待客户端连接
        Runnable runnable = () -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
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
