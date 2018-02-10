package com.jlkf.oiwifidemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView funRv;
    private WifiManager mWiFiManager;
    private TextView infoTv;
    private RecyclerView wifiRv;
    private List<ScanResult> wifiData;
    private WiFiAdapter wiFiAdapter;
    private static final int WIFICIPHER_NOPASS = 1;
    private static final int WIFICIPHER_WEP = 2;
    private static final int WIFICIPHER_WPA = 3;
    //网络的配置信息
    private WifiConfiguration config;
    private int type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //日志
        infoTv = (TextView) findViewById(R.id.info_tv);

        //WIFI初始化
        wifiInit();

        //功能RecycleView
        initFunRv();

        //WiFi列表RecycleView
        initWifiRv();

        //监听连接的是否系统发出的广播
        initBroadcastReceiver();
    }

    //WiFi列表RecycleView
    private void initWifiRv() {
        wifiRv = (RecyclerView) findViewById(R.id.wifi_rv);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        wifiRv.setLayoutManager(manager);
        wifiData = new ArrayList<>();
        wiFiAdapter = new WiFiAdapter(R.layout.adapter_wifi, wifiData);
        wifiRv.setAdapter(wiFiAdapter);
        wiFiAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {

                //
                mWiFiManager.disconnect();
                final ScanResult scanResult = wifiData.get(position);

                /**
                 * 三个安全性的排序为：WEP<WPA<WPA2。
                 * WEP是Wired Equivalent Privacy的简称，有线等效保密（WEP）协议是对在两台设备间无线传输的数据进行加密的方式，
                 * 用以防止非法用户窃听或侵入无线网络
                 * WPA全名为Wi-Fi Protected Access，有WPA和WPA2两个标准，是一种保护无线电脑网络（Wi-Fi）安全的系统，
                 * 它是应研究者在前一代的系统有线等效加密（WEP）中找到的几个严重的弱点而产生的
                 * WPA是用来替代WEP的。WPA继承了WEP的基本原理而又弥补了WEP的缺点：WPA加强了生成加密密钥的算法，
                 * 因此即便收集到分组信息并对其进行解析，也几乎无法计算出通用密钥；WPA中还增加了防止数据中途被篡改的功能和认证功能
                 * WPA2是WPA的增强型版本，与WPA相比，WPA2新增了支持AES的加密方式
                 **/
                String capabilities = scanResult.capabilities;
                //一般网络是这种类型
                type = WIFICIPHER_WPA;
                if (!TextUtils.isEmpty(capabilities)) {
                    if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                        type = WIFICIPHER_WPA;
                    } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                        type = WIFICIPHER_WEP;
                    } else {
                        type = WIFICIPHER_NOPASS; //无密码类型
                    }
                }
                //判断当前网络是否已经配置过, 测试小米Note配置过的网络也连接不上.必须输入密码才能连接上
                //建议自动连接保存本地来实现，系统的api基本连接不上
                config = isExsits(scanResult.SSID);
                //没有配置过
                if (config == null) {
                    if (type != WIFICIPHER_NOPASS) {//需要密码
                        showPsdWindow(scanResult.SSID, type);
                        return;
                    } else {
                        config = createWifiInfo(scanResult.SSID, "", type);
                        connect(config);
                    }
                } else {
                    connect(config);
                }

            }
        });
    }

    /**
     * 弹出密码的弹窗
     *
     * @param ssid 网络名称
     * @param type 类型
     */
    private void showPsdWindow(final String ssid, int type) {
        final EditText editText = new EditText(MainActivity.this);
        final int finalType = type;
        new AlertDialog.Builder(MainActivity.this).setTitle("请输入Wifi密码").setIcon(
                android.R.drawable.ic_dialog_info).setView(
                editText).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                config = createWifiInfo(ssid, editText.getText().toString(), finalType);
                connect(config);
            }
        }).setNegativeButton("取消", null).show();
    }


    /**
     * 连接wifi
     *
     * @param config
     */
    private void connect(final WifiConfiguration config) {
        infoTv.setText("连接中...");
        int wcgID = mWiFiManager.addNetwork(config);
        //连接wifi,第一个参数是需要连接wifi网络的networkId，第二个参数是指连接当前wifi网络是否需要断开其他网络
        boolean b = mWiFiManager.enableNetwork(wcgID, true);
        Log.e("oi", "wcgId:" + wcgID);
        Log.e("oi", "b:" + b);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //5s后判断当前是否已经连接上网络，没连接上的话就弹出密码框, 没连接上可能是获取到的配置连接不上
                //这个时候只能输入密码了，建议不要系统的配置了，自己本地保存密码还好点。
                if (!isWifi(MainActivity.this)) {
                    //弹窗里面有加“"”,所有这里去掉
                    showPsdWindow(config.SSID.replaceAll("\"", ""), type);
                }
            }
        }, 8000);
    }


    /**
     * 判断当前wifi是否有保存
     */
    private WifiConfiguration isExsits(String SSID) {
        //获取已记住密码的所有网络
        List<WifiConfiguration> existingConfigs = mWiFiManager.getConfiguredNetworks();
        //调用enableNetwork前把所有wifi都disenableNetwork（该方法能阻碍系统重新连接wifi）
        //enableNetwork会断开的当前连接的wifi，再连接指定wifi，而此时系统检测到wifi断开，
        // 也会自动连接wifi（默认为最后连接成功的那个wifi），因此可能导致调用enableNetwork却无法连接到指定wifi，
        // 或者连接成功后又被断开。所有在断开前禁止系统连接wifi
        for (WifiConfiguration c : existingConfigs) {
            mWiFiManager.disableNetwork(c.networkId);
        }
        //测试小米Note配置过的网络也连接不上.必须输入密码才能连接上
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * 初始化
     */
    private void wifiInit() {
        // 获取WiFi管理者对象
        mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //判断wifi模块是否可用
        if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            Toast.makeText(this, "WiFi可用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * UI点击事件
     */
    private void initFunRv() {
        funRv = (RecyclerView) findViewById(R.id.info_rv);
        GridLayoutManager manager = new GridLayoutManager(this, 4);
        funRv.setLayoutManager(manager);
        List<String> funData = new ArrayList<>();
        funData.add("开启WIFI");
        funData.add("关闭WIFI");
        funData.add("当前连接信息");
        funData.add("搜索WIFI列表");
        FunAdapter funAdapter = new FunAdapter(R.layout.adapter_info, funData);
        funRv.setAdapter(funAdapter);
        funAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                switch (position) {
                    //开启WIFI
                    case 0:
                        // 判断是否已经打开WiFi
                        if (!mWiFiManager.isWifiEnabled()) {
                            // 打开Wifi连接
                            mWiFiManager.setWifiEnabled(true);
                        }
                        break;
                    //关闭WIFI
                    case 1:
                        if (mWiFiManager.isWifiEnabled()) {
                            // 关闭Wifi连接
                            mWiFiManager.setWifiEnabled(false);
                        }
                        break;
                    //获取当前连接的WIFI的信息
                    case 2:
                        WifiInfo wifiInfo = mWiFiManager.getConnectionInfo();
                        StringBuffer info = new StringBuffer();
                        info.append("ssid: ").append(wifiInfo.getSSID()).append("\n")
                                .append("bssid: ").append(wifiInfo.getBSSID()).append("\n")
                                .append("mac address: ").append(wifiInfo.getMacAddress()).append("\n")
                                .append("speed: ").append(wifiInfo.getLinkSpeed()).append("\n")
                                .append("ip address: ").append(wifiInfo.getIpAddress()).append("\n")
                                .append("netwok id: ").append(wifiInfo.getNetworkId());
                        infoTv.setText(info.toString());
                        break;
                    //获取WIFI列表
                    case 3:
                        searchWiFi();
                        break;
                }
            }
        });
    }

    /**
     * 搜索WIFI列表
     */
    private void searchWiFi() {
        if (mWiFiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return;
        }
        // 开始扫描
        mWiFiManager.startScan();
        // mWiFiManager.getScanResults()获取搜索的WiFi内容
        // 返回结果是当前设备所在区域能搜索出来的WiFi列表
        List<ScanResult> results = mWiFiManager.getScanResults();
        wifiData.clear();
        wifiData.addAll(results);
        wiFiAdapter.notifyDataSetChanged();
    }


    /**
     * 配置当前需要连接的网络
     *
     * @param SSID     网络名称
     * @param password 密码
     * @param type     网络类型
     * @return 网络信息
     */
    public WifiConfiguration createWifiInfo(String SSID, String password,
                                            int type) {
        Log.w("AAA", "SSID = " + SSID + "password " + password + "type ="
                + type);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement
                    .set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            return null;
        }
        return config;
    }


    /**
     * enableNetwork 会发出广播， 注册广播监听网络变化
     */
    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //wifiManager.startScan(), 系统在扫描结束后，会发出WifiManager.SCAN_RESULTS_AVAILABLE_ACTION的广播
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.e("BBB", "SCAN_RESULTS_AVAILABLE_ACTION");
                // wifi已成功扫描到可用wifi。
                List<ScanResult> scanResults = mWiFiManager.getScanResults();
                wifiData.clear();
                wifiData.addAll(scanResults);
                wiFiAdapter.notifyDataSetChanged();
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                Log.e("BBB", "WifiManager.WIFI_STATE_CHANGED_ACTION");
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        //获取到wifi开启的广播时，开始扫描
                        mWiFiManager.startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        //wifi关闭发出的广播
                        break;
                }
                // WiFi在连接的过程中系统会发出WifiManager.NETWORK_STATE_CHANGED_ACTION的广播
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.e("BBB", "WifiManager.NETWORK_STATE_CHANGED_ACTION");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                //连接已断开
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    infoTv.setText("连接已断开");
                    //已成功连接
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    infoTv.setText("已连接到网络:" + wifiInfo.getSSID());
                    Log.w("AAA", "wifiInfo.getSSID():" + wifiInfo.getSSID());
                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == state.CONNECTING) {
                        infoTv.append("连接中...");
                    } else if (state == state.AUTHENTICATING) {
                        infoTv.append("正在验证身份信息...");
                    } else if (state == state.OBTAINING_IPADDR) {
                        infoTv.append("正在获取IP地址...");
                    } else if (state == state.FAILED) {
                        infoTv.append("连接失败");
                    }
                }

            }
        }
    };


    /**
     * 当前是否连接到WIFI
     */
    private static boolean isWifi(Context mContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }
}
