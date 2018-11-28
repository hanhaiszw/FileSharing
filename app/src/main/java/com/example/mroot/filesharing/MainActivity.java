package com.example.mroot.filesharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    @BindView(R.id.sv_prompt)
    ScrollView scrollView;
    @BindView(R.id.tv_prompt)
    TextView tv_prompt;

    @BindView(R.id.tv_fileName)
    TextView tv_fileName;

    /**
     * 0 代表是提示信息模式
     * 1 代表是圆形进度模式
     */
    private final int PROMPT_VIEW = 0;
    private final int WAVEPROGRESS_VIEW = 1;
    private int viewState;
    @BindView(R.id.layout_wavePro)
    LinearLayout layout_wavePro;

    @BindView(R.id.circle_progress)
    CircleProgress circleProgress;

    private long exitTime = 0;
    private int REQUEST_CODE_FROM_ACTIVITY = 1000;
    private static Context context;

    private WifiAPControl wifiAPControl;

    private MyServerSocket myServerSocket;
    private MyClientSocket myClientSocket;


    private static RunMode runMode;

    private static MainActivity mainActivity;

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

        viewState = PROMPT_VIEW;
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
        sendMsg2UIThread(MsgType.ENCODE_FILE_CHANGE.ordinal(),"");
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

    @OnClick(R.id.btn_openWifi)
    public void openClient() {
        MyThreadPool.execute(() -> {
            //wifiAPControl.openWifi();
            myClientSocket.connect(ConnectConstant.SERVER_IP, ConnectConstant.SERVER_PORT);
        });

    }

    @OnClick(R.id.btn_openAP)
    public void openAP() {
        wifiAPControl.openAP();
        myServerSocket.openServer(ConnectConstant.SERVER_PORT);

    }

    @OnClick(R.id.btn_test)
    public void test1() {
//        try {
//            for (int i = 0; i < 5; i++) {
//                byte[] bytes = MyByteBuffer.getBuffer(10 *1024*1024);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//
//        }
//        for (int i = 0; i < 100; i++) {
//            sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(), "hello, world!");
//
//        }
        //scrollView.fullScroll(ScrollView.FOCUS_DOWN);

        EncodeFile.getSingleton().recover();
    }

    @OnClick(R.id.btn_switch_view)
    public void switch_view() {
        if (viewState == PROMPT_VIEW) {
            scrollView.setVisibility(View.GONE);
            layout_wavePro.setVisibility(View.VISIBLE);
            viewState = WAVEPROGRESS_VIEW;

        } else if (viewState == WAVEPROGRESS_VIEW) {
            scrollView.setVisibility(View.VISIBLE);
            layout_wavePro.setVisibility(View.GONE);
            viewState = PROMPT_VIEW;
        }
    }


    //处理各个线程发来的消息
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            MsgType msgType = MsgType.values()[msg.what];
            switch (msgType) {
                case SHOW_MSG:
                    //Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    String prompt = msg.obj.toString() + "\n";
                    tv_prompt.append(prompt);
                    break;
                case ENCODE_FILE_CHANGE:
                    updateEncodeFileInfo();
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
            if (requestCode == REQUEST_CODE_FROM_ACTIVITY) {
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
                        sendMsg2UIThread(MsgType.ENCODE_FILE_CHANGE.ordinal(),"");
                    });
                })
                .show();
    }

    private void updateEncodeFileInfo() {
        EncodeFile encodeFile = EncodeFile.getSingleton();
        if(!encodeFile.isInitSuccess()){
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
        int progress = (int) ((float)currentPieceNum / totalPieceNum * 100);
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
    public void sendMsg2UIThread(int what, Object obj) {
        //sendMsg2UIThread(MsgType.SHOW_MSG.ordinal(),"hello, world!");
        if (handler != null) {
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }

    /**
     * 获取MainActivity实例
     *
     * @return
     */
    public static MainActivity getMainActivity() {
        return mainActivity;
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
