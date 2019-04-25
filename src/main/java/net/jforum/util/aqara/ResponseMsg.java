package net.jforum.util.aqara;

public class ResponseMsg<T> {

    /**
     * 状态码，0-成功，1-失败
     */
    private int code;
    /**
     * 请求id
     */
    private String requestId;
    /**
     * msg
     */
    private String message;
    /**
     * 返回业务数据
     */
    private T result;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
