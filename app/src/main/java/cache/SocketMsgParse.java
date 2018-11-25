package cache;

import java.io.File;
import java.util.Vector;

import connect.MySocket;
import connect.SocketMsgContent;

/**
 * 解析Socket信息
 * 从缓存文件中取取数据
 */
public class SocketMsgParse {
    //对方的配置文件
    private EncodeFile itsEncodeFile;

    private MySocket mySocket;
    // 0
    // 1 一方想要离开
    // 2 离开
    private int leaveFlag = 0;

    public SocketMsgParse(MySocket mySocket) {
        this.mySocket = mySocket;
    }

    public void parse(SocketMsgContent socketMsgContent) {
        int code = socketMsgContent.code;
        SocketMsgContent answer1 = null;
        SocketMsgContent answer2 = null;
        switch (code) {
            // 0 xml文件
            case 0:
                answer1 = solveXMLFile(socketMsgContent);
                //文件请求
                answer2 = getFileQuestInfor();
                break;
            // 1 对方的文件请求
            case 1:
                //
                solveFileRequest(socketMsgContent);
                break;
            // 2 partFile分片文件
            case 2:
                answer1 = solvePartFile(socketMsgContent);
                break;
            // 3 leave 或者其他信息
            case 3:
                //执行离开
                solveLeave();
                break;
            default:
                break;
        }
        if (answer1 != null) {
            mySocket.sendSocketMsgContent(answer1);
        }
        if (answer2 != null) {
            mySocket.sendSocketMsgContent(answer2);
        }
    }




    //处理xml配置文件
    private SocketMsgContent solveXMLFile(SocketMsgContent socketMsgContent) {
        itsEncodeFile = EncodeFile.xml2obj(socketMsgContent.file.getPath());
        SocketMsgContent answer = null;
        //如果是服务器端发送来的xml配置信息，客户端是需要给服务器返回xml配置信息的
        if (socketMsgContent.serveOrClient == 0) {
            EncodeFile.updateSingleton(itsEncodeFile);
            File file = new File(EncodeFile.getSingleton().getXmlFilePath());
            answer = new SocketMsgContent(1, 0, 0, file);
        }
        //文件请求判断
        return answer;
    }

    /**
     * 处理文件请求
     * @param socketMsgContent
     */
    private void solveFileRequest(SocketMsgContent socketMsgContent) {

    }




    //接收到partFile
    private SocketMsgContent solvePartFile(SocketMsgContent socketMsgContent) {
        //保存对方发送来的文件
        EncodeFile.getSingleton().savePartFile(socketMsgContent);
        //是否再次向对方请求文件处理
        //发送文件请求逻辑
        SocketMsgContent answer = getFileQuestInfor();
        return answer;
    }

    private synchronized void solveLeave() {
        leaveFlag += 1;
        //关闭socket
        if(leaveFlag == 2)
            mySocket.close();
    }


    private SocketMsgContent getFileQuestInfor() {
        SocketMsgContent socketMsgContent = new SocketMsgContent();
        socketMsgContent.serveOrClient = 2;

        Vector<byte[]> requestVector = EncodeFile.getSingleton().getFileRequest(itsEncodeFile);
        if (requestVector.size() == 0) {
            //说明对方没有对自己有用的数据
            //离开
            socketMsgContent.code = 3;
            solveLeave();
        } else {
            int row = requestVector.size();
            int col = requestVector.get(0).length;
            byte[] requestBytes = new byte[row * col];
            for (int i = 0; i < row; i++) {
                byte[] array = requestVector.get(i);
                for (int j = 0; j < col; j++) {
                    requestBytes[i * col + j] = array[j];
                }
            }
            //文件请求
            socketMsgContent.code = 1;

            socketMsgContent.requestLen = requestBytes.length;
            socketMsgContent.requestBytes = requestBytes;
        }

        return socketMsgContent;
    }


}
