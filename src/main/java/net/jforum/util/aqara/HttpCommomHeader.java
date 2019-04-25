package net.jforum.util.aqara;



import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HttpCommomHeader implements Serializable{
    private static final long serialVersionUID = -8419968866797569060L;

    public static final String REQUEST_HEADER_APPID = "Appid";

    public static final String REQUEST_HEADER_SIGN = "Sign";

    public static final String REQUEST_HEADER_TOKEN = "Token";

    public static final String REQUEST_HEADER_USERID = "Userid";

    public static final String REQUEST_HEADER_CLIENTID = "Clientid";

    public static final String REQUEST_HEADER_TIME = "Time";

    public static final String REQUEST_HEADER_DEVICETOKEN = "Device-Token";

    public static final String REQUEST_HEADER_LANG = "Lang";

    public static final String REQUEST_HEADER_APPVERSION = "App-Version";

    /**
     * 0-ios/1-andriod
     */
    public static final String REQUEST_HEADER_SYSTYPE = "Sys-Type";

    public static final String REQUEST_HEADER_SYSVERSION= "Sys-Version";

    public static final String REQUEST_HEADER_PHONEMODEl= "Phone-Model";


    /**
     * 请求参数map
     */
    private Map<String, String> paramsMap = new HashMap();

    public void initMap(){
        paramsMap.put(REQUEST_HEADER_APPID,this.appid);
        paramsMap.put(REQUEST_HEADER_SIGN,this.sign);
        paramsMap.put(REQUEST_HEADER_TOKEN,this.token);
        paramsMap.put(REQUEST_HEADER_USERID,this.userid);
        paramsMap.put(REQUEST_HEADER_CLIENTID,this.clientId);
        paramsMap.put(REQUEST_HEADER_TIME,this.time);
        paramsMap.put(REQUEST_HEADER_DEVICETOKEN,this.deviceToken);
        paramsMap.put(REQUEST_HEADER_LANG,this.lang);
        paramsMap.put(REQUEST_HEADER_APPVERSION,this.appVersion);
        paramsMap.put(REQUEST_HEADER_SYSTYPE,this.sysType);
        paramsMap.put(REQUEST_HEADER_PHONEMODEl,this.phoneModel);
    }

    private String appid;

    private String sign;

    private String token;

    private String userid;

    private String clientId;

    private String time;

    private String deviceToken;

    private String appVersion;

    private String sysType;

    private String sysVersion;

    private String phoneModel;

    private String lang;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getSysType() {
        return sysType;
    }

    public void setSysType(String sysType) {
        this.sysType = sysType;
    }

    public String getSysVersion() {
        return sysVersion;
    }

    public void setSysVersion(String sysVersion) {
        this.sysVersion = sysVersion;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Map<String, String> getParamsMap() {
        return paramsMap;
    }

    public void setParamsMap(Map<String, String> paramsMap) {
        this.paramsMap = paramsMap;
    }
}
