package wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;


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
        WifiConfiguration wifiConfiguration = apAdmin.makeConfiguration("myAP","hanhai116",WifiAPBase.KEY_WPA);
        apAdmin.startAp(wifiConfiguration);
    }
    private void closeAP(){
        apAdmin.stopAp();
    }

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
        WifiConfiguration wifiConfiguration = wifiAdmin.makeConfiguration("hanhaithinkpad","hanhai116",WifiAPBase.KEY_WPA);
        wifiAdmin.addNetwork(wifiConfiguration);
    }

}
