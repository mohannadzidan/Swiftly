package swiftly;

public class RealtimeDatabaseConnection {
    private RealtimeDatabase database;
    private String path;
    private String[] filters;
    private StreamingCallback callback;
    private int bufferSize;
    private boolean aborted = false;


    // private httpRequest or entity
    public RealtimeDatabaseConnection(StreamingCallback callback, int bufferSize, RealtimeDatabase database, String path, String... filters) {
        this.callback = callback;
        this.bufferSize = bufferSize;
        this.database = database;
        this.path = path;
        this.filters = filters;
    }


    public String getRequestUrl(String baseUrl) {
        // curl 'https://dinosaur-facts.firebaseio.com/dinosaurs.json?orderBy="weight"&limitToLast=2&print=pretty'

        StringBuilder builder = new StringBuilder(baseUrl);
        builder.append('/').append(path).append(".json");
        if (filters.length > 0) {
            builder.append('?');
            builder.append(filters[0]);
            for (int i = 1; i < filters.length; i++) {
                builder.append('&').append(filters[i]);
            }
        }
        return builder.toString();

    }

    public void abort() {
        aborted = true;
    }

    public void reconnect() {
        if (!aborted) throw new RuntimeException("attempt reconnect on non-aborted connection!");
        this.aborted = false;
        database.onValue(this);

    }

    public StreamingCallback getCallback() {
        return callback;
    }

    public String getPath() {
        return path;
    }

    public String[] getFilters() {
        return filters;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public boolean isAborted() {
        return aborted;
    }
}
