package org.openlca.app.tools.migration;

import static org.openlca.jsonld.Json.*;

import java.io.File;
import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.commons.Res;
import org.openlca.io.olca.migration.MatchingStrategy;
import org.openlca.io.olca.migration.MigrationPlan;
import org.openlca.io.olca.migration.ProviderInfo;
import org.openlca.io.olca.migration.ProviderMatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class MatchDump {

	private MatchDump() {
	}

	static Res<Void> store(MigrationPlan plan, File file) {
		if (plan == null || file == null)
			return Res.error("Plan or file is null");
		try {
			var root = new JsonObject();
			var matchesArray = new JsonArray();
			for (var match : plan.providerMatches()) {
				matchesArray.add(matchJson(match));
			}
			put(root, "matches", matchesArray);
			write(root, file);
			return Res.ok();
		} catch (Exception e) {
			return Res.error("Failed to store matches", e);
		}
	}

	private static JsonObject matchJson(ProviderMatch match) {
		var obj = new JsonObject();
		put(obj, "source", infoJson(match.source()));
		put(obj, "strategy", match.strategy());
		if (match.selected() != null
			&& match.selected().provider() != null) {
			put(obj, "selected", match.selected().provider().refId);
		}
		var altArray = new JsonArray();
		for (var alt : match.alternatives()) {
			altArray.add(infoJson(alt));
		}
		put(obj, "alternatives", altArray);
		return obj;
	}

	private static JsonObject infoJson(ProviderInfo info) {
		if (info == null)
			return null;
		var obj = new JsonObject();
		if (info.provider() != null) {
			put(obj, "provider", asRef(info.provider()));
		}
		if (info.flow() != null) {
			put(obj, "flow", asRef(info.flow()));
		}
		if (info.location() != null) {
			put(obj, "location", asRef(info.location()));
		}
		return obj;
	}

	static Res<Stats> apply(MigrationPlan plan, File file) {
		if (plan == null || file == null)
			return Res.error("Plan or file is null");

		var json = readObject(file);
		if (json.isEmpty())
			return Res.error("Failed to read file: " + file);

		var root = json.get();
		var matchesArray = getArray(root, "matches");
		if (matchesArray == null || matchesArray.isEmpty())
			return Res.error("No matches found in file");

		// build index: source provider refId -> dump entry
		var dumpIdx = new HashMap<String, DumpEntry>();
		for (var elem : matchesArray) {
			if (!elem.isJsonObject())
				continue;
			var matchObj = elem.getAsJsonObject();

			var sourceObj = getObject(matchObj, "source");
			if (sourceObj == null)
				continue;

			var providerObj = getObject(sourceObj, "provider");
			if (providerObj == null)
				continue;

			var sourceRefId = getString(providerObj, "@id");
			if (sourceRefId == null)
				continue;

			var strategy = getEnum(matchObj,
				"strategy", MatchingStrategy.class);
			var selectedRefId = getString(matchObj, "selected");
			if (selectedRefId != null) {
				dumpIdx.put(sourceRefId,
					new DumpEntry(strategy, selectedRefId));
			}
		}

		int checked = plan.providerMatches().size();
		int matching = 0;
		int updated = 0;

		for (var match : plan.providerMatches()) {
			if (match.source() == null
				|| match.source().provider() == null)
				continue;

			var sourceRefId = match.source().provider().refId;
			var entry = dumpIdx.get(sourceRefId);
			if (entry == null)
				continue;

			matching++;

			for (var alt : match.alternatives()) {
				if (alt.provider() == null
					|| !alt.provider().refId.equals(entry.selectedRefId))
					continue;

				// found the matching alternative
				if (alt != match.selected()) {
					match.select(alt, entry.strategy);
					updated++;
				}
				break;
			}
		}

		return Res.ok(new Stats(checked, matching, updated));
	}

	private record DumpEntry(
		MatchingStrategy strategy,
		String selectedRefId
	) {
	}

	record Stats(
		int checkedProviders,
		int matchingSourceProviders,
		int updatedMatches
	) {

		void show() {
			new StatsDialog(this).open();
		}

		private static class StatsDialog extends FormDialog {

			private final Stats stats;

			StatsDialog(Stats stats) {
				super(UI.shell());
				this.stats = stats;
			}

			@Override
			protected void configureShell(Shell shell) {
				super.configureShell(shell);
				shell.setText("Provider match import");
			}

			@Override
			protected void createFormContent(IManagedForm mform) {
				var tk = mform.getToolkit();
				var body = UI.dialogBody(mform.getForm(), tk);
				UI.gridLayout(body, 2, 10, 25);

				tk.createLabel(body, "Checked providers")
					.setFont(UI.boldFont());
				tk.createLabel(body, Integer.toString(stats.checkedProviders));

				tk.createLabel(body, "Matching source providers");
				tk.createLabel(body,
					Integer.toString(stats.matchingSourceProviders));

				tk.createLabel(body, "Updated matches");
				tk.createLabel(body,
					Integer.toString(stats.updatedMatches));
			}

			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				createButton(parent, IDialogConstants.OK_ID,
					IDialogConstants.OK_LABEL, true);
			}

			@Override
			protected Point getInitialSize() {
				return UI.initialSizeOf(this, 400, 200);
			}
		}
	}
}
