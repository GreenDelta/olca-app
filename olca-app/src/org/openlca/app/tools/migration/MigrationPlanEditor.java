package org.openlca.app.tools.migration;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.io.olca.migration.MigrationConfig;
import org.openlca.io.olca.migration.MigrationPlan;

public class MigrationPlanEditor extends SimpleFormEditor {

	private static final String ID = "MigrationPlanEditor";

	private MigrationCommand command;

	static void open(MigrationCommand cmd) {
		if (cmd == null)
			return;
		var key = AppContext.put(cmd);
		var input = new SimpleEditorInput(key, "Migration plan");
		Editors.open(input, ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.LINK.get());
		if (!(input instanceof SimpleEditorInput si)) {
			throw new PartInitException("No migration plan provided");
		}
		command = AppContext.remove(si.id, MigrationCommand.class);
		if (command == null) {
			throw new PartInitException("The migration plan is no longer available");
		}
	}

	@Override
	protected FormPage getPage() {
		return new MigrationPlanPage(this);
	}

	MigrationPlan plan() {
		return command.plan();
	}

	MigrationConfig config() {
		return command.config();
	}

	void runTransfer() {
		var res = App.exec("Execute migration", () -> command.execute());
		if (res.isError()) {
			MsgBox.error("Migration failed", res.error());
			return;
		}
		var b = Question.ask("Migration complete",
			"Successfully migrated to the target database. "
				+ "Do you want to open the target database now?");
		if (b) {
			new DbActivateAction(command.targetConfig()).run();
		}
	}
}
