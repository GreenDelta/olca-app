package org.openlca.app.editors.processes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.editors.parameters.ParameterChangeSupport;
import org.openlca.app.editors.parameters.ParameterPage;
import org.openlca.app.editors.processes.allocation.AllocationPage;
import org.openlca.app.editors.processes.exchanges.ProcessExchangePage;
import org.openlca.app.editors.processes.social.SocialAspectsPage;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.LibraryUtil;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.util.AllocationUtils;

public class ProcessEditor extends ModelEditor<Process> {

	// EventBus messages
	public static final String EXCHANGES_CHANGED = "EXCHANGE_CHANGED";
	public static final String SOURCES_CHANGED = "SOURCES_CHANGED";

	public static String ID = "editors.process";
	private ParameterChangeSupport parameterSupport;

	public ProcessEditor() {
		super(Process.class);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		var process = getModel();
		if (process.documentation == null) {
			process.documentation = new ProcessDocumentation();
		}
		evalFormulas();
		parameterSupport = new ParameterChangeSupport();
		parameterSupport.onEvaluation(this::evalFormulas);
		if (process.isFromLibrary()) {
			LibraryUtil.fillExchangesOf(process);
		}
	}

	private void evalFormulas() {
		Process p = getModel();
		var errors = Formulas.eval(Database.get(), p);
		if (!errors.isEmpty()) {
			String message = errors.get(0);
			if (errors.size() > 1)
				message += " (" + (errors.size() - 1) + " more)";
			MsgBox.error(M.FormulaEvaluationFailed, message);
		}
	}

	public ParameterChangeSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		AllocationUtils.cleanup(getModel());
		super.doSave(monitor);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new ProcessExchangePage(this));
			addPage(new AdminInfoPage(this));
			addPage(new ProcessModelingPage(this));
			addPage(ParameterPage.create(this));
			addPage(new AllocationPage(this));
			addPage(new SocialAspectsPage(this));
			addPage(new ImpactPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}
}
