package connect;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
                    mySockets.add(new MySocket(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("等待客户端超时");
                    break;
                }
            }
        };
        MyThreadPool.execute(runnable);
    }


    public void closeServer(){
        try {
            for(MySocket mySocket:mySockets){
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
