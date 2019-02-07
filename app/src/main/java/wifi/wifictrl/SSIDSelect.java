package wifi.wifictrl;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import connect.ConnectConstant;
import connect.MySocket;

/**
 * 选择热点的原则
 * 前3次已经连接过的热点在选择排队中将处于劣势
 * 尽可能连接新热点
 */
public class SSIDSelect {
    // 已经连接过的wifi信息
    Vector<SSIDInfo> ssidInfoConnected;

    public String selectedSSID = "";

    // 用来记录最近已经连接过的热点信息
    // 记录长度为3
    private Queue<String> connectedSSID;

    SSIDSelect() {
        ssidInfoConnected = new Vector<>();
        connectedSSID = new LinkedList<>();
    }

    public String selectSSID(List<ScanResult> list) {
        List<SSIDInfo> ssidInfoList = sortUsefulAPByRssi(list);
        for (SSIDInfo ssidInfo : ssidInfoList) {
            Log.d("hanhai", ssidInfo.ssid);
            Log.d("hanhai", "信号强度 = " + ssidInfo.signalLevel);
        }

        // 按信号强度
        // 选择的结果
        String retSsid = "";
        int size = ssidInfoList.size();
        for (int i = 0; i < size; i++) {
            String tempSsid = ssidInfoList.get(i).ssid;
            //int signalLevel = ssidInfoList.get(i).signalLevel;
            if (!connectedSSID.contains(tempSsid)) {
                retSsid = tempSsid;
                break;
            }
        }

        // 如果都连接过了，则选择信号最强的那个进行连接
        if (retSsid == "" && size != 0) {
            retSsid = ssidInfoList.get(0).ssid;
        }

        // 把已经选择的ssid加入到已连接过的ssid列表
        if (retSsid != "") {
            selectedSSID = retSsid;
            connectedSSID.add(retSsid);
        }

        // 当已已连接的列表大于3时，删除最久已连接的ssid信息
        if (connectedSSID.size() > 3) {
            // 删除头元素
            // 最先加入的元素
            connectedSSID.poll();
        }

        return retSsid;
    }


    public List<SSIDInfo> sortUsefulAPByRssi(List<ScanResult> list) {
        List<ScanResult> results = new Vector<>();
        // 0 到 -50  最强
        // -50 到70 较强
        // -70到-80 较弱
        // -100 到 -80 微弱
        for (ScanResult scanResult : list) {
            String ssid = scanResult.SSID;
            //results.add(scanResult);
            if (ConnectConstant.isRelateSSID(ssid)) {
                results.add(scanResult);
            }
        }
        //按照从强到弱排序
        Collections.sort(results, new Comparator<ScanResult>() {
            public int compare(ScanResult sr1, ScanResult sr2) {
                // 按照强度 从大到小排列
                return Integer.compare(sr2.level, sr1.level);
            }
        });


        List<SSIDInfo> ssidInfos = new Vector<>();
        for (ScanResult scanResult : results) {
            String ssid = scanResult.SSID;
            // 信号强度从0到3
            int signalLevel = WifiManager.calculateSignalLevel(scanResult.level, 4);
            // 把信号强度大于1的加入列表
            if (signalLevel > 1) {
                SSIDInfo ssidInfo = new SSIDInfo(ssid, signalLevel);
                ssidInfos.add(ssidInfo);
            }
        }

        return ssidInfos;
    }


    class SSIDInfo {
        String ssid = "";
        int signalLevel = 0;

        SSIDInfo(String ssid, int signalLevel) {
            this.ssid = ssid;
            this.signalLevel = signalLevel;
        }
    }
}
