package org.openlca.app.db;

import java.util.Objects;

import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Categories;
import org.openlca.util.Categories.PathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the cache management of the application.
 */
public final class Cache {

	private static Logger log = LoggerFactory.getLogger(Cache.class);
	private static AppCache appCache = new AppCache();
	private static EntityCache entityCache;
	private static MatrixCache matrixCache;
	private static PathBuilder pathCache;

	private Cache() {
	}

	public static EntityCache getEntityCache() {
		return entityCache;
	}

	public static MatrixCache getMatrixCache() {
		return matrixCache;
	}

	public static PathBuilder getPathCache() {
		if (pathCache == null) {
			var db = Objects.requireNonNull(Database.get());
			pathCache = Categories.pathsOf(db);
		}
		return pathCache;
	}

	public static void close() {
		log.trace("close cache");
		evictAll();
		entityCache = null;
		matrixCache = null;
		appCache = null;
		pathCache = null;
	}

	/**
	 * Initializes the caches for the given database. Old cache instances are
	 * closed.
	 */
	public static void create(IDatabase database) {
		log.trace("create cache");
		close();
		entityCache = EntityCache.create(database);
		matrixCache = MatrixCache.createLazy(database);
		appCache = new AppCache();
	}

	public static AppCache getAppCache() {
		return appCache;
	}

	public static void evict(Descriptor d) {
		if (d == null)
			return;
		log.trace("evict {} with ID {}", d.getClass(), d.id);
		if (d.type == null)
			evictAll(); // to be on the save side
		else if (shouldEvictAll(d.type)) {
			if (entityCache != null)
				entityCache.invalidateAll();
			evictFromMatrices(d);
		} else {
			evictEntity(d);
			evictFromMatrices(d);
		}
	}

	private static boolean shouldEvictAll(ModelType type) {
		return type != null && type.isOneOf(
				ModelType.UNIT,
				ModelType.UNIT_GROUP,
				ModelType.FLOW,
				ModelType.FLOW_PROPERTY,
				ModelType.CATEGORY);
	}

	public static void evictAll() {
		log.trace("evict all from caches");
		if (entityCache != null) {
			entityCache.invalidateAll();
		}
		if (matrixCache != null) {
			matrixCache.evictAll();
		}
		pathCache = null;
	}

	private static void evictEntity(Descriptor d) {
		if (entityCache == null)
			return;
		long id = d.id;
		Class<?> clazz = d.getClass();
		log.trace("evict from entity cache {} with id={}", clazz, id);
		entityCache.invalidate(clazz, id);
		if (d.type == null)
			return;
		clazz = d.type.getModelClass();
		log.trace("evict from entity cache {} with id={}", clazz, id);
		entityCache.invalidate(clazz, id);
		if (d.type == ModelType.CATEGORY) {
			pathCache = null;
		}
	}

	private static void evictFromMatrices(Descriptor d) {
		if (matrixCache == null)
			return;
		matrixCache.evict(d.type, d.id);
	}

	public static void registerNew(Descriptor descriptor) {
		if (matrixCache == null)
			return;
		log.trace("register new model {}", descriptor);
		matrixCache.registerNew(descriptor.type, descriptor.id);
	}

}
