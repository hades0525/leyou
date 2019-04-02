package com.leyou.order.config;

import com.github.wxpay.sdk.WXPayConfig;
import lombok.Data;

import java.io.InputStream;



@Data
public class PayConfig implements WXPayConfig {

    /**
     * 公众账号ID
     */
    private String appID;

    /**
     * 商户号
     */
    private String mchID;

    /**
     * 生成签名的密钥
     */
    private String key;

    /**
     * 连接超时时间
     */
    private int httpConnectTimeoutMs;

    /**
     * 读取超时时间
     */
    private int httpReadTimeoutMs;

    /**
     * 读取超时时间
     */
    private String notifyUrl;


    @Override
    public InputStream getCertStream() {
        return null;
    }

}
