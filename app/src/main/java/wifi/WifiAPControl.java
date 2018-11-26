package wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import connect.ConnectConstant;


public class WifiAPControl {
    APAdmin apAdmin;
    WifiAdmin wifiAdmin;

    Context context;


    public WifiAPControl(Context context) {
        this.context = context;
        apAdmin = new APAdmin(context);
        wifiAdmin = new WifiAdmin(context);
    }


    //配置并开启AP
    public void openAP(){
        WifiConfiguration wifiConfiguration = apAdmin.makeConfiguration(
                ConnectConstant.MY_SSID,ConnectConstant.AP_PASSWORD,WifiAPBase.KEY_WPA);
        apAdmin.startAp(wifiConfiguration);
    }
    private void closeAP(){
        apAdmin.stopAp();
    }

    /**
     * 打开wifi并连接AP
     */
    public void openWifi(){
        closeAP();
        wifiAdmin.openWifi();
        connectAP();
    }
    private void closeWifi(){
        wifiAdmin.closeWifi();
    }


    //连接指定的AP
    private void connectAP(){
        WifiConfiguration wifiConfiguration = wifiAdmin.makeConfiguration(
                ConnectConstant.MY_SSID, ConnectConstant.AP_PASSWORD, WifiAPBase.KEY_WPA);
        wifiAdmin.addNetwork(wifiConfiguration);
    }

}
