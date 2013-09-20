package org.openlca.app.db;

import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.cache.MatrixCache;

/**
 * Contains the cache management of the application.
 */
public final class Cache {

	private static AppCache appCache = new AppCache();
	private static EntityCache entityCache;
	private static MatrixCache matrixCache;

	private Cache() {
	}

	public static EntityCache getEntityCache() {
		return entityCache;
	}

	public static MatrixCache getMatrixCache() {
		return matrixCache;
	}

	public static void close() {
		if (entityCache != null)
			entityCache.invalidateAll();
		if (matrixCache != null)
			; // TODO: matrixCache.invalidateAll();
		entityCache = null;
		matrixCache = null;
		appCache = null;
	}

	/**
	 * Initializes the caches for the given database. Old cache instances are
	 * closed.
	 */
	public static void create(IDatabase database) {
		close();
		entityCache = EntityCache.create(database);
		matrixCache = MatrixCache.create(database);
		appCache = new AppCache();
	}

	public static AppCache getAppCache() {
		return appCache;
	}

}
