package swiftly;

import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import swiftly.promise.Promise;
import swiftly.promise.PromiseError;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealtimeDatabase extends FirebaseBase {
    public enum ErrorCode {
        AUTH_REVOKED,
        STREAM_ENDED_UNEXPECTEDLY,
        CANCELED_BY_END_POINT,
        PERMISSION_DENIED,
        UNKNOWN
    }

    private static final Logger LOGGER = Logger.getRootLogger();

    private final HashMap<String, Object> cache = new HashMap<>();
    private final ArrayList<RealtimeDatabaseConnection> connections = new ArrayList<>();
    private final ExecutorService executor;
    private final CrossThreadRunnable crossThreadRunnable;

    public RealtimeDatabase(String baseUrl, int threadPoolSize, CrossThreadRunnable crossThreadRunnable) throws FirebaseException {
        super(baseUrl);
        executor = Executors.newFixedThreadPool(threadPoolSize);
        this.crossThreadRunnable = crossThreadRunnable;
    }

    public RealtimeDatabase(String baseUrl, Boolean useJsonExtension, int threadPoolSize, CrossThreadRunnable crossThreadRunnable) throws FirebaseException {
        super(baseUrl, useJsonExtension);
        executor = Executors.newFixedThreadPool(threadPoolSize);
        this.crossThreadRunnable = crossThreadRunnable;
    }

    public RealtimeDatabase(String baseUrl, String secureToken, int threadPoolSize, CrossThreadRunnable crossThreadRunnable) throws FirebaseException {
        super(baseUrl, secureToken);
        executor = Executors.newFixedThreadPool(threadPoolSize);
        this.crossThreadRunnable = crossThreadRunnable;
    }

    public void shutdown() {
        connections.forEach(RealtimeDatabaseConnection::disconnect);
        executor.shutdown();
    }

    public Promise<Object> onValue(String path, int bufferSize, NameValuePair... queries) {

        return new Promise<>((resolve, errorHandler) -> {
            new Thread(() -> {
                String url = buildFullUrlFromRelativePath(path, queries);
                HttpGet httpget = new HttpGet(url);
                httpget.setHeader("accept", "text/event-stream");
                CloseableHttpResponse response;
                try {
                    response = client.execute(httpget);
                    LOGGER.info("Start streaming:" + url);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error("Couldn't make the request!");
                    errorHandler.handle(new PromiseError(ErrorCode.UNKNOWN));
                    return;
                }
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                if (statusLine.getStatusCode() >= 300) {
                    LOGGER.error(statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
                    switch (statusLine.getStatusCode()) {
                        case 401:
                            errorHandler.handle(new PromiseError(ErrorCode.PERMISSION_DENIED));
                            break;
                        default:
                            errorHandler.handle(new PromiseError(ErrorCode.UNKNOWN));
                            break;
                    }
                    return;
                }
                if (entity == null) {
                    LOGGER.error("Response contains no content");
                    errorHandler.handle(new PromiseError(ErrorCode.UNKNOWN));
                    return;
                }
                if (!entity.isStreaming()) {
                    try {
                        EntityUtils.consume(entity);
                    } catch (IOException e) {
                        LOGGER.error("Exception thrown while consuming the HttpEntity");
                        e.printStackTrace();
                    }
                    LOGGER.error("The entity isn't streaming!");
                    errorHandler.handle(new PromiseError(ErrorCode.UNKNOWN));
                    return;
                }
                final byte[] buffer = new byte[bufferSize];
                final ObjectMapper mapper = new ObjectMapper();
                final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
                };
                while (true) {
                    int bytesLength;
                    InputStream stream;
                    try {
                        stream = entity.getContent();
                        bytesLength = stream.read(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                        LOGGER.error("Exception thrown while reading the stream");
                        errorHandler.handle(new PromiseError(ErrorCode.UNKNOWN));
                        break;
                    }
                    if (bytesLength > 0) {

                        String[] rawResponse = new String(Arrays.copyOf(buffer, bytesLength)).split("\n");
                        String event = rawResponse[0].substring(7); // after 'event: '

                        if ("put".equals(event) || "patch".equals(event)) {
                            String jsonData = rawResponse[1].substring(6); // after 'data: '
                            try {
                                Map<String, Object> data = mapper.readValue(jsonData, typeRef);
                                // update cache
                                putToCache(path + data.get("path"), data.get("data"));
                                //LOGGER.debug("Updated cache = " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cache));
                                if (crossThreadRunnable != null)
                                    crossThreadRunnable.runInAnotherThread(() -> resolve.run((Map<String, Object>) getFromCache(path)));
                                else resolve.run((Map<String, Object>) getFromCache(path));
                            } catch (IOException e) {
                                e.printStackTrace();
                                LOGGER.error("Exception thrown while parsing json data");
                                errorHandler.handle(new PromiseError(ErrorCode.UNKNOWN));
                                break;
                            }
                        } else if ("auth_revoked".equals(event)) {
                            LOGGER.error("Authentication revoked!");
                            errorHandler.handle(new PromiseError(ErrorCode.AUTH_REVOKED));
                            break;
                        } else if ("cancel".equals(event)) {
                            LOGGER.error("Streaming canceled!");
                            errorHandler.handle(new PromiseError(ErrorCode.CANCELED_BY_END_POINT));
                            break;
                        }

                    } else {
                        // end of stream
                        LOGGER.error("Stream ended!");
                        errorHandler.handle(new PromiseError(ErrorCode.STREAM_ENDED_UNEXPECTEDLY));
                        break;
                    }
                }
                try {
                    EntityUtils.consume(entity);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error("Exception thrown while consuming the HttpEntity");
                }
            }).start();
        });
    }

    public Promise<Object> onValue(String path, NameValuePair... queries){
        return onValue(path, 2048, queries);
    }


    private Object getFromCache(String path) {
        var current = cache;
        var keys = path.split("/");
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (current.containsKey(key)) {
                current = (HashMap<String, Object>) current.get(key);
            } else {
                return new HashMap<String, Object>();
            }
        }
        return current.get(keys[keys.length - 1]);
    }

    private synchronized void putToCache(String path, Object newData) {
        var current = cache;
        var keys = path.split("/");
        if (keys.length == 1) {
            // overwrite root
            current.put("root", newData);
            return;
        }
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (current.containsKey(key)) {
                current = (HashMap<String, Object>) current.get(key);
            } else {
                var a = new HashMap<String, Object>();
                current.put(key, a);
                current = a;
            }
        }
        current.put(keys[keys.length - 1], newData);
    }

    /**
     * @param path
     * @return promise of firebase response, the promise error is always a throwable
     */
    public Promise<FirebaseResponse> getAsync(String path, NameValuePair... queries) {
        return new Promise<FirebaseResponse>((resolver, exceptionHandler) -> executor.execute(() -> {
            try {
                FirebaseResponse result = super.get(path, queries);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> resolver.run(result));
                } else {
                    resolver.run(result);
                }
            } catch (Throwable e) {
                if (exceptionHandler == null) throw new RuntimeException(e);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> exceptionHandler.handle(new PromiseError(e)));
                } else {
                    exceptionHandler.handle(new PromiseError(e));
                }
            }
        }));
    }

    public Promise<FirebaseResponse> patchAsync(String path, Map<String, Object> data, NameValuePair... queries) {
        return new Promise<>((resolve, exceptionHandler) -> executor.execute(() -> {
            try {
                FirebaseResponse result = super.patch(path, data, queries);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> resolve.run(result));
                } else {
                    resolve.run(result);
                }
            } catch (Throwable e) {
                if (exceptionHandler == null) throw new RuntimeException(e);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> exceptionHandler.handle(new PromiseError(e)));
                } else {
                    exceptionHandler.handle(new PromiseError(e));
                }
            }
        }));
    }

    public Promise<FirebaseResponse> putAsync(String path, Map<String, Object> data, NameValuePair... queries) {
        return new Promise<>((resolve, exceptionHandler) -> executor.execute(() -> {
            try {
                FirebaseResponse result = super.put(path, data, queries);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> resolve.run(result));
                } else {
                    resolve.run(result);
                }
            } catch (Throwable e) {
                if (exceptionHandler == null) throw new RuntimeException(e);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> exceptionHandler.handle(new PromiseError(e)));
                } else {
                    exceptionHandler.handle(new PromiseError(e));
                }
            }
        }));
    }

    public Promise<FirebaseResponse> postAsync(String path, Map<String, Object> data, NameValuePair... queries) {
        return new Promise<FirebaseResponse>((resolve, exceptionHandler) -> executor.execute(() -> {
            try {
                FirebaseResponse result = super.post(path, data, queries);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> resolve.run(result));
                } else {
                    resolve.run(result);
                }
            } catch (Throwable e) {
                if (exceptionHandler == null) throw new RuntimeException(e);

                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> exceptionHandler.handle(new PromiseError(e)));
                } else {
                    exceptionHandler.handle(new PromiseError(e));
                }
            }
        }));
    }


    public Promise<FirebaseResponse> deleteAsync(String path, NameValuePair... queries) {
        return new Promise<>((resolve, exceptionHandler) -> executor.execute(() -> {
            try {
                FirebaseResponse result = super.delete(path, queries);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> resolve.run(result));
                } else {
                    resolve.run(result);
                }
            } catch (FirebaseException e) {
                if (exceptionHandler == null) throw new RuntimeException(e);
                if (crossThreadRunnable != null) {
                    crossThreadRunnable.runInAnotherThread(() -> exceptionHandler.handle(new PromiseError(e)));
                } else {
                    exceptionHandler.handle(new PromiseError(e));
                }
            }
        }));
    }


}
