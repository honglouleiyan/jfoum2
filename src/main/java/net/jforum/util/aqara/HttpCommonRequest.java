package net.jforum.util.aqara;

import net.jforum.sso.DefaultLoginAuthenticator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lvyl
 * @date 2019/1/3 15:16
 * @description
 */
public class HttpCommonRequest {

    private static final Logger LOGGER = Logger.getLogger(HttpCommonRequest.class);
//
//    private static String opencloudAppId = "d0ec0978a70fe6f609c130ab";
//
//    private  static String opencloudAppKey = "7Cn6nO3xE5YkPLFuGPGZvnvUrbwVbKob";

    private static String opencloudAppId = "4f050659665803280ba5162b";

    private  static String opencloudAppKey = "xjlxe2u0OkHTLTM7AEYrMa0YbcToOXOU";

    /**
     * 获取http请求的公共头部信息
     * @return
     */
    public static Map<String,String> getHeaderMap(HttpCommomHeader httpCommonHeader){
        String userId = "";
        String token = "";
        httpCommonHeader.setAppid(opencloudAppId);
        if(StringUtils.isNotEmpty(httpCommonHeader.getUserid()) || StringUtils.isEmpty(userId)){
            httpCommonHeader.setUserid(httpCommonHeader.getUserid());
        }else {
            httpCommonHeader.setUserid(userId);
        }
        if(StringUtils.isNotEmpty(httpCommonHeader.getToken()) || StringUtils.isEmpty(token)){
            httpCommonHeader.setToken(httpCommonHeader.getToken());
        }else {
            httpCommonHeader.setToken(token);
        }

        httpCommonHeader.setLang("zh");
        httpCommonHeader.setTime(String.valueOf(System.currentTimeMillis()));
        httpCommonHeader.setSign(getSign(httpCommonHeader));
        httpCommonHeader.initMap();
        Map<String,String> headerMap = httpCommonHeader.getParamsMap();
        return headerMap;
    }

    private static String getSign(HttpCommomHeader httpCommonHeader){
        Map<String,String> signMap = new HashMap<>();
        if(StringUtils.isNotEmpty(httpCommonHeader.getAppid())){
            signMap.put(HttpCommomHeader.REQUEST_HEADER_APPID, httpCommonHeader.getAppid());
        }
        if(StringUtils.isNotEmpty(httpCommonHeader.getToken())){
            signMap.put(HttpCommomHeader.REQUEST_HEADER_TOKEN, httpCommonHeader.getToken());
        }
        if(StringUtils.isNotEmpty(httpCommonHeader.getUserid())){
            signMap.put(HttpCommomHeader.REQUEST_HEADER_USERID, httpCommonHeader.getUserid());
        }
        if(StringUtils.isNotEmpty(httpCommonHeader.getLang())){
            signMap.put(HttpCommomHeader.REQUEST_HEADER_LANG, httpCommonHeader.getLang());
        }
        if(StringUtils.isNotEmpty(httpCommonHeader.getTime())){
            signMap.put(HttpCommomHeader.REQUEST_HEADER_TIME, httpCommonHeader.getTime());
        }
        return createSign(signMap,opencloudAppKey,false);
    }

    /**
     * 生成签名
     * @param parames
     * @return
     */
    public static String createSign(Map<String, String> parames, String appKey, Boolean encode){
        //sort
        Object[] keys = parames.keySet().toArray();
        Arrays.sort(keys);
        StringBuffer stringBuffer = new StringBuffer();
        boolean first = true;
        for (Object key : keys) {
            if (first) {
                first = false;
            } else {
                stringBuffer.append("&");
            }
            stringBuffer.append(key).append("=");
            String paramValue = parames.get(key);
            String valueString = "";
            if (null != paramValue) {
                valueString = String.valueOf(paramValue);
            }
            if (encode) {
                try {
                    stringBuffer.append(URLEncoder.encode(valueString, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("URLEncoder encode error", e);
                }
            } else {
                stringBuffer.append(valueString);
            }
        }
        //append appKey
        String signStr = stringBuffer.append("&").toString().toLowerCase()+appKey;
        try {
            return Md5Util.MD5_32(signStr);
        } catch (Exception e) {
            LOGGER.error("Md5Util MD5 error", e);
        }
        return null;
    }
}
