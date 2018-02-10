package com.jlkf.oiwifidemo;

import android.net.wifi.ScanResult;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

/**
 * Created by oi on 2018/2/8 email:630869889@qq.com
 */
class WiFiAdapter extends BaseQuickAdapter<ScanResult, BaseViewHolder> {

    public WiFiAdapter(@LayoutRes int layoutResId, @Nullable List<ScanResult> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ScanResult item) {
        helper.setText(R.id.name, item.SSID);
        helper.setText(R.id.address, item.BSSID);
        int level = item.level;
        String levelInfo;
        if (level <= 0 && level >= -50) {
            levelInfo = "信号很好";
        } else if (level < -50 && level >= -70) {
            levelInfo = "信号较好";
        } else if (level < -70 && level >= -80) {
            levelInfo = "信号一般";
        } else if (level < -80 && level >= -100) {
            levelInfo = "信号较差";
        } else {
            levelInfo = "信号很差";
        }
        helper.setText(R.id.level, levelInfo);
        helper.addOnClickListener(R.id.connBtn);
    }
}
