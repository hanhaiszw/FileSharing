package com.example.mroot.filesharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.lzyzsd.circleprogress.CircleProgress;
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
import data.CachePath;
import data.MsgType;
import data.RunMode;
import nc.NCUtils;
import utils.MyThreadPool;
import utils.ToolUtils;
import wifi.wifictrl.WifiAPControl;
import wifi.wifictrl.WifiStateReceiver;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.sv_prompt)
    ScrollView scrollView;
    @BindView(R.id.tv_prompt)
    TextView tv_prompt;

    @BindView(R.id.tv_fileName)
    TextView tv_fileName;

    @BindView(R.id.btn_openServer)
    Button btn_openServer;
    @BindView(R.id.btn_openClient)
    Button btn_openClient;

    /**
     * 0 代表是提示信息模式
     * 1 代表是圆形进度模式
     */
    private final int PROMPT_VIEW = 0;
    private final int CIRCLE_PROGRESS_VIEW = 1;
    private int viewState;
    @BindView(R.id.layout_wavePro)
    LinearLayout layout_wavePro;

    @BindView(R.id.circle_progress)
    CircleProgress circleProgress;

    private long exitTime = 0;
    private int REQUEST_CODE_FROM_ACTIVITY = 1000;
    private static Context context;

    private WifiAPControl wifiAPControl;

    private static RunMode runMode;

    private static MainActivity mainActivity;

    // wifi广播
    private WifiStateReceiver wifiStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        context = MainActivity.this;
        mainActivity = this;

        requestPermissions();

        //全局抓取异常   并存储在本地
        AECrashHelper.initCrashHandler(getApplication(),
                new AECHConfiguration.Builder()
                        .setLocalFolderPath(CachePath.CRASH_PATH) //配置日志信息存储的路径
                        .setSaveToLocal(true).build()   //开启存储在本地功能
        );


        //滚动到最下面
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() ->
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN)));

        init();
    }

    private void init() {
        wifiAPControl = new WifiAPControl(this);
        runMode = new RunMode();
        //读取上一次的配置数据
        runMode.initRunMode(context);

        // 不再执行对原有文件的初始化载入
        // EncodeFile.updateSingleton(runMode.lastXMLFilePath);
        // sendMsg2UIThread(MsgType.ENCODE_FILE_CHANGE.ordinal(), "");

        // 切换默认视图
        if (viewState == PROMPT_VIEW) {
            switch_view();
        }

        // 关闭wifi和ap
        wifiAPControl.closeWifiAp();

        // 注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiStateReceiver = new WifiStateReceiver();
        registerReceiver(wifiStateReceiver, intentFilter);

    }


    @OnClick(R.id.btn_select_file)
    public void selectFile() {
        new LFilePicker()
                .withActivity(MainActivity.this)
                .withRequestCode(REQUEST_CODE_FROM_ACTIVITY)
                .withStartPath(runMode.selectStartPath)
                .withMutilyMode(false)
                .start();
    }

    @OnClick(R.id.btn_openClient)
    public void openClient() {
        wifiAPControl.openClient(true);
    }

    @OnClick(R.id.btn_openServer)
    public void openServer() {
        wifiAPControl.openServer();
    }

    @OnClick(R.id.btn_test)
    public void test1() {
        //NCUtils.test();
        //EncodeFile.getSingleton().recover();
        // 再编码测试
        //EncodeFile.getSingleton().test();
    }

    // 恢复初始化
    @OnClick(R.id.btn_initial)
    public void recoverInitial() {
        wifiAPControl.closeWifiAp();

        // EncodeFile.getSingleton().setInitSuccess(false);
        clearCache();
        EncodeFile.updateSingleton();
        updateEncodeFileInfo();
        setInitFlag();
        //ActivityManager manager = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
        //manager.restartPackage(getPackageName());
//        onBackPressed();
//        onBackPressed();
        //Intent intent = new Intent(this, Object.class);

        //startActivity(intent);
    }

    @OnClick(R.id.btn_switch_view)
    public void switch_view() {
        if (viewState == PROMPT_VIEW) {
            scrollView.setVisibility(View.GONE);
            layout_wavePro.setVisibility(View.VISIBLE);
            viewState = CIRCLE_PROGRESS_VIEW;

        } else if (viewState == CIRCLE_PROGRESS_VIEW) {
            scrollView.setVisibility(View.VISIBLE);
            layout_wavePro.setVisibility(View.GONE);
            viewState = PROMPT_VIEW;
        }
    }


    /**
     * 处理各个线程发来的消息
     */
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            MsgType msgType = MsgType.values()[msg.what];
            switch (msgType) {
                // 消息提示
                case SHOW_MSG:
                    String prompt = msg.obj.toString();
                    setPrompt(prompt);
                    break;
                // 更新状态球显示
                case ENCODE_FILE_CHANGE:
                    updateEncodeFileInfo();
                    break;

                // 处理wifi广播消息
                case OPEN_WIFI_SUCCESS:
                    wifiAPControl.openWifiSuccess();
                    break;
                case CONNECT_WIFI_SUCCESS:
                    String ssid = msg.obj.toString();
                    wifiAPControl.connectWifiSuccess(ssid);
                    break;
                case WIFI_SCAN_SUCCESS:
                    wifiAPControl.wifiScanSuccess();
                    break;


                // wifi状态切换
                //
                case SERVER_2_CLIENT:
                    wifiAPControl.server2client();
                    break;
                case CLIENT_2_SERVER:
                    wifiAPControl.client2server();
                    break;
                case SERVER_STATE_FLAG:
                    setServerFlag();
                    break;
                case CLIENT_STATE_FLAG:
                    setClientFlag();
                    break;


                // 打开文件
                // 因为失败后，有可能使用Toast弹窗，所以需要在主线程执行
                case OPEN_FILE:
                    File file = (File) msg.obj;
                    com.example.zpc.file.util.Utils.openFile(context, file);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };


    private void setClientFlag() {
        btn_openClient.setTextColor(Color.RED);
        btn_openServer.setTextColor(Color.BLACK);
    }

    private void setServerFlag() {
        btn_openServer.setTextColor(Color.RED);
        btn_openClient.setTextColor(Color.BLACK);
    }

    // 恢复初始化状态
    // 即server  client 都不变色
    private void setInitFlag() {
        btn_openServer.setTextColor(Color.BLACK);
        btn_openClient.setTextColor(Color.BLACK);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    Log.i("hanhai", "onActivityResult write settings granted");
                } else {
                    Log.i("hanhai", "onActivityResult write settings 被拒绝");
                }
            }
        } else if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_FROM_ACTIVITY) {
                //在这里获取到选择的文件完整路径
                List<String> list = data.getStringArrayListExtra("paths");
                //这里用的单选
                String filePath = list.get(0);
                //Toast.makeText(context, filePath, Toast.LENGTH_SHORT).show();
                Log.v("hanhai", filePath);
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
                    Log.v("hanhai", text.toString());
                    runMode.runModeString = text.toString();
                    //更新标题
                    setTitle(text);
                    return true;
                })
                .dismissListener(dialog -> {
                    //开始处理文件
                    String info = "当前运行模式: " + runMode.runModeString + " K = " + runMode.K;
                    Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
                    MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), info);

                    MyThreadPool.execute(() -> {
                        Log.i("hanhai", "文件预处理开始");
                        MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "文件预处理开始");
                        EncodeFile.updateSingleton(file, runMode.K, runMode.runModeString);
                        sendMsg2UIThread(MsgType.ENCODE_FILE_CHANGE.ordinal(), "");
                        Log.i("hanhai", "文件预处理结束");
                        MainActivity.sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "文件预处理结束");
                        //打开Server模式
                        openServer();
                    });
                })
                .show();
    }


    private void updateEncodeFileInfo() {
        EncodeFile encodeFile = EncodeFile.getSingleton();
        if (!encodeFile.isInitSuccess()) {
            setTitle("FileSharing");
            circleProgress.setProgress(0);
            circleProgress.setPrefixText("");
            circleProgress.setSuffixText("%");
            tv_fileName.setText("");
            return;
        }
        runMode.runModeString = encodeFile.getRunModeString();
        runMode.K = encodeFile.getK();
        runMode.lastXMLFilePath = encodeFile.getXmlFilePath();

        setTitle(runMode.runModeString);
        tv_fileName.setText(encodeFile.getFileName());
        // 更新进度球
        int currentPieceNum = encodeFile.getCurrentPieceNum();
        int totalPieceNum = encodeFile.getTotalPieceNum();

        circleProgress.setFinishedColor(runMode.getRunColor());
        int progress = (int) ((float) currentPieceNum / totalPieceNum * 100);
        circleProgress.setProgress(progress);
        circleProgress.setPrefixText(currentPieceNum + "/" + totalPieceNum + " (");
        circleProgress.setSuffixText("%)");
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            runMode.commitRunMode(this);
            finish();
            // 关闭wifi和ap
            wifiAPControl.closeWifiAp();

            // 清空多余的缓存
            clearCache();

            NCUtils.UninitGalois();
            MyThreadPool.shutdownNow();
            //android.os.Process.killProcess(android.os.Process.myPid());
            unregisterReceiver(wifiStateReceiver);
            //不会调用周期函数，如onDestroy()
            System.exit(0);
        }
    }

    private void clearCache() {
        // 清空不必要的缓存
        // 清空ReceiveTemp目录
        File receiveTemp = new File(CachePath.RECEIVE_TEMP_PATH);
        if (receiveTemp.exists() && receiveTemp.isDirectory()) {
            File[] files = receiveTemp.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    ToolUtils.deleteFile(file);
                } else if (file.isDirectory()) {
                    ToolUtils.deleteDir(file);
                }
            }
        }


        // 清空Temp目录
        File folder = new File(CachePath.TEMP_PATH);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            String encodeFilePath = EncodeFile.getSingleton().getFolderPath();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    ToolUtils.deleteFile(file);
                } else if (file.isDirectory()) {
                    // 不删除当前文件
                    if (!file.getPath().equals(encodeFilePath)) {
                        ToolUtils.deleteDir(file);
                    }
                    // 删除所有文件
                    //ToolUtils.deleteDir(file);
                }
            }
        }

    }


    // 设置提示信息
    private void setPrompt(String prompt) {
        // 获取的值是之前的行数加1 所以这里减1
        int lineNum = tv_prompt.getLineCount();
        Log.v("hanhai", "prompt中有" + lineNum + "行数据");
        // 控制最大显示行数为400行  最大显示其实为402行
        if (lineNum > 400) {
            String text = tv_prompt.getText().toString();
            // 去掉两行  下面会增加两行
            text = text.substring(text.indexOf("\n") + 1);
            text = text.substring(text.indexOf("\n") + 1);
            tv_prompt.setText(text);
        }
        String strTime = ToolUtils.getCurrentTime();
        tv_prompt.append(strTime + "\n");
        tv_prompt.append(prompt + "\n");
    }

    //返回context对象
    public static Context getContext() {
        return context;
    }

    //全局发送message到handle处理的方法
    private void send2UI(int what, Object obj) {
        //sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),"hello, world!");
        if (handler != null) {
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }

    public static void sendMsg2UIThread(int what, Object obj) {
        mainActivity.send2UI(what, obj);
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
                            int k = 0;
                            try {
                                k = Integer.parseInt(input.toString());
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                // 如果用户没有输入任何值，而点击了确定按钮，就会执行到此处
                                return;
                            }
                            if (k < 2 || k > 50) {  // 设置K值范围 2 到 50
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
                // 打开记录文件
            case R.id.item_open_logFile:
                String logFilePath = CachePath.LOG_PATH+File.separator+CachePath.LOG_FILE_NAME;
                ToolUtils.openFile(logFilePath);
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
        requestWriteSettings();
        // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .permission("android.permission.ACCESS_FINE_LOCATION")
                .onGranted(permissions -> {
                    // Storage permission are allowed.
                })
                .onDenied(permissions -> {
                    // Storage permission are not allowed.
                    AndPermission.with(this)
                            .runtime()
                            .setting()
                            .onComeback(() -> {
                                // 用户从设置回来了。
                                //Toast.makeText(this, "用户从设置回来了", Toast.LENGTH_SHORT).show();
                            })
                            .start();
                })
                .start();
    }

    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;

    /**
     * 注意：有时候热点打开会失败
     * 手机重启后成功
     */
    private void requestWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
            }
        }
    }

}
