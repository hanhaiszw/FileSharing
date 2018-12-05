package wifi.wifictrl;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

import com.example.mroot.filesharing.MainActivity;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cache.EncodeFile;
import connect.ConnectConstant;
import connect.MyClientSocket;
import connect.MyServerSocket;
import data.MsgType;
import wifi.wifibase.APAdmin;
import wifi.wifibase.WifiAPBase;
import wifi.wifibase.WifiAdmin;


public class WifiAPControl {
    private APAdmin apAdmin;
    private WifiAdmin wifiAdmin;

    //private Context context;

    // 分为三种状态
    // 0 无
    // 1 wifi状态
    // 2 ap状态
    private final static int STATE_NONE = 0;
    private final static int STATE_WIFI = 1;
    private final static int STATE_AP = 2;
    private int state;

    private MyServerSocket myServerSocket;
    private MyClientSocket myClientSocket;


    private SSIDSelect ssidSelect;
    private boolean hasUsefulSSID;
    private boolean connectNeedWifiSuccess;

    public WifiAPControl(Context context) {
        //this.context = context;
        apAdmin = new APAdmin(context);
        wifiAdmin = new WifiAdmin(context);
        myServerSocket = new MyServerSocket();
        myClientSocket = new MyClientSocket();


        ssidSelect = new SSIDSelect();
        state = STATE_NONE;

        hasUsefulSSID = false;
        connectNeedWifiSuccess = false;
    }


    /**
     * 配置并开启AP
     */
    private void openAP() {
        // 不是必要的
        closeWifi();
        WifiConfiguration wifiConfiguration = apAdmin.makeConfiguration(
                ConnectConstant.MY_SSID, ConnectConstant.AP_PASSWORD, WifiAPBase.KEY_WPA);
        apAdmin.startAp(wifiConfiguration);

        state = STATE_AP;
    }
    private void closeAP() {
        apAdmin.stopAp();
        //
        state = STATE_NONE;
    }

    private void openWifi() {
        closeAP();
        wifiAdmin.openWifi();
        state = STATE_WIFI;
        hasUsefulSSID = false;
        connectNeedWifiSuccess = false;
        if (wifiAdmin.isWifiEnabled()) {
            openWifiSuccess();
            wifiScanSuccess();
        }
    }

    private void closeWifi() {
        wifiAdmin.deleteContainSSid();
        wifiAdmin.closeWifi();
        state = STATE_NONE;
    }



    /**
     * 开启server状态
     */
    public void openServer() {
        // 文件没有准备好 无法开启服务
        if (!EncodeFile.getSingleton().isInitSuccess()) {
            MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "编码文件没有准备好，无法开启服务");
            return;
        }

        //取消myClientSocket的自动切换操作
        myClientSocket.cancelSwitchTimer();
        openAP();

