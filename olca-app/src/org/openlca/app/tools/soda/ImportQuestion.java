package org.openlca.app.tools.soda;

import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.openlca.app.M;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.ilcd.descriptors.Descriptor;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.epd.EpdProfiles;

class ImportQuestion {

	private final IDatabase db;
	private final List<Descriptor<?>> selection;
	private final boolean withEpds;

	private ImportQuestion(
		IDatabase db, List<Descriptor<?>> selection, boolean withEpds) {
		this.db = db;
		this.selection = selection;
		this.withEpds = withEpds;
	}

	static boolean isOk(
		IDatabase db, List<Descriptor<?>> selection, boolean withEpds) {
		return new ImportQuestion(db, selection, withEpds).runChecks();
	}

	private boolean runChecks() {
		if (selection == null || selection.isEmpty())
			return false;
		if (db == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NoDatabaseOpenedImportInfo);
			return false;
		}

		if (isForEpds() && hasNoEpdMethods()) {

			var message = "The database does not contain known impact categories"
				+ " that can be mapped to EN 15804 indicators. This may result in"
				+ " very long import times, as the import process will attempt to"
				+ " download them from the server. However, soda4LCA servers often"
				+ " do not include all referenced data for impact categories (such"
				+ " as flows), which can lead to frequent import errors.\n\n"
				+ "You can obtain these indicators for free by downloading the EN"
				+ " 15804 method package from openLCA Nexus.\n\n"
				+ "Would you like to continue with the import anyway?";
			int resp = new MessageDialog(
				UI.shell(),
				"Run import with missing indicators?",
				null,
				message,
				MessageDialog.WARNING,
				new String[] { M.Yes, M.No },
				1).open();
			return resp == 0;
		}

		return Question.ask(
			M.ImportSelectedDataSetQ,
			M.ImportSelectedDataSetQuestion);
	}

	private boolean isForEpds() {
		if (!withEpds)
			return false;
		for (var d : selection) {
			if (d instanceof ProcessDescriptor)
				return true;
		}
		return false;
	}

	private boolean hasNoEpdMethods() {
		var ids = new HashSet<String>();
		for (var p : EpdProfiles.getAll()) {
			for (var i : p.getIndicators()) {
				ids.add(i.getUUID());
			}
		}

		for (var i : db.getDescriptors(ImpactCategory.class)) {
			if (ids.contains(i.refId))
				return false;
		}
		return true;
	}

}
