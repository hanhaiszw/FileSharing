package com.example.zpc.file;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zpc.file.adapter.MyAdapter;
import com.example.zpc.file.common.IProxy;
import com.example.zpc.file.entity.FileInfo;
import com.example.zpc.file.util.Utils;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity implements IProxy, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    List<FileInfo> list;// 数据
    ListView lv;
    List<FileInfo> allList = new ArrayList<FileInfo>();
    ListView listView;
    LinearLayout layout;
    RelativeLayout relativeLayout1;
    String name = "";
    EditText text;
    ImageView img, img1;
    TextView tv_path;
    View view;
    String currPath; // 当前目录
    String parentPath; // 上级目录


    String rootPath;//SDCard根目录
    final String ROOT = Utils.getSDCardPath(); //SDCard根目录
    public static final int T_DIR = 0;// 文件夹
    public static final int T_FILE = 1;// 文件

    public static final int SORT_NAME = 0;//按名称排序
    public static final int SORT_DATE = 1;//按日期排序
    public static final int SORT_SIZE = 2;//按大小排序

    final String[] sorts = {"名称", "日期", "大小"};
    MyAdapter adapter; // 适配器

    int currSort = SORT_DATE;//当前排序
    Comparator<FileInfo> comparator = null;// 当前所使用的比较器


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        setContentView(R.layout.activity_main1);

        initView();// 初始化

        Intent intent = getIntent();
        String path = intent.getStringExtra("extra_path");
        rootPath = path;
        //updateData(rootPath);


        Utils.KEY = "";//初始化
        // 初始化控件
        lv = (ListView) findViewById(R.id.list);
        adapter = new MyAdapter(this);
        adapter.setList(list);
        adapter.setProxy(this);
        lv.setAdapter(adapter);


        sort = (TextView) findViewById(R.id.sort);
        count = (TextView) findViewById(R.id.count);
        size = (TextView) findViewById(R.id.size);
        iv_asc = (ImageView) findViewById(R.id.iv_asc);
        layout = (LinearLayout) findViewById(R.id.pathclick);
        img = (ImageView) findViewById(R.id.imgpath);
        relativeLayout1 = (RelativeLayout) findViewById(R.id.bottom);

        layout.setEnabled(false);
        //updateData();// 子线程--拿数据
        updateData(rootPath);

    }


    private void initView() {
        tv_path = (TextView) findViewById(R.id.path);
        listView = (ListView) findViewById(R.id.list);
        adapter = new MyAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

    }

    private void updateData(String path) {
        currPath = path;// 记录当前的目录
        File file = new File(path);
        parentPath = file.getParent();// 更新了上级目录
        adapter.selectMap.clear();
        list = Utils.getListData(path);// 数据
        list = Utils.getGroupList(list);//2次排序
        adapter.setList(list);
        adapter.notifyDataSetChanged();// 刷新视图
        tv_path.setText(submitPath(path));

        //hanhai添加   为了实现按时间逆序排序
        clickImg(iv_asc);
    }

    /**
     * 截取字符串
     *
     * @return
     */

    public String submitPath(String path) {
        //原代码
//        if (path.equals(ROOT)) {
//            return "/";
//        } else {
//            String PathCurrent = currPath.substring(ROOT.length());
//            return PathCurrent;
//        }

        //hanhai修改

        if (path.equals(ROOT)) {
            return "/";
        } else {
            String PathCurrent = currPath.substring(ROOT.length());
            return PathCurrent;
        }
    }

    //声明变量3
    SearchView sv;
    MenuItem search;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        search = menu.findItem(R.id.search);//容器
        sv = (SearchView) search.getActionView();//真正的搜索对象
        sv.setIconifiedByDefault(false);//图标显示在外侧
        sv.setSubmitButtonEnabled(true);//让提交按钮可用
        sv.setQueryHint("请输入文件名");//提示用户信息
        sv.setOnQueryTextListener(this);//关联提交事件
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.sort_name) {
            currSort = SORT_NAME;// 给排序状态赋值
        } else if (id == R.id.sort_date) {
            currSort = SORT_DATE;
        } else if (id == R.id.sort_size) {
            currSort = SORT_SIZE;
        } else if (id == R.id.c1) {
            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            //显示确认框
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("系统提示")
                    .setView(view)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            text = (EditText) view.findViewById(R.id.name1);
                            name = text.getText().toString();
                            File destDir = new File(currPath + "/" + name);
                            if (!destDir.exists()) {
                                destDir.mkdirs();
                            }
                            updateData(currPath);
                            adapter.notifyDataSetChanged();
                        }
                    }).setNegativeButton("取消", null)
                    .create().show();


        } else if (id == R.id.y1) {


            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = (ImageView) view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.txt);
            //显示确认框
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("系统提示")
                    .setView(view)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            text = (EditText) view.findViewById(R.id.name1);
                            name = text.getText().toString();
                            File destDir = new File(currPath + "/" + name + ".txt");
                            if (!destDir.exists()) {
                                try {
                                    destDir.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            updateData1(currPath);

                        }
                    }).setNegativeButton("取消", null)
                    .create().show();


        } else if (id == R.id.y2) {


            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = (ImageView) view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.xml);
            //显示确认框
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("系统提示")
                    .setView(view)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            text = (EditText) view.findViewById(R.id.name1);
                            name = text.getText().toString();
                            File destDir = new File(currPath + "/" + name + ".xml");
                            if (!destDir.exists()) {
                                try {
                                    destDir.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            updateData1(currPath);

                        }
                    }).setNegativeButton("取消", null)
                    .create().show();

        } else if (id == R.id.y3) {

            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = (ImageView) view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.doc);
            //显示确认框
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("系统提示")
                    .setView(view)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            text = (EditText) view.findViewById(R.id.name1);
                            name = text.getText().toString();
                            File destDir = new File(currPath + "/" + name + ".doc");
                            if (!destDir.exists()) {
                                try {
                                    destDir.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            updateData1(currPath);

                        }
                    }).setNegativeButton("取消", null)
                    .create().show();


        } else if (id == R.id.y4) {

            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = (ImageView) view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.xls);
            //显示确认框
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("系统提示")
                    .setView(view)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            text = (EditText) view.findViewById(R.id.name1);
                            name = text.getText().toString();
                            File destDir = new File(currPath + "/" + name + ".xls");
                            if (!destDir.exists()) {
                                try {
                                    destDir.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            updateData1(currPath);

                        }
                    }).setNegativeButton("取消", null)
                    .create().show();


        } /*else if (id == R.id.d1) {
            File file = new File(currPath);
            DeleteFile(file);
            mHandler.sendEmptyMessage(1);
            tv_path.setText(parentPath);
            updateData();
        }
*/

        update_sort();// 调用统一的排序方法

        asc *= -1;//负数,正数

        return super.onOptionsItemSelected(item);
    }

