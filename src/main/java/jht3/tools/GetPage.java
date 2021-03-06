package jht3.tools;

import jht3.Setting;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class GetPage {
    private static final TrustAllManager trustAllManager = new TrustAllManager();

    private static final OkHttpClient okHttpClient =new OkHttpClient.Builder()
            .connectTimeout(Setting.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(Setting.readTimeout,TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(7,7,TimeUnit.SECONDS))
            .retryOnConnectionFailure(true)
            .sslSocketFactory(createTrustAllSSLFactory(), trustAllManager)
            .retryOnConnectionFailure(true)
            .build();

    @FunctionalInterface
    public interface MyReqyest {
        void setHeader(Request.Builder x);
    }
    protected static Object getPage(String url,Boolean isString){
        return GetPage.getPage(url,isString,null);
    }
    protected static Object getPage(String url,Boolean isString,MyReqyest setHeder){
        Request.Builder builder = new Request.Builder().url(url).get()
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .addHeader("Cookie", Setting.cookie)
                .addHeader("Referer", "https://www.pixiv.net")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.26 Safari/537.36 Edg/85.0.564.13");
        if(setHeder!=null)
            setHeder.setHeader(builder);
        Request request = builder.build();
        Call call = okHttpClient.newCall(request);
        Response res = null;
        Object body;
        try {
            res=call.execute();
            if(isString) {
                body = res.body().string();
            }
            else {
                body = res.body().bytes();
            }
        } catch (IOException e) {
            System.out.println("重新尝试获取 "+url);
            body = GetPage.getPage(url,isString,setHeder);
        }finally{
            if(res==null||!res.isSuccessful()){
                if(res!=null)
                    System.out.println("请求失败,code: "+res.code()+" ,重新尝试中");
                try {
                    Thread.sleep(Setting.retrySleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                body=GetPage.getPage(url,isString,setHeder);
            }
        }
        return body;
    }

    protected static Response getHead(String url){
        Request request = new Request.Builder().url(url).get()
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                //.addHeader("Cookie", Setting.cookie)
                .addHeader("Referer", "https://www.pixiv.net")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.26 Safari/537.36 Edg/85.0.564.13")
                .build();
        Call call = okHttpClient.newCall(request);
        Response res;
        try {
            res = call.execute();
            res.close();
        } catch (IOException e) {
            res=GetPage.getHead(url);
        }
        return res;
    }
    private static SSLSocketFactory createTrustAllSSLFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{GetPage.trustAllManager}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssfFactory;
    }

    static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType){ }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType){ }
        @Override public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
