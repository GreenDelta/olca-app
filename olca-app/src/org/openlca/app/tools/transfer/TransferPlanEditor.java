package org.openlca.app.tools.transfer;

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
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.io.olca.systransfer.TransferConfig;
import org.openlca.io.olca.systransfer.TransferPlan;

public class TransferPlanEditor extends SimpleFormEditor {

	private static final String ID = "TransferPlanEditor";

	private TransferCommand command;

	static void open(TransferCommand cmd) {
		if (cmd == null)
			return;
		var key = AppContext.put(cmd);
		var name = "Transfer plan - " + Labels.name(cmd.config().system());
		var input = new SimpleEditorInput(key, name);
		Editors.open(input, ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.DATABASE_DISABLED.get());
		if (!(input instanceof SimpleEditorInput si)) {
			throw new PartInitException("No transfer plan provided");
		}
		command = AppContext.remove(si.id, TransferCommand.class);
		if (command == null) {
			throw new PartInitException("The transfer plan is no longer available");
		}
	}

	@Override
	protected FormPage getPage() {
		return new TransferPlanPage(this);
	}

	TransferPlan plan() {
		return command.plan();
	}

	TransferConfig config() {
		return command.config();
	}

	void runTransfer() {
		var res = App.exec("Transfer product system", () -> command.execute());
		if (res.isError()) {
			MsgBox.error("Transfer failed", res.error());
			return;
		}
		var b = Question.ask("Transfer complete",
			"Successfully transferred the product system to the target database. "
				+ "Do you want to open the target database now?");
		if (b) {
			new DbActivateAction(command.targetConfig()).run();
		}
	}
}
