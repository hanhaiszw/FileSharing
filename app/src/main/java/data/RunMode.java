package data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;


import com.afollestad.materialdialogs.MaterialDialog;

import static android.content.Context.MODE_PRIVATE;

//运行模式
public class RunMode {
    public final static String OD_MODE = "OD模式";
    public final static String RS_MODE = "RS模式";
    public final static String NC_MODE = "NC模式";

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
        Log.e("hanhai", K + "");
        this.lastXMLFilePath = pref.getString("lastXMLFilePath", "");
        Log.e("hanhai", lastXMLFilePath);

        this.runModeString = pref.getString("runModeString", OD_MODE);

        this.selectStartPath = pref.getString("selectStartPath", "/storage/emulated/0");

        Log.e("hanhai", runModeString);
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

}
