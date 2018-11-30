package wifi.wifibase;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

import java.util.List;

import connect.ConnectConstant;

public class WifiAdmin extends WifiAPBase {

    public WifiAdmin(Context context) {
        super(context);
    }

    /** 断开指定id的wifi **/
    public void disconnectWifi(int paramInt) {
        this.mWifiManager.disableNetwork(paramInt);
    }

    public WifiConfiguration makeConfiguration(String ssid, String passawrd, int authAlogrithm) {
        return super.makeConfiguration(ssid, passawrd, authAlogrithm, WIFI_CLIENT_MODE);
    }

    /** 添加并连接指定网络 **/
    public void addNetwork(WifiConfiguration paramWifiConfiguration) {
        if (!super.getWifiState()) {
            return;
        }
        WifiConfiguration tempConfiguration = isExsits(paramWifiConfiguration.SSID);
        if (tempConfiguration != null) {
            mWifiManager.removeNetwork(tempConfiguration.networkId); // 从列表中删除指定的网络配置网络
        }
        int i = mWifiManager.addNetwork(paramWifiConfiguration);
        mWifiManager.enableNetwork(i, true);
    }

    public void startScan(){
        mWifiManager.startScan();
    }

    // 需要在广播WifiManager.SCAN_RESULTS_AVAILABLE_ACTION后调用才能获取到最新列表
    public List<ScanResult> getWifiScanResult(){
        return mWifiManager.getScanResults();
    }

    /**
     * 连接指定配置好的网络
     *
     * @param index 配置好网络的ID
     */
    public void connectConfiguration(int index) {
        super.openWifi();
        // 索引大于配置好的网络索引返回
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId, true);
    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    // 判断连接的wifi是否可以数据通信了
    public boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        }

        return false;
    }

    //获取当前wifi连接的ssid
    public String currentConnectSSID() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return wifiInfo.getSSID();
    }

    // 删除之前以后保存过的网络信息
    // 删除相关的ssid
    // 需要在wifi打开的状态下才能成功删除
    public void deleteContainSSid() {
        try {
            List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration existingConfig : existingConfigs) {
                String ssid = existingConfig.SSID;
                if (ssid.contains(ConnectConstant.PRE_SSID)) {
                    int networkId = existingConfig.networkId;
                    mWifiManager.removeNetwork(networkId);
                    mWifiManager.saveConfiguration();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
