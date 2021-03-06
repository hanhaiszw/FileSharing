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
    private volatile int state;

    private MyServerSocket myServerSocket;
    private MyClientSocket myClientSocket;


    private SSIDSelect ssidSelect;
    // 用以判断是否已经找到了有用的热点
    // 暂时弃用此变量   2.7
    private volatile boolean hasUsefulSSID;
    // 用以标记已经连接到了有用的热点
    private boolean connectNeedWifiSuccess;


    public WifiAPControl(Context context) {
        // 初始化
        init(context);

    }

    private void init(Context context) {
        apAdmin = new APAdmin(context);
        wifiAdmin = new WifiAdmin(context);
        myServerSocket = new MyServerSocket();
        myClientSocket = new MyClientSocket();


        ssidSelect = new SSIDSelect();
        //
        state = STATE_NONE;
        hasUsefulSSID = false;
        connectNeedWifiSuccess = false;

        // 加快wifi扫描热点的速度
        Timer scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (state == STATE_WIFI) {
                    //wifiScanSuccess();
                    //执行具体的任务
                    wifiAdmin.startScan();
                }

            }
        }, 0, 1000);
    }


    /**
     * 配置并开启AP
     */
    private void openAP() {
        // 不是必要的
        closeWifi();
//        WifiConfiguration wifiConfiguration = apAdmin.makeConfiguration(
//                ConnectConstant.MY_SSID, ConnectConstant.AP_PASSWORD, WifiAPBase.KEY_WPA);
        WifiConfiguration wifiConfiguration = apAdmin.makeConfiguration(
                ConnectConstant.MY_SSID, ConnectConstant.AP_PASSWORD, WifiAPBase.KEY_NONE);

        apAdmin.startAp(wifiConfiguration);

//        while(true){
//            if(apAdmin.isWifiApEnabled()){
//                break;
//            }
//        }
        state = STATE_AP;
    }

    private void closeAP() {
        apAdmin.stopAp();
        //
        state = STATE_NONE;
    }

    private void openWifi() {
        Log.i("hanhai", "准备打开wifi");

        closeAP();

        // 为了适配Android8.0   AP关闭缓慢导致wifi打开失败的问题
        Timer timer = new Timer();
        long startTime = System.currentTimeMillis();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!wifiAdmin.isWifiEnabled()) {
                    wifiAdmin.openWifi();
                } else {
                    timer.cancel();
                }
                // 超过3秒还没连接上时，取消定时器
                if (System.currentTimeMillis() - startTime > 3 * 1000) {
                    timer.cancel();
                }
            }
        }, 0, 1000);


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
        hasUsefulSSID = false;
        connectNeedWifiSuccess = false;
    }


    /**
     * 开启server状态
     */
    public void openServer() {
        Log.i("hanhai", "准备打开ap");

        // 文件没有准备好 无法开启服务
        if (!EncodeFile.getSingleton().isInitSuccess()) {
            MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "编码文件没有准备好，无法开启服务");
            return;
        }

        //取消myClientSocket的自动切换操作
        myClientSocket.cancelSwitchTimer();
        myServerSocket.cancelSwitchTimer();

        openAP();

        // 开启serverSocket
        myServerSocket.openServer(ConnectConstant.SERVER_PORT);
        MainActivity.sendMsg2UIThread(MsgType.SERVER_STATE_FLAG.ordinal(), "");


        Log.i("hanhai", "ap打开成功");
    }


    /**
     * 开启client状态
     */
    public void openClient() {
        openClient(false);
    }

    // 是否是手动点击的
    // 当是手动点击时   不允许切换
    public void openClient(boolean click) {
        //userClickClient = click;
        // 取消myServerSocket的自动切换动作
        myServerSocket.cancelSwitchTimer();
        myClientSocket.cancelSwitchTimer();

        // 打开wifi
        openWifi();
        // 异步方法
        // 连接server的操作不能在此执行
        // 再连接wifi热点成功后执行
        MainActivity.sendMsg2UIThread(MsgType.CLIENT_STATE_FLAG.ordinal(), "");

        // 当处于client模式时，不再使用连接超时切换
//        if (!click) {
//            Timer timer = new Timer();
//            long startTime = System.currentTimeMillis();
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    if (connectNeedWifiSuccess) {
//                        timer.cancel();
//                        return;
//                    }
//                    // 超过15秒还没连接上时，取消定时器
//                    if (System.currentTimeMillis() - startTime > 15 * 1000) {
//                        client2server();
//                        timer.cancel();
//                    }
//                }
//            }, 0, 1000);
//        } else {
//            // 阻止其自动切换
//        }
    }

    /**
     * wifi广播消息处理方法
     */
    public void openWifiSuccess() {
        //
        if (state == STATE_WIFI) {
            Log.i("hanhai", "wifi打开成功");
        }
    }

    public void connectWifiSuccess(String ssid) {
        // 这里执行连接ServerSocket逻辑
        // 不能在主线程执行连接网络操作
        Log.i("hanhai", "连接wifi成功");
        if (state == STATE_WIFI && !connectNeedWifiSuccess) {
            //连接的wifi不是需要的
            String wifiAdminSSID = wifiAdmin.currentConnectSSID();
            if (!wifiAdminSSID.equals("\"" + ssidSelect.selectedSSID + "\"")) {
                wifiAdmin.connectAP(ssidSelect.selectedSSID);
                return;
            }
            Log.i("hanhai", wifiAdminSSID);

            connectNeedWifiSuccess = true;

            // 连接ServerSocket
            Timer timer = new Timer();
            long startTime = System.currentTimeMillis();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (wifiAdmin.isWifiConnected()) {
                        Log.i("hanhai", "wifi可以使用了");
                        boolean ret = myClientSocket.connect(ConnectConstant.SERVER_IP, ConnectConstant.SERVER_PORT);
                        // 可能出现的卡在client的原因
                        // 连接serverSocket失败  没有再进行重连
                        if (ret) {
                            timer.cancel();
                            //
                            //Log.d("hanhai", "连接SocketServer定时器取消");
                        }
                    } else {
                        //Log.i("hanhai", "wifi不可用");
                    }
                    // 超过5秒还没连接上时，取消定时器
                    // 尝试切换为server状态
                    if (System.currentTimeMillis() - startTime > 5 * 1000) {
                        timer.cancel();
                        // 连接失败，允许重新选择热点连接
                        hasUsefulSSID = false;
                        connectNeedWifiSuccess = false;
                        wifiAdmin.deleteContainSSid();
                    }
                }
            }, 0, 1000);
        }
    }

    // 在此判断需要连接哪个wifi
    public void wifiScanSuccess() {
        //Log.v("hanhai", "state = " + state + "  hasUsefulSSID = " + hasUsefulSSID);
        //  && !hasUsefulSSID
        if (state == STATE_WIFI && !connectNeedWifiSuccess) {
            Log.v("hanhai", "处理热点列表");
            List<ScanResult> list = wifiAdmin.getWifiScanResult();

            String ssid = ssidSelect.selectSSID(list);

            if (ssid != "") {
                Log.d("hanhai", "获取到有效ssid");
                //hasUsefulSSID = true;
                String currentSSID = wifiAdmin.currentConnectSSID();
                Log.v("hanhai", "currentSSID = " + currentSSID);
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
        // 当已经拥有所有数据时，不再切换
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
                // 查看附近有多少热点
                // 数据测试时用的是3
                // 如果附近热点数目大于5  则不切换  继续在client状态下工作
//                if (currentPieceNum >= totalPieceNum) {
//                    openServer();
//                } else {
                    // 取消此部分优化
//                    List<ScanResult> list = wifiAdmin.getWifiScanResult();
//                    List<SSIDSelect.SSIDInfo> ssidInfoList = ssidSelect.sortUsefulAPByRssi(list);
//                    // 附近已经有3个以上的热点了, 而且本机数据还没接收完全，就不要再切换到server了
//                    if (ssidInfoList.size() >= 10) {
//                        openClient();
//                    } else {
//                        openServer();
//                    }
//                    openServer();
//                }
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
        myServerSocket.cancelSwitchTimer();
        myClientSocket.cancelSwitchTimer();
        closeWifi();
        closeAP();
        state = STATE_NONE;
    }

}
