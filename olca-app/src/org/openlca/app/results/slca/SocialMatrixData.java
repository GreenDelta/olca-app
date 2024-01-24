package org.openlca.app.results.slca;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.format.Matrix;
import org.openlca.core.matrix.format.MatrixBuilder;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.model.RiskLevel;
import org.openlca.util.Strings;

import java.util.Optional;

record SocialMatrixData(
		TechIndex techIndex,
		SocialIndex socialIndex,
		Matrix activityData,
		Matrix rawData,
		SocialLevelMatrix levelData
) {

	static Optional<SocialMatrixData> fetch(
			IDatabase db, TechIndex techIndex, SocialIndex socialIndex
	) {
		if (db == null
				|| techIndex == null
				|| techIndex.isEmpty()
				|| socialIndex == null
				|| socialIndex.isEmpty())
			return Optional.empty();
		var builder = new Builder(db, techIndex, socialIndex);
		return Optional.of(builder.build());
	}

	private static class Builder {

		private final IDatabase db;
		private final TechIndex techIndex;
		private final SocialIndex socialIndex;

		private final MatrixBuilder activityData;
		private final MatrixBuilder rawData;
		private final SocialLevelMatrix levelData;

		Builder(IDatabase db, TechIndex techIndex, SocialIndex socialIndex) {
			this.db = db;
			this.techIndex = techIndex;
			this.socialIndex = socialIndex;

			activityData = new MatrixBuilder();
			activityData.minSize(socialIndex.size(), techIndex.size());
			rawData = new MatrixBuilder();
			rawData.minSize(socialIndex.size(), techIndex.size());
			levelData = new SocialLevelMatrix();
		}

		SocialMatrixData build() {

			var q = "select " +
					"f_process, " +
					"f_indicator, " +
					"activity_value, " +
					"raw_amount, " +
					"risk_level from tbl_social_aspects";
			NativeSql.on(db).query(q, r -> {

				long processId = r.getLong(1);
				var techFlows = techIndex.getProviders(processId);
				if (techFlows.isEmpty())
					return true;
				var indicator = socialIndex.getForId(r.getLong(2));
				if (indicator == null)
					return true;
				var i = socialIndex.of(indicator);

				var activityValue = r.getDouble(3);
				var rawValue = r.getString(4);
				var riskLevel = riskLevelOf(r.getString(5));

				for (var techFlow : techFlows) {

					int j = techIndex.of(techFlow);
					activityData.set(i, j, activityValue);

					if (Strings.notEmpty(rawValue)) {
						try {
							double v = Double.parseDouble(rawValue);
							rawData.set(i, j, v);
						} catch (Exception ignored) {
						}
					}

					if (riskLevel != null) {
						levelData.put(indicator, techFlow, riskLevel);
					}
				}
				return true;
			});

			return new SocialMatrixData(
					techIndex,
					socialIndex,
					activityData.finish(),
					rawData.finish(),
					levelData
			);
		}

		private RiskLevel riskLevelOf(String name) {
			try {
				return RiskLevel.valueOf(name);
			} catch (Exception e) {
				return null;
			}
		}
	}
}
