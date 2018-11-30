package wifi.wifictrl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.mroot.filesharing.MainActivity;

import data.MsgType;

/**
 * 接收wifi状态广播
 */
public class WifiStateReceiver extends BroadcastReceiver {
    // 处理网络状态变化 接收到多次广播的情况
    // 如果网络断开 false  网络连接true
    public boolean isConnect;
    public String lastSSID;

    public WifiStateReceiver() {
        isConnect = false;
        lastSSID = "";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
            //signal strength changed

        } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {//wifi连接上与否
            // System.out.println("网络状态改变");
            // Log.e("hanhai","网络状态改变");
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                System.out.println("wifi网络连接断开");
                if(isConnect){
                    Log.e("hanhai", "wifi网络连接断开");
                    isConnect = false;
                }
            } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                //获取当前wifi名称
                String ssid = wifiInfo.getSSID();
                if(ssid .equals("<unknown ssid>")){
                    return;
                }
                System.out.println("连接到网络 " + ssid);
                if(!isConnect || !lastSSID.equals(ssid)){
                    Log.e("hanhai", "连接到网络 " + wifiInfo.getSSID());
                    lastSSID = ssid;
                    isConnect = true;
                    MainActivity.sendMsg2UIThread(
                            MsgType.CONNECT_WIFI_SUCCESS.ordinal(),ssid);
                }
            }
        } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {//wifi打开与否
            int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
            if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                System.out.println("系统关闭wifi");
                Log.e("hanhai", "系统关闭wifi");
            } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                System.out.println("系统开启wifi");
                Log.e("hanhai", "系统开启wifi");
                // 扫描wifi列表
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifiManager.startScan(); //<<<这里


                MainActivity.sendMsg2UIThread(
                        MsgType.OPEN_WIFI_SUCCESS.ordinal(),"");
            }
        }else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
            // 经测试 每15秒系统会自动扫描一次  小米4测试结果
            // 当连接上一个wifi时，扫描更慢 大于20秒

            Log.v("hanhai", "获取到wifi扫描结果");
            MainActivity.sendMsg2UIThread(
                    MsgType.WIFI_SCAN_SUCCESS.ordinal(),"");
            //WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//            List<ScanResult> list = wifiManager.getScanResults();
//            for (int i = 0; i < list.size(); i++) {
//                Log.e("hanhai", list.get(i).toString());
//                Log.e("hanhai", "ScanResult SSID = " + list.get(i).SSID);
//            }

        }

    }
}
