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
import org.openlca.io.olca.systransfer.TransferExecutor;
import org.openlca.io.olca.systransfer.TransferPlan;

public class TransferPlanEditor extends SimpleFormEditor {

	private static final String ID = "TransferPlanEditor";

	private TransferPlan plan;
	private boolean running;

	public static void open(TransferPlan plan) {
		if (plan == null || plan.config() == null)	return;
		var key = AppContext.put(plan);
		var name = "Transfer plan - " + Labels.name(plan.config().system());
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
		plan = AppContext.remove(si.id, TransferPlan.class);
		if (plan == null) {
			throw new PartInitException("The transfer plan is no longer available");
		}
	}

	@Override
	protected FormPage getPage() {
		return new TransferPlanPage(this);
	}

	TransferPlan plan() {
		return plan;
	}

	void runTransfer() {
		if (running || plan == null)
			return;
		running = true;
		var execRes = new Res[1];
		App.runWithProgress("Transfer product system", () ->
			execRes[0] = TransferExecutor.of(plan).execute());
		running = false;
		if (execRes[0] == null || execRes[0].isError()) {
			var error = execRes[0] != null
				? execRes[0].error()
				: "Failed to transfer the product system";
			MsgBox.error("Transfer failed", error);
			return;
		}

		var config = plan.config();
		MsgBox.info("Transfer complete",
			"Transferred product system to target database '"
				+ config.target().getName() + "' with "
				+ plan.matches().size() + " provider assignment"
				+ (plan.matches().size() == 1 ? "" : "s")
				+ " and " + plan.copies().size() + " provider "
				+ (plan.copies().size() == 1 ? "copy" : "copies") + ".");
	}
}
