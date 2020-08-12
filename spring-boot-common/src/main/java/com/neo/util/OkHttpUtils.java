package com.neo.util;

import com.alibaba.fastjson.JSON;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class OkHttpUtils {

    /**
     * 默认超时时间：10S
     */
    public static final long DEFAULT_TIMEOUT = 10;

    /**
     * POST 请求
     *
     * @param url               请求URL
     * @param requestHeaders    请求头
     * @param requestBody       请求体
     * @param queryParams       请求URL参数
     * @param connectionTimeout 连接超时时间（单位：秒）
     * @param readTimeout       读取超时时间（单位：秒）
     * @return
     */
    public static String post(String url, Map<String, Object> requestHeaders, Map<String, Object> requestBody, Map<String, Object> queryParams, Long connectionTimeout, Long readTimeout) throws IOException {
        return request(url, requestHeaders, requestBody, queryParams, connectionTimeout, readTimeout);
    }

    /**
     * POST 请求
     *
     * @param url            请求URL
     * @param requestHeaders 请求头
     * @param requestBody    请求体
     * @return
     * @throws IOException
     */
    public static String post(String url, Map<String, Object> requestHeaders, Map<String, Object> requestBody) throws IOException {
        return post(url, requestHeaders, requestBody, null, null, null);
    }

    /**
     * GET 请求
     *
     * @param url               请求URL
     * @param requestHeaders    请求头
     * @param queryParams       请求URL参数
     * @param connectionTimeout 连接超时时间（单位：秒）
     * @param readTimeout       读取超时时间（单位：秒）
     * @return
     */
    public static String get(String url, Map<String, Object> requestHeaders, Map<String, Object> queryParams, Long connectionTimeout, Long readTimeout) throws IOException {
        return request(url, requestHeaders, null, queryParams, connectionTimeout, readTimeout);
    }

    /**
     * GET 请求
     *
     * @param url            请求URL
     * @param requestHeaders 请求头
     * @param queryParams    请求URL参数
     * @return
     */
    public static String get(String url, Map<String, Object> requestHeaders, Map<String, Object> queryParams) throws IOException {
        return request(url, requestHeaders, null, queryParams, null, null);
    }

    /**
     * 请求
     *
     * @param url               请求URL
     * @param requestHeaders    请求头
     * @param requestBody       请求体
     * @param queryParams       请求URL参数
     * @param connectionTimeout 连接超时时间（单位：秒）
     * @param readTimeout       读取超时时间（单位：秒）
     * @return
     * @throws IOException
     */
    public static String request(String url, Map<String, Object> requestHeaders, Map<String, Object> requestBody, Map<String, Object> queryParams, Long connectionTimeout, Long readTimeout) throws IOException {
        if (StringUtils.isBlank(url)) {
            throw new RuntimeException("请求URL必须不为空");
        }

        // 组装：请求URL参数
        if (MapUtils.isNotEmpty(queryParams)) {
            HttpUrl.Builder httpUrl = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
            queryParams.forEach((k, v) -> httpUrl.addQueryParameter(k, Objects.toString(v)));
            url = String.valueOf(httpUrl);
        }

        Request.Builder requestBuilder = new Request.Builder();
        // 组装：请求头
        if (MapUtils.isNotEmpty(requestHeaders)) {
            requestHeaders.forEach((k, v) -> requestBuilder.addHeader(k, Objects.toString(v)));
        }

        // 组装：请求体
        if (MapUtils.isNotEmpty(requestBody)) {
            RequestBody body = RequestBody.create(JSON.toJSONString(requestBody), MediaType.parse("application/json; charset=utf-8"));
            requestBuilder.addHeader("Content-Type", "application/json").post(body);
        }

        Request request = requestBuilder.url(url).build();


        connectionTimeout = Optional.ofNullable(connectionTimeout).orElse(DEFAULT_TIMEOUT);
        readTimeout = Optional.ofNullable(readTimeout).orElse(DEFAULT_TIMEOUT);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();

        Response response = client
                .newCall(request)
                .execute();

        return Objects.requireNonNull(response.body()).string();
    }


    public static void main(String[] args) throws Exception {
        String url = "https://gcc.11185.cn/ZxptRestGccWEB/gcc/search";
        Map<String, Object> requestHeaders = Collections.singletonMap("Origin", "https://mall.11185.cn");
        Map requestBody = JSON.parseObject("{\"businessId\":\"JY\",\"pageNo\":1,\"isHiden\":\"1\",\"pageSize\":12,\"prodGoodsType\":\"NORMAL\",\"classficationId\":\"JY-0\"}", Map.class);
        String responseBody = OkHttpUtils.post(url, requestBody, requestHeaders, null, null, null);
        System.out.println(responseBody);
    }
}
