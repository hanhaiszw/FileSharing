package cache;

import com.example.mroot.filesharing.MainActivity;

import data.CachePath;
import data.MsgType;
import utils.ToolUtils;

// 统计连接数 有效连接数
public class ConnectCount {
    private static int totalConnectNum = 0;
    private static int usefulConnectNum = 0;

    private static void clearCount() {
        totalConnectNum = 0;
        usefulConnectNum = 0;
    }

    public synchronized static void addTotalConnect() {
        // 只有未完成分享时，才统计其连接次数
        EncodeFile encodeFile = EncodeFile.getSingleton();
        int currentPieceNum = encodeFile.getCurrentPieceNum();
        int totalPieceNum = encodeFile.getTotalPieceNum();
        if (!encodeFile.isInitSuccess() || currentPieceNum != totalPieceNum) {
            ++totalConnectNum;
        }
    }

    public synchronized static void addUsefulConnect() {
        // 只有未完成分享时，才统计其连接次数
        EncodeFile encodeFile = EncodeFile.getSingleton();
        int currentPieceNum = encodeFile.getCurrentPieceNum();
        int totalPieceNum = encodeFile.getTotalPieceNum();
        if (!encodeFile.isInitSuccess() || currentPieceNum != totalPieceNum) {
            ++usefulConnectNum;
        }
    }


    // 写入结果文件
    public synchronized static void write2logFile() {
        String msg = "有效连接/总连接：" + usefulConnectNum + "/" + totalConnectNum;
        String fileMsg = "\n" + ToolUtils.getCurrentTime() + "\n" + msg + "\n\n";
        MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), msg);
        byte[] bt_fileMsg = fileMsg.getBytes();
        ToolUtils.writeToFile(CachePath.LOG_PATH, CachePath.LOG_FILE_NAME, bt_fileMsg, bt_fileMsg.length, true);

        // 记录之后，清除之前计数
        clearCount();
    }

}
