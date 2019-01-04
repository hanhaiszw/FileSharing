package data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

//运行模式
public class RunMode {
    /**
     * 三种模式
     */
    public final static String OD_MODE = "OD模式";
    public final static String RS_MODE = "RS模式";
    public final static String NC_MODE = "NC模式";
    /**
     * 三种模式选择的进度球颜色值
     */
    private final static int COLOR_OD = 0x88ff0000;
    private final static int COLOR_RS = 0xff90EE90;
    private final static int COLOR_NC = 0x880000ff;
    private static Map<String, Integer> colorMap = new HashMap<>();

    static {
        colorMap.put(OD_MODE, COLOR_OD);
        colorMap.put(RS_MODE, COLOR_RS);
        colorMap.put(NC_MODE, COLOR_NC);
    }


    public String runModeString;
    public int K;
    public String lastXMLFilePath;
    public String selectStartPath;


    public RunMode() {
        runModeString = OD_MODE;
        K = 4;
        lastXMLFilePath = "";
    }

    public void initRunMode(Context context) {
        SharedPreferences pref = context.getSharedPreferences("data", MODE_PRIVATE);
        this.K = pref.getInt("K", 4);
        Log.v("hanhai", K + "");
        this.lastXMLFilePath = pref.getString("lastXMLFilePath", "");
        Log.v("hanhai", lastXMLFilePath);

        this.runModeString = pref.getString("runModeString", OD_MODE);

        this.selectStartPath = pref.getString("selectStartPath", "/storage/emulated/0");

        Log.v("hanhai", runModeString);
    }

    @SuppressLint("ApplySharedPref")
    public void commitRunMode(Context context) {
        //使用 editor.apply()失败  改用editor.commit()保存成功
        //apply异步   commit同步
        SharedPreferences.Editor editor = context.getSharedPreferences("data", MODE_PRIVATE).edit();
        editor.putString("lastXMLFilePath", lastXMLFilePath);
        Log.e("hanhai", lastXMLFilePath);

        editor.putInt("K", K);
        editor.putString("runModeString", runModeString);
        editor.putString("selectStartPath", selectStartPath);
        editor.commit();
    }

    public int getRunColor() {
        return colorMap.get(runModeString);
    }

}
