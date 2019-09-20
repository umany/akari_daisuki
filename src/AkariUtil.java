import java.util.Collections;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;

public class AkariUtil {
	private static final Logger log = Logger.getLogger(AkariUtil.class
			.getName());

	public static Cache getCache() {
		Cache cache = null;
		try {
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			cache = cacheFactory.createCache(Collections.emptyMap());
		} catch (CacheException e) {
			log.severe(e.getMessage());
		}
		return cache;
	}
}
