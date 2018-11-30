package connect;

import utils.ToolUtils;

public class ConnectConstant {
    public final static String SERVER_IP = "192.168.43.1";
    public final static int SERVER_PORT = 9000;
    public final static String PRE_SSID = "fileSharing";
    public final static String  MY_SSID;
    public final static String  AP_PASSWORD = "123456789";

    static {
        MY_SSID = PRE_SSID + "_" + ToolUtils.randomString(5);
    }

    // 检查是不是一个本应用需要的ssid
    public static boolean isRelateSSID(String ssid){
        int rightLen = MY_SSID.length();
        int index = ssid.indexOf(PRE_SSID);
        int len = ssid.length();
        return rightLen == len && index == 0;
    }
}
