package swiftly;

import org.apache.http.NameValuePair;

import java.util.Hashtable;
import java.util.TreeMap;

public class RealtimeDatabaseConnection {
    private final String path;
    private final NameValuePair[] queries;
    private boolean online = true;

    public RealtimeDatabaseConnection(String path, NameValuePair[] queries) {
        this.path = path;
        this.queries = queries;
    }
    public  void disconnect() {
        online = false;
    }

    public boolean isOnline() {
        return online;
    }

    public String getPath() {
        return path;
    }

    public NameValuePair[] getQueries() {
        return queries;
    }
}
