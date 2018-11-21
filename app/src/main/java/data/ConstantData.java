package data;

import android.provider.Settings;

import com.example.mroot.filesharing.MainActivity;

public class ConstantData {
    //android设备唯一识别码
    public final static String ANDROID_ID;
    static {
        ANDROID_ID = Settings.System.getString(MainActivity.getContext().getContentResolver(), Settings.System.ANDROID_ID);
    }
}
