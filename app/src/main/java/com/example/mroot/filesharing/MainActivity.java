package com.example.mroot.filesharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.mauiie.aech.AECHConfiguration;
import com.mauiie.aech.AECrashHelper;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import data.CachePath;
import data.MsgType;
import wifi.APAdmin;
import wifi.WifiAPBase;
import wifi.WifiAPControl;
import wifi.WifiAdmin;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.sample_text)
    TextView tv;
    private long exitTime = 0;
    private int REQUESTCODE_FROM_ACTIVITY = 1000;
    private static Context context;

    WifiAPControl wifiAPControl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        context = MainActivity.this;
        tv.setText("hello");

        requestPermissions();

        //全局抓取异常   并存储在本地
        AECrashHelper.initCrashHandler(getApplication(),
                new AECHConfiguration.Builder()
                        .setLocalFolderPath("/storage/emulated/0/1Sharing") //配置日志信息存储的路径
                        .setSaveToLocal(true).build()   //开启存储在本地功能
        );



        wifiAPControl = new WifiAPControl(this);
    }


    @OnClick(R.id.btn_select_file)
    public void selectFile() {
        new LFilePicker()
                .withActivity(MainActivity.this)
                .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                .withStartPath("/storage/emulated/0")
                .withMutilyMode(false)
                .start();
    }

    @OnClick(R.id.btn_test)
    public void test() {
        wifiAPControl.openWifi();
    }

    @OnClick(R.id.btn_openAP)
    public void openAP() {
        wifiAPControl.openAP();
    }

    //处理各个线程发来的消息
    @SuppressLint("HandlerLeak")
    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            MsgType msgType = MsgType.values()[msg.what];
            switch (msgType) {
                case SHOW_MSG:
                    Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUESTCODE_FROM_ACTIVITY) {
                //在这里获取到选择的文件完整路径
                List<String> list = data.getStringArrayListExtra("paths");
                for (String s : list) {
                    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            //不会调用周期函数，如onDestroy()
            System.exit(0);
        }
    }

    //返回context对象
    public static Context getContext() {
        return context;
    }

    //全局发送message到handle处理的方法
    public static void sendMsg2UIThread(int what, Object obj) {
        //sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),"hello, world!");
        if (handler != null) {
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("程序退出");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //文件管理器
            case R.id.item_file_manager:
                String path = CachePath.APP_PATH;
                Intent intent = new Intent(MainActivity.this, com.example.zpc.file.MainActivity.class);
                intent.putExtra("extra_path", path);
                startActivity(intent);
                break;
            //设置K值
            case R.id.item_set_k_value:
                new MaterialDialog.Builder(this)
                        .title("设置K值")
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input("6", null, (dialog, input) -> {
                            int k = Integer.parseInt(input.toString());
                            Toast.makeText(MainActivity.this, k + "", Toast.LENGTH_SHORT).show();
                        })
                        .positiveText("确认")
                        .negativeText("取消")
                        .show();
                break;
            //权限管理
            case R.id.item_permission_manager:
                Toast.makeText(this, "请允许所有权限", Toast.LENGTH_SHORT).show();
                AndPermission.with(this)
                        .runtime()
                        .setting()
                        .onComeback(() -> {
                            // 用户从设置回来了。
                            //Toast.makeText(this, "用户从设置回来了", Toast.LENGTH_SHORT).show();
                        })
                        .start();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 申请权限
     */
    private void requestPermissions() {
        // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）

        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onGranted(permissions -> {
                    // Storage permission are allowed.
                })
                .onDenied(permissions -> {
                    // Storage permission are not allowed.
                })
                .start();
    }


}
