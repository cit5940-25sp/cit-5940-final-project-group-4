package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求工具类
 */
@Slf4j
public class HttpUtil {
    // 增加超时时间到30秒
//    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            // 配置代理
//            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)))
//            .build();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .proxy(Proxy.NO_PROXY) // ⬅ Disable proxy explicitly
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 发送GET请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @return 响应结果
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        log.debug("发送GET请求: {}", url);
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        Request request = requestBuilder.build();
        try (Response response = HTTP_CLIENT
                .newCall(request)
                .execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response
                    .body()
                    .string();
            log.debug("GET请求响应: {}", responseBody);
            return responseBody;
        }
    }

    /**
     * 发送POST请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param body    请求体
     * @return 响应结果
     */
    public static String post(String url, Map<String, String> headers, String body) throws IOException {
        log.debug("发送POST请求: {}, body: {}", url, body);
        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json"));
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        Request request = requestBuilder.build();
        try (Response response = HTTP_CLIENT
                .newCall(request)
                .execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = null;
            if (response.body() != null) {
                responseBody = response
                        .body()
                        .string();
            }
            log.debug("POST请求响应: {}", responseBody);
            return responseBody;
        }
    }

    /**
     * 将JSON字符串转换为对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类
     * @return 转换后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJson(Object object) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
     * URL编码
     *
     * @param value 需要编码的字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("URL编码异常", e);
            return value;
        }
    }
}
