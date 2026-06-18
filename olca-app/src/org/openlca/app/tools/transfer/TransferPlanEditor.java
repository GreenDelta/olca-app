package org.openlca.app.tools.transfer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.commons.Res;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.olca.systransfer.TransferConfig;
import org.openlca.io.olca.systransfer.TransferExecutor;
import org.openlca.io.olca.systransfer.TransferPlan;

public class TransferPlanEditor extends SimpleFormEditor {

	private static final String ID = "TransferPlanEditor";

	private TransferPlan plan;
	private TransferConfig config;
	private boolean running;

	public static void open(TransferPlan plan, TransferConfig config) {
		if (plan == null || config == null)
			return;
		var storage = new PlanStorage(plan, config);
		var key = AppContext.put(storage);
		var name = "Transfer plan - " + Labels.name(config.system());
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
		var storage = AppContext.remove(si.id, PlanStorage.class);
		if (storage == null) {
			throw new PartInitException("The transfer plan is no longer available");
		}
		plan = storage.plan;
		config = storage.config;
	}

	@Override
	protected FormPage getPage() {
		return new TransferPlanPage(this);
	}

	TransferPlan plan() {
		return plan;
	}

	TransferConfig config() {
		return config;
	}

	void runTransfer() {
		if (running || plan == null || config == null)
			return;
		running = true;
		var execRes = new Res[1];
		App.runWithProgress("Transfer product system", () ->
			execRes[0] = doTransfer());
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
				+ config.target().getName() + "' with "
				+ plan.matches().size() + " provider assignment"
				+ (plan.matches().size() == 1 ? "" : "s")
				+ " and " + plan.copies().size() + " provider "
				+ (plan.copies().size() == 1 ? "copy" : "copies") + ".");
	}

	private Res<ProductSystem> doTransfer() {
		var targetName = config.target().getName();
		var dbConfig = Database.getConfigurations().getAll().stream()
			.filter(c -> c.name().equals(targetName))
			.findFirst()
			.orElse(null);
		if (dbConfig == null) {
			return Res.error("Target database '" + targetName
				+ "' is no longer available");
		}
		var freshTarget = dbConfig.connect(Workspace.dbDir());
		try {
			var freshConfig = new TransferConfig(
				config.source(), freshTarget, config.system(),
				config.strategies());
			return TransferExecutor.of(plan, freshConfig).execute();
		} finally {
			try {
				freshTarget.close();
			} catch (Exception e) {
				// ignore close errors
			}
		}
	}

	private record PlanStorage(TransferPlan plan, TransferConfig config) {
	}
}
