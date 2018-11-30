package wifi.wifictrl;

import android.net.wifi.ScanResult;

import java.util.List;
import java.util.Vector;

import connect.ConnectConstant;

public class SSIDSelect {
    // 已经连接过的wifi信息
    Vector<SSIDInfo> ssidInfoConnected;
    // 最小的权值
    private final static int LOW_WEIGHT = -5;


    public String selectedSSID = "";

    SSIDSelect() {
        ssidInfoConnected = new Vector<>();

    }


    public String selectSSID(List<ScanResult> list) {
        Vector<SSIDInfo> newSSIDVector = new Vector<>();
        for (int i = 0; i < list.size(); i++) {
            //Log.e("hanhai", list.get(i).toString());
            //Log.e("hanhai", "ScanResult SSID = " + list.get(i).SSID);
            String ssid = list.get(i).SSID;
            if (ConnectConstant.isRelateSSID(ssid)) {
                newSSIDVector.add(new SSIDInfo(ssid));
            }
        }


        // 重新赋权值
        for (SSIDInfo ssidInfo : newSSIDVector) {
            for (SSIDInfo ssidInfo1 : ssidInfoConnected) {
                if (ssidInfo.ssid.equals(ssidInfo1.ssid)) {
                    ssidInfo1.weight += 1;
                    ssidInfo.weight = ssidInfo1.weight;
                    break;
                }
            }
        }

        int weight = Integer.MIN_VALUE;
        String ssid = null;
        for (SSIDInfo ssidInfo : newSSIDVector) {
            if (ssidInfo.weight > weight) {
                ssid = ssidInfo.ssid;
                weight = ssidInfo.weight;
            }
        }

        for (SSIDInfo ssidInfo : ssidInfoConnected) {
            if (ssidInfo.ssid.equals(ssid)) {
                ssidInfo.weight = LOW_WEIGHT;
                break;
            }
        }

        selectedSSID = ssid;
        return ssid;
    }


    private class SSIDInfo {
        String ssid;
        int weight = 0;

        SSIDInfo(String ssid) {
            this.ssid = ssid;
        }
    }
}
