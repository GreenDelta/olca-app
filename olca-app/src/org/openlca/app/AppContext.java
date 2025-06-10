package org.openlca.app;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.cache.ProviderMap;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The application context is a singleton used to manage the caching of
/// entities and other application-wide resources. It provides static methods
/// for easy access to the caches but all is bound to the singleton instance.
/// The caches are not thread-safe, and it is intended that it is only used
/// from the UI thread.
public class AppContext {

	private static final AppContext ctx = new AppContext();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<String, Object> map = new HashMap<>();
	private IDatabase db;
	private EntityCache entities;
	private ProviderMap providerMap;

	private AppContext() {
	}

	public static AppContext get() {
		return ctx;
	}

	/// Puts the given object under the given key into the cache.
	public static void put(String key, Object val) {
		ctx.map.put(key, val);
	}

	/// Puts the given object into the cache. Allocates a new key and returns it.
	public static String put(Object val) {
		String key = UUID.randomUUID().toString();
		put(key, val);
		return key;
	}

	/// Removes the object with the given key from the cache and returns it.
	public static <T> T remove(String key, Class<T> type) {
		Object o = ctx.map.remove(key);
		return type.isInstance(o)
				? type.cast(o)
				: null;
	}

	/// Removes and returns the object with the given key from the cache. It makes
	/// an unsafe cast to T, so you need to be sure what you are doing here.
	@SuppressWarnings("unchecked")
	public static <T> T remove(String key) {
		return (T) ctx.map.remove(key);
	}

	public static ProviderMap getProviderMap() {
		if (ctx.providerMap == null && ctx.db != null) {
			ctx.providerMap = ProviderMap.create(ctx.db);
		}
		return ctx.providerMap;
	}

	public static EntityCache getEntityCache() {
		if (ctx.entities == null && ctx.db != null) {
			ctx.entities = EntityCache.create(ctx.db);
		}
		return ctx.entities;
	}

	/// Changes the database of the application context. This will clear the
	/// caches and create new ones if necessary.
	public static void change(IDatabase db) {
		clear();
		if (db == null) {
			ctx.db = null;
			ctx.entities = null;
			return;
		}
		ctx.db = db;
		ctx.log.trace("create app cache for {}", db);
		ctx.entities = EntityCache.create(db);
	}

	public static void clear() {
		ctx.log.trace("clear app cache");
		evictAll();
		ctx.entities = null;
		ctx.providerMap = null;
		ctx.map.clear();
	}

	public static void evict(Descriptor d) {
		if (d == null || ctx.entities == null)
			return;
		if (d.type == null || shouldEvictAll(d.type)) {
			evictAll();
			return;
		}
		ctx.evictEntity(d);
		ctx.providerMap = null;
	}

	public static void evictAll() {
		ctx.log.trace("evict all from cache");
		if (ctx.entities != null) {
			ctx.entities.invalidateAll();
		}
		ctx.providerMap = null;
	}

	private void evictEntity(Descriptor d) {
		var cache = ctx.entities;
		if (cache == null)
			return;
		long id = d.id;
		Class<?> clazz = d.getClass();
		log.trace("evict from entity cache {} with id={}", clazz, id);
		cache.invalidate(clazz, id);
		if (d.type == null)
			return;
		clazz = d.type.getModelClass();
		log.trace("evict from entity cache {} with id={}", clazz, id);
		cache.invalidate(clazz, id);
	}

	private static boolean shouldEvictAll(ModelType type) {
		return type == ModelType.UNIT_GROUP
				|| type == ModelType.FLOW
				|| type == ModelType.FLOW_PROPERTY
				|| type == ModelType.CATEGORY;
	}
}
