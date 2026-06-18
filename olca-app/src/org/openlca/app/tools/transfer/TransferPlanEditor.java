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
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.commons.Res;
import org.openlca.io.olca.systransfer.TransferConfig;
import org.openlca.io.olca.systransfer.TransferPlan;

public class TransferPlanEditor extends SimpleFormEditor {

	private static final String ID = "TransferPlanEditor";

	private TransferCommand command;
	private boolean running;

	public static void open(TransferCommand cmd) {
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
		return command != null ? command.plan() : null;
	}

	TransferConfig config() {
		return command != null ? command.config() : null;
	}

	void runTransfer() {
		if (running || command == null)
			return;
		running = true;
		var execRes = new Res[1];
		App.runWithProgress("Transfer product system", () ->
			execRes[0] = command.execute());
		running = false;
		if (execRes[0] == null || execRes[0].isError()) {
			var error = execRes[0] != null
				? execRes[0].error()
				: "Failed to transfer the product system";
			MsgBox.error("Transfer failed", error);
			return;
		}

		MsgBox.info("Transfer complete",
			"Transferred product system to target database '"
				+ command.config().target().getName() + "' with "
				+ command.plan().matches().size() + " provider assignment"
				+ (command.plan().matches().size() == 1 ? "" : "s")
				+ " and " + command.plan().copies().size() + " provider "
				+ (command.plan().copies().size() == 1 ? "copy" : "copies") + ".");
	}
}
