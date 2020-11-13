package swiftly.utils;

import javafx.scene.image.Image;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SwiftlyUtils {
    private static final Logger LOGGER = Logger.getRootLogger();
    private static final HashMap<String, CachedValue<Image>> imagesCache = new HashMap<>();
    private static final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(1);

    static {
        // clean cache
        schedule.scheduleAtFixedRate(() -> {
            synchronized (imagesCache) {
                var keys = new String[imagesCache.keySet().size()];
                imagesCache.keySet().toArray(keys);
                for (var k : keys) {
                    if (imagesCache.get(k).isDead()) {
                        imagesCache.remove(k);
                    }
                }
            }
        }, 5, 5, TimeUnit.MINUTES);
    }


    public static Image getImageFromUrl(String url) {
        var cachedImage = imagesCache.get(url);
        if (cachedImage == null) {
            cachedImage = new CachedValue<>(new Image(url, true), 5 * 60 * 1000);
            imagesCache.put(url, cachedImage);
        } else cachedImage.refresh();
        return cachedImage.get();
    }


}
