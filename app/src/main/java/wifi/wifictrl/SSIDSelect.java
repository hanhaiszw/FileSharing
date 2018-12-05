package wifi.wifictrl;

import android.net.wifi.ScanResult;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import connect.ConnectConstant;

/**
 * 选择热点的原则
 * 前5次已经连接过的热点在选择排队中将处于劣势
 * 尽可能连接新热点
 */
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
        // 找到合适的热点
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
                    ssidInfo.weight = ssidInfo1.weight;
                    break;
                }
            }
        }

        // 找到权值最大的ssid
        int weight = Integer.MIN_VALUE;
        String ssid = null;
        for (SSIDInfo ssidInfo : newSSIDVector) {
            if (ssidInfo.weight > weight) {
                ssid = ssidInfo.ssid;
                weight = ssidInfo.weight;
            }
        }

        // 更新权值
        boolean findSuccess = false;
        for (SSIDInfo ssidInfo : ssidInfoConnected) {
            ssidInfo.weight += 1 ;
            // 这里经常报空指针异常 为什么？？？
            try {
                if (ssidInfo.ssid.equals(ssid)) {
                    findSuccess = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }


        if(!findSuccess){
            ssidInfoConnected.add(new SSIDInfo(ssid, LOW_WEIGHT));
        }


        // 把刚连接的热点权值设置为0
        for (SSIDInfo ssidInfo : newSSIDVector) {
            if (ssidInfo.equals(ssid)) {
                ssidInfo.weight = LOW_WEIGHT;
            }
        }

        // 当 weight == 0时，从列表删除
        Iterator<SSIDInfo> it = ssidInfoConnected.iterator();
        while (it.hasNext()) {
            SSIDInfo ssidInfo = it.next();
            if (ssidInfo.weight == 0) {
                it.remove();
            }
        }

        selectedSSID = ssid;
        return ssid;
    }


    private class SSIDInfo {
        String ssid = "";
        int weight = 0;

        SSIDInfo(String ssid,int weight) {
            this.ssid = ssid;
            this.weight = weight;
        }
        SSIDInfo(String ssid) {
            this.ssid = ssid;
        }
    }
}
