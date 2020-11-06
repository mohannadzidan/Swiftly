package swiftly;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RealtimeDatabase {
    public enum ErrorCodes {
        AUTH_REVOKED,
        STREAM_ENDED_UNEXPECTEDLY,
        CANCELED_BY_END_POINT,
        PERMISSION_DENIED,
        UNKNOWN
    }

    private static final Logger LOGGER = Logger.getRootLogger();
    private final String baseUrl;
    private final CloseableHttpClient httpclient;
    private final HashMap<String, Object> cache = new HashMap<>();

    public RealtimeDatabase(String baseUrl) {
        this.baseUrl = baseUrl;
        httpclient = HttpClients.createDefault();
        cache.put("root", new HashMap<String, Object>());
    }

    public void close() throws IOException {
        httpclient.close();
    }

    public RealtimeDatabaseConnection onValue(RealtimeDatabaseConnection connection) {
        new Thread(() -> {
            String url = connection.getRequestUrl(baseUrl);
            HttpGet httpget = new HttpGet(url);
            httpget.setHeader("accept", "text/event-stream");
            CloseableHttpResponse response;
            try {
                synchronized (httpclient) {
                    response = httpclient.execute(httpget);
                }
            } catch (IOException e) {
                connection.abort();
                LOGGER.error("Couldn't make the request!");
                e.printStackTrace();
                connection.getCallback().onError(ErrorCodes.UNKNOWN);
                return;
            }
            StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                connection.abort();
                LOGGER.error(statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
                switch (statusLine.getStatusCode()) {
                    case 401 -> connection.getCallback().onError(ErrorCodes.PERMISSION_DENIED);
                    default -> connection.getCallback().onError(ErrorCodes.UNKNOWN);
                }

                return;
            }
            if (entity == null) {
                connection.abort();
                LOGGER.error("Response contains no content");
                connection.getCallback().onError(ErrorCodes.UNKNOWN);

                return;
            }
            if (!entity.isStreaming()) {
                connection.abort();
                LOGGER.error("The entity isn't streaming!");
                connection.getCallback().onError(ErrorCodes.UNKNOWN);

                return;
            }
            final byte[] buffer = new byte[connection.getBufferSize()];
            final ObjectMapper mapper = new ObjectMapper();
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
            };
            while (!connection.isAborted()) {
                int bytesLength;
                InputStream stream;
                try {
                    stream = entity.getContent();
                    bytesLength = stream.read(buffer);
                } catch (IOException e) {
                    connection.abort();
                    LOGGER.error("Exception thrown while reading the stream");
                    e.printStackTrace();
                    connection.getCallback().onError(ErrorCodes.UNKNOWN);

                    break;
                }
                if (bytesLength > 0) {

                    String[] rawResponse = new String(Arrays.copyOf(buffer, bytesLength)).split("\n");
                    String event = rawResponse[0].substring(7); // after 'event: '
                    if (event.equals("put") || event.equals("patch")) {
                        String jsonData = rawResponse[1].substring(6); // after 'data: '
                        Map<String, Object> data;
                        try {
                            data = mapper.readValue(jsonData, typeRef);
                            // update cache
                            putToCache((String) data.get("path"), data.get("data"));
                            // LOGGER.debug("Updated cache = " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cache));
                            connection.getCallback().onReceive(getFromCache(connection.getPath()));
                        } catch (IOException e) {
                            connection.abort();
                            LOGGER.error("Exception thrown while parsing json data");
                            e.printStackTrace();
                            connection.getCallback().onError(ErrorCodes.UNKNOWN);

                        }

                    } else if (event.equals("auth_revoked")) {
                        connection.abort();
                        LOGGER.error("Authentication revoked!");
                        connection.getCallback().onError(ErrorCodes.AUTH_REVOKED);
                    } else if (event.equals("cancel")) {
                        connection.abort();
                        LOGGER.error("Streaming canceled!");
                        connection.getCallback().onError(ErrorCodes.CANCELED_BY_END_POINT);
                    }

                } else {
                    // end of stream
                    connection.abort();
                    LOGGER.error("Stream ended!");
                    connection.getCallback().onError(ErrorCodes.STREAM_ENDED_UNEXPECTEDLY);
                }
            }
            try {
                EntityUtils.consume(entity);
            } catch (IOException e) {
                LOGGER.error("Exception thrown while consuming the HttpEntity");
                e.printStackTrace();
            }
        }).start();
        return connection;
    }

    private Object getFromCache(String path) {
        if (path.charAt(0) == '/')
            path = "root" + path;
        else
            path = "root/" + path;
        LOGGER.debug("path-" + path);
        var current = cache;

        var keys = path.split("/");
        if (keys.length == 1) {
            // overwrite root
            return current.get("root");
        }
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (current.containsKey(key)) {
                current = (HashMap<String, Object>) current.get(key);
            } else {
                return null;
            }
        }
        return current.get(keys[keys.length - 1]);
    }

    private void putToCache(String path, Object newData) {
        if (path.charAt(0) == '/')
            path = "root" + path;
        else
            path = "root/" + path;
        LOGGER.debug("path-" + path);
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
                current.put(key, new HashMap<String, Object>());
            }
        }
        current.put(keys[keys.length - 1], newData);
    }

    public RealtimeDatabaseConnection onValue(StreamingCallback callback, int bufferSize, String path, String... filters) {
        return onValue(new RealtimeDatabaseConnection(callback, bufferSize, this, path, filters));
    }

}
