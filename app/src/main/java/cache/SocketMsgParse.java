package cache;

import com.example.mroot.filesharing.MainActivity;

import java.io.File;
import java.util.Vector;

import connect.MySocket;
import connect.SocketMsgContent;
import data.MsgType;
import utils.ToolUtils;

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

        switch (code) {
            // 1 对方的文件请求
            case SocketMsgContent.CODE_FILE_REQUEST:
                //
                solveFileRequest(socketMsgContent);
                break;
            // 2 partFile分片文件
            case SocketMsgContent.CODE_XML_PART_FILE:
                if(socketMsgContent.partNo == SocketMsgContent.XML_PRATNO){
                    // 处理文件请求
                    solveXMLFile(socketMsgContent);
                }else{
                    solvePartFile(socketMsgContent);
                }
                break;
            // 3 leave 或者其他信息
            case SocketMsgContent.CODE_LEAVE:
                //执行离开
                solveLeave();
                break;
            // 4 对方的一次文件请求处理完毕
            case SocketMsgContent.CODE_ANSWER_END:
                // 再次请求文件
                sendFileQuestInfor();
                break;
            default:
                break;
        }

        // 把改变发给MainActivity
        // 通知MainActivity, EncodeFile单例发生了改变
        MainActivity.getMainActivity().sendMsg2UIThread(MsgType.ENCODE_FILE_CHANGE.ordinal(),"");

    }


    //处理xml配置文件
    private void solveXMLFile(SocketMsgContent socketMsgContent) {
        itsEncodeFile = EncodeFile.xml2obj(socketMsgContent.file.getPath());
        //如果是服务器端发送来的xml配置信息，客户端是需要给服务器返回xml配置信息的
        if (socketMsgContent.serveOrClient == SocketMsgContent.SERVER_MSG) {
            EncodeFile.updateSingleton(itsEncodeFile);
            File file = new File(EncodeFile.getSingleton().getXmlFilePath());
            SocketMsgContent answer = new SocketMsgContent(
                    SocketMsgContent.CLIENT_MSG, SocketMsgContent.XML_PRATNO, file);
            //发送xml文件
            mySocket.sendSocketMsgContent(answer);

        }
        //删除对方的xml文件
        ToolUtils.deleteFile(socketMsgContent.file);

        //发送文件请求
        sendFileQuestInfor();
    }

    /**
     * 处理文件请求
     *
     * @param socketMsgContent
     */
    private void solveFileRequest(SocketMsgContent socketMsgContent) {
        int requestLen = socketMsgContent.requestLen;
        byte[] requestBytes = socketMsgContent.requestBytes;

        EncodeFile encodeFile = EncodeFile.getSingleton();
        int col = 1 + encodeFile.getK();
        int row = requestLen / col;

        Vector<byte[]> requestVector = new Vector<>();
        int index = 0;
        for (int i = 0; i < row; i++) {
            byte[] request = new byte[col];
            for (int j = 0; j < col; j++) {
                request[j] = requestBytes[index++];
            }
            requestVector.add(request);
        }

        for (byte[] request : requestVector) {
            int partNo = (int) request[0];
            File file = encodeFile.getSendFile(partNo, request);
            if (file != null) {
                SocketMsgContent msgContent = new SocketMsgContent(
                        SocketMsgContent.SERVER_CLIENT_MSG, partNo, file);
                mySocket.sendSocketMsgContent(msgContent);
                encodeFile.afterSendFile(partNo,file);
            }
        }

        // 一次文件请求结束
        SocketMsgContent msgContent = new SocketMsgContent(
                SocketMsgContent.SERVER_CLIENT_MSG, SocketMsgContent.CODE_ANSWER_END);
        mySocket.sendSocketMsgContent(msgContent);

    }


    /**
     * 接收到partFile
     *
     * @param socketMsgContent
     * @return
     */
    private void solvePartFile(SocketMsgContent socketMsgContent) {
        //保存对方发送来的文件
        EncodeFile.getSingleton().savePartFile(socketMsgContent);
    }

    private synchronized void solveLeave() {
        leaveFlag += 1;
        //关闭socket
        if (leaveFlag == 2)
            mySocket.close();
    }


    /**
     * 发送文件请求
     */
    private void sendFileQuestInfor() {
        SocketMsgContent socketMsgContent = new SocketMsgContent();
        socketMsgContent.serveOrClient = SocketMsgContent.SERVER_CLIENT_MSG;

        Vector<byte[]> requestVector = EncodeFile.getSingleton().getFileRequest(itsEncodeFile);
        if (requestVector.size() == 0) {
            //说明对方没有对自己有用的数据
            //离开
            socketMsgContent.code = SocketMsgContent.CODE_LEAVE;
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
            socketMsgContent.code = SocketMsgContent.CODE_FILE_REQUEST;

            socketMsgContent.requestLen = requestBytes.length;
            socketMsgContent.requestBytes = requestBytes;
        }

        mySocket.sendSocketMsgContent(socketMsgContent);
    }


}
