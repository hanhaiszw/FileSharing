package cache;

import java.io.File;

import connect.MySocket;
import connect.SocketMsgContent;

/**
 * 解析Socket信息
 * 从缓存文件中取取数据
 */
public class SocketMsgParse {
    //对方的配置文件
    private EncodeFile itsEncodeFile;

    public SocketMsgParse() {

    }

    public void parse(MySocket mySocket, SocketMsgContent socketMsgContent) {
        int code = socketMsgContent.code;
        SocketMsgContent answer = null;
        switch (code) {
            //xml文件
            case 0:
                answer = solveXMLFile(socketMsgContent);
                break;
            case 1:

                break;
            case 2:
                break;
            default:
                break;
        }
        if (answer != null) {
            mySocket.sendSocketMsgContent(answer);
        }
    }


    //处理xml配置文件
    private SocketMsgContent solveXMLFile(SocketMsgContent socketMsgContent) {
        itsEncodeFile = EncodeFile.xml2obj(socketMsgContent.file.getPath());
        SocketMsgContent answer = null;
        if (socketMsgContent.serveOrClient == 0) {
            EncodeFile.updateSingleton(itsEncodeFile);
            File file = new File(EncodeFile.getSingleton().getXmlFilePath());
            answer = new SocketMsgContent(1, 0, 0, file);
        }
        return answer;
    }

    //接收到partFile
    private SocketMsgContent solvePartFile(SocketMsgContent socketMsgContent){
        EncodeFile.getSingleton().savePartFile(socketMsgContent);

        return null;
    }

}
