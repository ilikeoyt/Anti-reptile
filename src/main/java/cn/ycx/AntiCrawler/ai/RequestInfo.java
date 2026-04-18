package cn.ycx.AntiCrawler.ai;

import java.util.Date;
import java.util.Map;

/**
 * 请求信息类，用于存储与反爬相关的请求信息
 */
public class RequestInfo {
    
    private String requestId;
    private Date requestTime;
    private String requestMethod;
    private String requestUrl;
    private String requestUri;
    private String queryString;
    private String remoteAddr;
    private String userAgent;
    private String referer;
    private String cookies;
    private Map<String, String> headers;
    private Map<String, String[]> parameters;
    private String requestBody;
    private String interceptReason;
    private String interceptRule;
    private String contentType;
    private int contentLength;
    private String protocol;
    private int responseStatus;
    private long responseTime;
    private String aiAnalysisResult;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String[]> parameters) {
        this.parameters = parameters;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getInterceptReason() {
        return interceptReason;
    }

    public void setInterceptReason(String interceptReason) {
        this.interceptReason = interceptReason;
    }

    public String getInterceptRule() {
        return interceptRule;
    }

    public void setInterceptRule(String interceptRule) {
        this.interceptRule = interceptRule;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public String getAiAnalysisResult() {
        return aiAnalysisResult;
    }

    public void setAiAnalysisResult(String aiAnalysisResult) {
        this.aiAnalysisResult = aiAnalysisResult;
    }
}
