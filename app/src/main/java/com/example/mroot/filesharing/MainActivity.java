package com.example.mroot.filesharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.mauiie.aech.AECHConfiguration;
import com.mauiie.aech.AECrashHelper;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cache.EncodeFile;
import connect.ConnectConstant;
import connect.MyClientSocket;
import connect.MyServerSocket;
import data.CachePath;
import data.MsgType;
import data.RunMode;
import nc.NCUtils;
import utils.MyThreadPool;
import wifi.WifiAPControl;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.sample_text)
    TextView tv;
    private long exitTime = 0;
    private int REQUESTCODE_FROM_ACTIVITY = 1000;
    private static Context context;

    private WifiAPControl wifiAPControl;

    private MyServerSocket myServerSocket;
    private MyClientSocket myClientSocket;

    //网络编码中的GenerationSize
    // private int K;

    private static RunMode runMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        context = MainActivity.this;
        //tv.setText("hello");

        requestPermissions();

        //全局抓取异常   并存储在本地
        AECrashHelper.initCrashHandler(getApplication(),
                new AECHConfiguration.Builder()
                        .setLocalFolderPath(CachePath.CRASH_PATH) //配置日志信息存储的路径
                        .setSaveToLocal(true).build()   //开启存储在本地功能
        );

        init();
    }

    private void init() {
        wifiAPControl = new WifiAPControl(this);
        myServerSocket = new MyServerSocket();
        myClientSocket = new MyClientSocket();
        runMode = new RunMode();
        //读取上一次的配置数据
        runMode.initRunMode(context);

        EncodeFile.updateSingleton(runMode.lastXMLFilePath);
        setTitle(runMode.runModeString);

//        File file = new File(runMode.lastXMLFilePath);
//        if (!file.exists()) {
//            Log.e("hanhai", "文件名为 " + file.getName());
//            Log.e("hanhai", "文件长度为 " + (int) file.length());
//        }
    }


    @OnClick(R.id.btn_select_file)
    public void selectFile() {
        new LFilePicker()
                .withActivity(MainActivity.this)
                .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                .withStartPath(runMode.selectStartPath)
                .withMutilyMode(false)
                .start();
    }

    @OnClick(R.id.btn_openWifi)
    public void test() {
        wifiAPControl.openWifi();

    }

    @OnClick(R.id.btn_test)
    public void test1() {
//        MyThreadPool.execute(() -> {
//            myClientSocket.connect(ConnectConstant.SERVER_IP,ConnectConstant.SERVER_PORT);
//            myClientSocket.sendFile(new File(CachePath.APP_PATH + File.separator+"My Heart Will Go On.mp4"));
//        });
        byte[] a = {112, 34, 56, (byte) 234};
        byte[] b = {113, 23, 45, 89};
        byte[] ret = NCUtils.mul(a, b);
    }

    @OnClick(R.id.btn_openAP)
    public void openAP() {
        wifiAPControl.openAP();
        myServerSocket.openServer(ConnectConstant.SERVER_PORT);

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
                //这里用的单选
                String filePath = list.get(0);
                //Toast.makeText(context, filePath, Toast.LENGTH_SHORT).show();
                Log.e("hanhai", filePath);
                File file = new File(filePath);
                solveSelectFile(file);
            }
        }
    }

    private void solveSelectFile(File file) {
        runMode.selectStartPath = file.getParent();
        new MaterialDialog.Builder(context)
                .title("请选择运行模式")
                .positiveText("确认")
                .items(new String[]{RunMode.OD_MODE, RunMode.RS_MODE, RunMode.NC_MODE})
                .itemsCallbackSingleChoice(-1, (dialog, view, which, text) -> {
                    Log.e("hanhai", text.toString());
                    runMode.runModeString = text.toString();
                    //更新标题
                    setTitle(text);
                    return true;
                })
                .dismissListener(dialog -> {
                    //开始处理文件
                    Toast.makeText(MainActivity.this, "当前运行模式" +
                            runMode.runModeString, Toast.LENGTH_SHORT).show();
                    MyThreadPool.execute(() -> {
                        Log.d("hanhai", "文件预处理开始");
                        EncodeFile.updateSingleton(file, runMode.K, runMode.runModeString);
                        //EncodeFile encodeFile = EncodeFile.getSingleton();
//                    for (int i = 0; i < 10; i++) {
//                        //初始化待发送文件
//                        int index = i + 1;
//
//                        Log.d("hanhai", "第" + index + "次预处理开始");


                        //Log.d("hanhai", "文件预处理开始");
                        //EncodeFile.updateSingleton(file, runMode.K, runMode.runModeString);
                        //encodeFile.recover();
//                        Log.d("hanhai", "第" + index + "次执行结束");
//                    }
                    });
                })
                .show();
    }


    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            //注意：写在finish后的话，存值失败
            runMode.lastXMLFilePath = EncodeFile.getSingleton().getXmlFilePath();
            runMode.commitRunMode(context);

            finish();
            NCUtils.UninitGalois();
            MyThreadPool.shutdownNow();
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
                Intent intent = new Intent(MainActivity.this,
                        com.example.zpc.file.MainActivity.class);
                intent.putExtra("extra_path", path);
                startActivity(intent);
                break;
            //设置K值
            case R.id.item_set_k_value:
                new MaterialDialog.Builder(this)
                        .title("设置K值")
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input(runMode.K + "", null, (dialog, input) -> {
                            int k = Integer.parseInt(input.toString());
                            if (k < 2 || k > 10) {
                                Toast.makeText(MainActivity.this, "K值在2到10之间", Toast.LENGTH_SHORT).show();
                            } else {
                                if (k != runMode.K) {
                                    runMode.K = k;
                                }
                            }
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
