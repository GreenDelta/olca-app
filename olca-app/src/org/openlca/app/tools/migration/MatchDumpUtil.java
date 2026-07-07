package org.openlca.app.tools.migration;

import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.MsgBox;
import org.openlca.io.olca.migration.MigrationPlan;

class MatchDumpUtil {

	private MatchDumpUtil() {
	}

	static void doSave(MigrationPlan plan) {
		var file = FileChooser.forSavingFile(
			"Store provider matches", "provider-matches.json");
		if (file == null)
			return;
		var res = MatchDumpWriter.write(plan, file);
		if (res.isError()) {
			MsgBox.error("Failed to store matches", res.error());
		}
	}

	static void doApply(MigrationPlan plan, Runnable onSuccess) {
		var file = FileChooser.open("json");
		if (file == null)
			return;
		var res = App.exec(
			"Loading matches from file ...",
			() -> MatchDumpLoader.apply(plan, file));
		if (res.isError()) {
			MsgBox.error("Failed to apply matches", res.error());
			return;
		}
		var stats = res.value();
		if (stats.foundCount() == 0) {
			MsgBox.info("No matching providers found",
				"The selected file does not fit this migration plan. " +
					"No matching provider data was found.");
			return;
		}

		stats.showDialog();
		if (onSuccess != null) {
			onSuccess.run();
		}
	}
}
