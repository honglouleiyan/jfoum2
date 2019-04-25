package net.jforum.util.aqara;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.*;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PooledHttpClientUtils {

    private static final Logger LOGGER = Logger.getLogger(PooledHttpClientUtils.class);

    private static final String CONTENT_JSON = "application/json; charset=utf-8";
    private static final String CONTENT_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=utf-8";

    private static final int DEFAULT_POOL_MAX_TOTAL = 200;
    private static final int DEFAULT_POOL_MAX_PER_ROUTE = 200;

    private static final int DEFAULT_CONNECT_TIMEOUT = 1500;
    private static final int DEFAULT_CONNECT_REQUEST_TIMEOUT = 1500;
    private static final int DEFAULT_SOCKET_TIMEOUT = 5000;

    private static volatile CloseableHttpClient httpClient = null;
    private static IdleConnectionMonitorThread idleThread = null;


    private static CloseableHttpClient createHttpClient() {
        CloseableHttpClient client = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().build();
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType) {
                }
            }}, null);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
//            //ALLOW_ALL_HOSTNAME_VERIFIER:这个主机名验证器基本上是关闭主机名验证的,实现的是一个空操作，并且不会抛出javax.net.ssl.SSLException异常。
            SSLConnectionSocketFactory sslsfl= new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());


            ConnectionSocketFactory socketFactory = PlainConnectionSocketFactory.getSocketFactory();
            Registry<ConnectionSocketFactory> registry = RegistryBuilder
                    .<ConnectionSocketFactory>create()
                    .register("http", socketFactory)
                    .register("https", new SSLConnectionSocketFactory(sslContext))
                    .build();

            //创建连接管理器
            PoolingHttpClientConnectionManager pcm = new PoolingHttpClientConnectionManager(registry);
            // Create socket configuration
            SocketConfig socketConfig = SocketConfig.custom()
                    .setTcpNoDelay(true).build();
            pcm.setDefaultSocketConfig(socketConfig);
            // Create message constraints
            MessageConstraints messageConstraints = MessageConstraints.custom()
                    .setMaxHeaderCount(200).setMaxLineLength(2000).build();
            // Create connection configuration
            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setMalformedInputAction(CodingErrorAction.IGNORE)
                    .setUnmappableInputAction(CodingErrorAction.IGNORE)
                    .setCharset(Consts.UTF_8)
                    .setMessageConstraints(messageConstraints).build();
            pcm.setDefaultConnectionConfig(connectionConfig);
            pcm.setMaxTotal(DEFAULT_POOL_MAX_TOTAL);
            pcm.setDefaultMaxPerRoute(DEFAULT_POOL_MAX_PER_ROUTE);

            //重试处理
            HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
                @Override
                public boolean retryRequest(IOException e, int count, HttpContext httpContext) {
                    if (count >= 3) {
                        return false;
                    }
                    if (e instanceof NoHttpResponseException){
                        //连接丢失，尝试重连
                        return true;
                    }
                    if (e instanceof InterruptedIOException){
                        return false;
                    }
                    if (e instanceof UnknownHostException){
                        return false;
                    }

                    HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
                    HttpRequest httpRequest = clientContext.getRequest();
                    //请求幂等，重试
                    if (!(httpRequest instanceof HttpEntityEnclosingRequest)){
                        return true;
                    }
                    return false;
                }
            };


            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(DEFAULT_CONNECT_REQUEST_TIMEOUT)
                    .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                    .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
                    .build();

            client = HttpClients.custom()
                    .setConnectionManager(pcm)
                    .setRetryHandler(httpRequestRetryHandler)
                    .setDefaultRequestConfig(requestConfig)
                    .setSSLSocketFactory(sslsfl)
                    .build();

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SslContext builder init. NoSuchAlgorithmException", e);
        } catch (KeyManagementException e) {
            LOGGER.error("SslContext builder init. KeyManagementException.", e);
        }
        return client;
    }

    /**
     * 获取httpclient对象
     *
     * @return httpclient对象
     */
    public static CloseableHttpClient getHttpClient() {

        if (httpClient == null) {
            // 双重校验
            synchronized (PooledHttpClientUtils.class) {
                if (httpClient == null) {
                    httpClient = createHttpClient();
                }
            }
        }

        return httpClient;
    }

    public static String doGet(String url) {
        return doGet(url, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    }

    public static String doGet(String url, Map<String, Object> params) {
        return doGet(url, Collections.EMPTY_MAP, params);
    }

    public static String doGet(String url, Map<String, String> headers, Map<String, Object> params) {

        httpClient = getHttpClient();
        /**
         * 构建GET请求头
         */
        String apiUrl = getUrlWithParams(url, params);
        HttpGet httpGet = new HttpGet(apiUrl);

        /**
         * 设置header信息
         */
        if ( headers != null && headers.size() > 0 ) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue());
            }
        }

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);

            return getResponseResult(response);
        } catch (IOException e) {
            LOGGER.error("httpClient execute get exception",e);
        } finally {
            if ( response != null ) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("response close exception",e);
                }
            }
        }
        return null;
    }

    /**
     *
     * @param response
     * @return
     */
    private static String getResponseResult(CloseableHttpResponse response){
        if (response == null || response.getStatusLine() == null) {
            return null;
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if ( statusCode == HttpStatus.SC_OK ) {
            HttpEntity entityRes = response.getEntity();
            if (entityRes != null) {
                try {
                    return EntityUtils.toString(entityRes, "UTF-8");
                } catch (IOException e) {
                    LOGGER.error("entityUtils toString parse exception",e);
                }
            }
        }
        return null;
    }

    public static String doPost(String apiUrl, Map<String, Object> params) {
        return doPost(apiUrl, Collections.EMPTY_MAP, params);
    }

    public static String doPost(String apiUrl,Map<String, String> headers,Map<String, Object> params) {
        return doPost(apiUrl, headers, params, true);
    }

    public static String doPost(String apiUrl,Map<String, String> headers,Map<String, Object> params, boolean paramsFormatStringEntity) {

        httpClient = getHttpClient();

        HttpPost httpPost = new HttpPost(apiUrl);

        if ( headers != null && headers.size() > 0 ) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }
        }

        HttpEntity entityReq;
        if (paramsFormatStringEntity) {
            httpPost.addHeader(HTTP.CONTENT_TYPE, CONTENT_JSON);
            entityReq = getUrlEncodedFormStringEntity(params);
        } else {
            httpPost.addHeader(HTTP.CONTENT_TYPE, CONTENT_X_WWW_FORM_URLENCODED);
            entityReq = getUrlEncodedFormNameValuePairEntity(params);
        }
        httpPost.setEntity(entityReq);

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);

            return getResponseResult(response);
        } catch (IOException e) {
            LOGGER.error("httpClient execute post exception",e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("response close exception",e);
                }
            }
        }
        return null;
    }

    private static HttpEntity getUrlEncodedFormStringEntity(Map<String, Object> params) {
        if (params == null || params.size() == 0 ) {
            return null;
        }
        JSONObject pairList = new JSONObject();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if(null != entry.getValue()){
                pairList.put(entry.getKey(), entry.getValue());
            }
        }
        StringEntity entity = new StringEntity(pairList.toString(), Consts.UTF_8);
        entity.setContentType("application/json");
        return entity;
    }

    private static HttpEntity getUrlEncodedFormNameValuePairEntity(Map<String, Object> params) {
        if ( params == null || params.size() == 0 ) {
            return null;
        }
        List<NameValuePair> nameValuePairList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String value;
            Object obj = entry.getValue();
            if (!(obj instanceof String)) {
                value = JSONObject.toJSONString(obj);
            } else {
                value = (String) obj;
            }
            nameValuePairList.add(new BasicNameValuePair(entry.getKey(), value));
        }

        return new UrlEncodedFormEntity(nameValuePairList, Consts.UTF_8);
    }

    private static String getUrlWithParams(String url, Map<String, Object> params) {
        boolean first = true;
        StringBuilder sb = new StringBuilder(url);
        for (String key : params.keySet()) {
            char ch = '&';
            if (first == true) {
                ch = '?';
                first = false;
            }
            String value = params.get(key).toString();
            try {
                String sval = URLEncoder.encode(value, "UTF-8");
                sb.append(ch).append(key).append("=").append(sval);
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("unsupportedEncodingException exception",e);
            }
        }
        return sb.toString();
    }


    public static void shutdown() {
        if(idleThread != null){
            idleThread.shutdown();
        }
    }


    /**
     * 清理线程, 定期主动处理过期/空闲连接
     * 当流量为0时, 你会发现存在处于ClOSE_WAIT的连接. 由于httpclient清理过期/被动关闭的socket,是采用懒惰清理的策略.
     * 它是在连接从连接池取出使用的时候, 检测状态并做相应处理. 如果没有流量, 那这些socket将一直处于CLOSE_WAIT(半连接的状态), 系统资源被浪费
     */
    private static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private volatile boolean exitFlag = false;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            this.connMgr = connMgr;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!this.exitFlag) {
                synchronized (this) {
                    try {
                        this.wait(2000);
                    } catch (InterruptedException e) {
                        LOGGER.error("thread interruptedException",e);
                    }
                }
                /**
                 * 关闭失效的连接
                 */
                connMgr.closeExpiredConnections();

                /**
                 * 可选的, 关闭30秒内不活动的连接
                 */
                connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
            }
        }

        public void shutdown() {
            this.exitFlag = true;
            synchronized (this) {
                notify();
            }
        }
    }

    public static void main(String [] args){
        String wholePath = "http://110.43.34.124/app/v1.0/lumi/user/checkPassword";
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "5326403428625859.539775163185496065");
        params.put("spassword", "578e01ad57fa89d9ccc5e5cfc5e31753");
        headers.put("Appid", "7be1984f0556276133336839");
        headers.put("Content-Type", "application/json");
        // System.out.println(PooledHttpClientUtils.doPost(wholePath, headers, params));
        String result = PooledHttpClientUtils.doPost(wholePath, headers, params);
        System.out.println("result:"+result);

        //    JSONObject.parseObject(JSON.parseObject(result).getString("result"), IotUserHomeServer.class);
        //System.out.println(PooledHttpClientUtils.doGet("http://www.baidu.com"));
        // System.out.println(PooledHttpClientUtils.doGet("http://www.baidu.com"));
    }
}