        // 开启serverSocket
        myServerSocket.openServer(ConnectConstant.SERVER_PORT);
        MainActivity.sendMsg2UIThread(MsgType.SERVER_STATE_FLAG.ordinal(),"");
    }


    /**
     * 开启client状态
     */
    public void openClient() {
       openClient(false);
    }

    // 是否是手动点击的
    // 当是手动点击时   不允许切换
    public void openClient(boolean click){
        // 取消myServerSocket的自动切换动作
        myServerSocket.cancelSwitchTimer();

        // 打开wifi
        openWifi();
        // 异步方法
        // 连接server的操作不能在此执行
        // 再连接wifi热点成功后执行
        MainActivity.sendMsg2UIThread(MsgType.CLIENT_STATE_FLAG.ordinal(),"");

        if(!click){
            Timer timer = new Timer();
            long startTime = System.currentTimeMillis();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (connectNeedWifiSuccess) {
                        timer.cancel();
                        return;
                    }
                    // 超过15秒还没连接上时，取消定时器
                    if (System.currentTimeMillis() - startTime > 15 * 1000) {
                        client2server();
                        timer.cancel();
                    }
                }
            }, 0, 1000);
        }

    }

    /**
     * wifi广播处理方法
     */
    public void openWifiSuccess() {
        if (state == STATE_WIFI) {
            Log.e("hanhai", "打开wifi成功");
            MainActivity.sendMsg2UIThread(
                    MsgType.SHOW_MSG.ordinal(), "打开wifi成功");
            // 加快wifi扫描热点的速度
            Timer scanTimer = new Timer();
            scanTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (state != STATE_WIFI) {
                        scanTimer.cancel();
                    }
                    //执行具体的任务
                    wifiAdmin.startScan();
                }
            }, 0, 1000);



        }
    }


    public void connectWifiSuccess(String ssid) {
        // 这里执行连接ServerSocket逻辑
        // 不能在主线程执行连接网络操作
        Log.e("hanhai", "连接wifi成功");
        if (state == STATE_WIFI && !connectNeedWifiSuccess) {
            //连接的wifi不是需要的
            String wifiAdminSSID = wifiAdmin.currentConnectSSID();
            if (!wifiAdminSSID.equals("\"" + ssidSelect.selectedSSID + "\"")) {
                wifiAdmin.connectAP(ssidSelect.selectedSSID);
                return;
            }
            Log.e("hanhai", wifiAdminSSID);

            connectNeedWifiSuccess = true;

            Timer timer = new Timer();
            long startTime = System.currentTimeMillis();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (wifiAdmin.isWifiConnected()) {
                        Log.e("hanhai", "wifi可以使用了");
                        myClientSocket.connect(ConnectConstant.SERVER_IP, ConnectConstant.SERVER_PORT);
                        timer.cancel();
                    } else {
                        //Log.e("hanhai", "wifi不可用");
                    }
                    // 超过5秒还没连接上时，取消定时器
                    // 尝试切换为server状态
                    if (System.currentTimeMillis() - startTime > 5 * 1000) {
                        timer.cancel();
                    }
                }
            }, 0, 100);
        }
    }

    // 在此判断需要连接哪个wifi
    public void wifiScanSuccess() {
        Log.e("hanhai", "state = " + state + "  hasUsefulSSID = " + hasUsefulSSID);
        if (state == STATE_WIFI && !hasUsefulSSID) {
            Log.e("hanhai", "处理热点列表");

            List<ScanResult> list = wifiAdmin.getWifiScanResult();
            String ssid = ssidSelect.selectSSID(list);
            if (ssid != null) {
                hasUsefulSSID = true;
                String currentSSID = wifiAdmin.currentConnectSSID();
                Log.d("hanhai", "currentSSID = " + currentSSID);
                if (currentSSID.equals("\"" + ssid + "\"")) {
                    connectWifiSuccess(ssid);
                } else {
                    wifiAdmin.connectAP(ssid);
                }

            }


        }
    }


    /**
     * 状态转化方法
     */
    public void server2client() {
        EncodeFile encodeFile = EncodeFile.getSingleton();
        int currentPieceNum = encodeFile.getCurrentPieceNum();
        int totalPieceNum = encodeFile.getTotalPieceNum();
        if (currentPieceNum == totalPieceNum) {
            openServer();
        } else {
            openClient();
        }
    }

    public void client2server() {
        if (EncodeFile.getSingleton().isInitSuccess()) {
            // 需不需要等获取到一半数据后再准许切换向server
            EncodeFile encodeFile = EncodeFile.getSingleton();
            int currentPieceNum = encodeFile.getCurrentPieceNum();
            int totalPieceNum = encodeFile.getTotalPieceNum();
            if (currentPieceNum >= totalPieceNum / 2) {
                openServer();
            } else {
                openClient();
            }
        } else {
            openClient();
        }
    }


    /**
     * 清理方法
     * 关闭wifi和ap
     */
    public void closeWifiAp() {
        closeWifi();
        closeAP();
    }


}