/**
 Handler mHandler = new Handler() {
 public void handleMessage(Message msg) {
 switch (msg.what) {
 case 0:
 Toast.makeText(getApplicationContext(), "文件或文件夹不存在", Toast.LENGTH_LONG).show();
 break;
 case 1:
 Toast.makeText(getApplicationContext(), "删除成功！", Toast.LENGTH_LONG).show();
 break;
 default:
 break;
 }
 }

 ;
 };
 */
    /**
     * 递归删除文件和文件夹
     *
     * @param file 要删除的根目录
     */
    /**
     * public void DeleteFile(File file) {
     * if (file.exists() == false) {
     * mHandler.sendEmptyMessage(0);
     * return;
     * } else {
     * if (file.isFile()) {
     * file.delete();
     * return;
     * }
     * if (file.isDirectory()) {
     * File[] childFile = file.listFiles();
     * if (childFile == null || childFile.length == 0) {
     * file.delete();
     * return;
     * }
     * for (File f : childFile) {
     * DeleteFile(f);
     * }
     * file.delete();
     * }
     * }
     * }
     */
    private void update_sort() {
        if (currSort == SORT_NAME) {
            comparator = nameComparator;// 选择不同的比较器
        }
        if (currSort == SORT_DATE) {
            comparator = dateComparator;
        }
        if (currSort == SORT_SIZE) {
            comparator = sizeComparator;
        }
        Collections.sort(list, comparator);// 这里才是排序的操作
        list = Utils.getGroupList(list);//2次排序
        adapter.setList(list);
        adapter.notifyDataSetChanged();// 刷新视图
        update_infobar();
    }


    //hanhai 1 为正序  -1为逆序
    //hanhai 修改 1 为 -1  为了实现按时间逆序排序
    int asc = -1; // 可以帮助在正序和倒序之间进行切换
    // 日期比较器
    Comparator<FileInfo> dateComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {


            if (rhs.lastModify > lhs.lastModify) {
                return -1 * asc;
            } else if (rhs.lastModify == lhs.lastModify) {
                return 0;
            } else {
                return 1 * asc;
            }
        }
    };

    // 大小比较器
    Comparator<FileInfo> sizeComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            if (rhs.bytesize > lhs.bytesize) {
                return -1 * asc;
            } else if (rhs.bytesize == lhs.bytesize) {
                return 0;
            } else {
                return 1 * asc;
            }
        }
    };

    // 应用名比较器
    Comparator<FileInfo> nameComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            // 为了适应汉字的比较
            Collator c = Collator.getInstance(Locale.CHINA);
            return (asc == 1) ? c.compare(lhs.name, rhs.name)
                    : c.compare(rhs.name, lhs.name);
        }
    };


    // 1声明进度框对象
    ProgressDialog pd;

    // 显示一个环形进度框
    public void showProgressDialog() {
        // 实例化
        pd = new ProgressDialog(this);
        // "旋转"风格
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setTitle("系统信息");
        pd.setMessage("正在加载文件列表,请耐心等待...");
        pd.show();// 显示
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {// 内部类
        @Override
        public void handleMessage(Message msg) {
            // 重写方法
            if (msg.what == 1) {// UI 线程的回调处理
                pd.dismiss();
                // 更新列表
                adapter.notifyDataSetChanged();

                Toast.makeText(MainActivity.this
                        , "文件数:" + list.size(), Toast.LENGTH_LONG).show();
                update_sort();
            }
        }
    };


    private void updateData1(final String path) {

        // (1)--启动新线程,处理耗时操作
        new Thread() {
            public void run() {
                // 获得数据(所有的应用)
                list = Utils.getListData(path);
                allList.clear();// 清空
                allList.addAll(list);// 复制集合
                list = Utils.getGroupList(list);//2次排序
                adapter.setList(list);
                // 给主线程发消息

                Message msg = handler.obtainMessage();
                msg.what = 1;
                handler.sendMessage(msg);// msg.what=1
            }


        }.start();
        // (2) --
        showProgressDialog();// 显示进度框
    }


    //3.子线程
    private void updateData() {

        // (1)--启动新线程,处理耗时操作
        new Thread() {
            public void run() {
                // 获得数据(所有的应用)
                list = Utils.getListData(Utils.getSDCardPath());
                allList.clear();// 清空
                allList.addAll(list);// 复制集合
                list = Utils.getGroupList(list);//2次排序
                adapter.setList(list);
                // 给主线程发消息

                Message msg = handler.obtainMessage();
                msg.what = 1;
                handler.sendMessage(msg);// msg.what=1
            }

            ;
        }.start();
        // (2) --
        showProgressDialog();// 显示进度框
    }

    ImageView iv_asc;
    TextView sort, count, size;

    // 更新顶部信息栏中内容
    private void update_infobar() {


        if (asc == 1) {
            iv_asc.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            iv_asc.setImageResource(android.R.drawable.arrow_down_float);
        }


        sort.setText("排序: " + sorts[currSort]);

        //sort.setOnClickListener(this);
        count.setText("文件数: " + list.size());
        size.setText("大小: " + getListSize());
    }

    /**
     * 遍历数据集合,累加全部的Size
     *
     * @return
     */
    private String getListSize() {
        long sum = 0;// 总和
        for (FileInfo app : list) {// foreach
            sum += app.bytesize;
        }
        return Utils.getSize(sum);
    }

    //long time = System.currentTimeMillis();

    @Override
    public void onBackPressed() {
        // 点击"回退"键

        // 计算和上次点击的时间差
//        long delta = System.currentTimeMillis() - time;
//        if (delta < 1000) {// 小于1秒
//            // 用户真的要退出
//            finish();
//        } else {// 不让用户退出
//            Toast.makeText(this, "再点击一次退出", Toast.LENGTH_SHORT).show();
//            time = System.currentTimeMillis();// 让time更新为当前时间
//        }

        // 返回 --> 打开上级
        if (currPath.equals(rootPath)) {
            // 退出流程
            getExit();
        } else {
            //打开上目录
            updateData(parentPath);
        }
    }

    public boolean onQueryTextSubmit(String query) {
        //提交关键字
        // Utils.toast(this, "您查询的关键字是 ：" + query.trim());
        Utils.KEY = query.trim();
        list = Utils.getSearchResult(allList, query);//根据关键字生成结果
        update_sort();//重新排序事件

        return true;//消化事件
    }

    public void clickImg(View v) {

        update_sort();
//切换正负倒序
      //  asc *= -1;


    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Utils.KEY = newText.trim();
        list = Utils.getSearchResult(allList, newText.trim());//根就关键字生成结果
        update_sort();//重新排序更新

        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo item = (FileInfo) parent.getItemAtPosition(position);
        // 判断文件/文件夹
        if (item.type == T_DIR) {
            // 进入
            updateData(item.path);


        } else {
            // 文件: 打开
            Utils.openFile(this, new File(item.path));
        }

    }


    private void getExit() {
        finish();
//        new AlertDialog.Builder(this)
//                .setIcon(android.R.drawable.stat_sys_warning)
//                .setMessage("确定退出吗?")
//                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                })
//                .setNegativeButton("取消", null)
//                .show();
    }


    @Override
    public void itemClick(int position) {

        //点击多选的实现
        if (adapter.selectMap.containsKey(position)) {
//删除key
            adapter.selectMap.remove(position);
            if (adapter.selectMap.size() == 0) {

                relativeLayout1.setVisibility(View.GONE);
            } else {

                relativeLayout1.setVisibility(View.VISIBLE);
            }
        } else {

            relativeLayout1.setVisibility(View.VISIBLE);
            adapter.selectMap.put(position, position);
        }
        adapter.notifyDataSetChanged();
    }


    //复制文件结构
    HashMap<Integer, String> copyMap = new HashMap<Integer, String>();

    public void cope(View view) {

        if (adapter.selectMap.size() == 0) {
            Toast.makeText(this, "您还没选中任何项目！", Toast.LENGTH_SHORT).show();
        } else {

//把用户信息保存到一个合理的数据结构中
            copyMap.clear();
            for (Integer position : adapter.selectMap.keySet()) {

                copyMap.put(position, list.get(position).path);
            }
            Toast.makeText(this, copyMap.size() + "个项目已保存", Toast.LENGTH_SHORT).show();
            //切换粘贴为激活状态
//切换粘贴为激活状态
            layout.setEnabled(true);

            img.setImageResource(R.drawable.ic_menu_paste_holo_light);

        }

    }


    public void delete(View view) {

        if (adapter.selectMap.size() == 0) {
            Toast.makeText(this, "您还没选中任何项目！", Toast.LENGTH_SHORT).show();

        } else {


            //显示确认框
            new AlertDialog.Builder(this)
                    .setTitle("系统提示")
                    .setMessage("您是否要删除这" + adapter.selectMap.size() + "个项目")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //遍历
                            for (Integer position : adapter.selectMap.keySet()) {
                                String path = list.get(position).path;
                                File file = new File(path);
                                //根据path路径删除文件
                                if (file.isFile()) {
                                    Utils.deleteFile(path);
                                }
                                if (file.isDirectory()) {
                                    Utils.deleteDir(path);
                                }

                            }
                            //更新
                            adapter.selectMap.clear();
                            updateData(currPath);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .create().show();
        }
    }


    public void path(View view) {

        if (copyMap.size() > 0) {


            for (String path : copyMap.values()) {
                File file = new File(path);

                if (file.isFile()) {

                    int res = Utils.pasteFile(currPath, new File(path));
                    if (res == 1) {
                        Toast.makeText(this, "该文件已存在", Toast.LENGTH_SHORT).show();

                    } else {

                        Toast.makeText(this, path + "文件复制成功", Toast.LENGTH_SHORT).show();
                    }

                }

                if (file.isDirectory()) {

                    Utils.pasteDir(currPath, new File(path));
                }
            }

            copyMap.clear();

            updateData(currPath);
            layout.setEnabled(false);
            img.setImageResource(R.drawable.ic_menu_paste_holo_dark);


        } else {

            Toast.makeText(this, "没有可粘帖的项目", Toast.LENGTH_SHORT).show();
        }

    }

    //全选操作
    public void selectAll(View v) {
        adapter.selectMap.clear();
        for (int i = 0; i < list.size(); i++) {
            adapter.selectMap.put(i, i);
        }
        adapter.notifyDataSetChanged();
    }

    //全不选操作
    public void selectNone(View v) {
        adapter.selectMap.clear();

        adapter.notifyDataSetChanged();
    }


    //剪切

    public void pathDelete(View v) {
        if (copyMap.size() > 0) {


            for (String path : copyMap.values()) {
                File file = new File(path);

                if (file.isFile()) {

                    int res = Utils.pasteFile(currPath, new File(path));
                    if (res == 1) {
                        Toast.makeText(this, "该文件已存在", Toast.LENGTH_SHORT).show();

                    } else {

                        Toast.makeText(this, path + "文件剪切成功", Toast.LENGTH_SHORT).show();
                    }

                }

                if (file.isDirectory()) {

                    Utils.pasteDir(currPath, new File(path));
                }
            }

            deletePath(view);
            copyMap.clear();

            updateData(currPath);
            layout.setEnabled(false);
            img.setImageResource(R.drawable.ic_menu_paste_holo_dark);


        } else {

            Toast.makeText(this, "没有可粘帖的项目", Toast.LENGTH_SHORT).show();
        }

    }

    public void deletePath(View view) {


//遍历
                            for (String path : copyMap.values()) {
                                File file = new File(path);

                                if (file.isFile()) {
                                    Utils.deleteFile(path);
                                }
                                if (file.isDirectory()) {
                                    Utils.deleteDir(path);
                                }
                            }
                            //更新
                            adapter.selectMap.clear();
                            updateData(currPath);


    }

}
