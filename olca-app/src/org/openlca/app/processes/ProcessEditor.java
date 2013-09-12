package org.openlca.app.processes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;

public class ProcessEditor extends ModelEditor<Process> implements IEditor {

	public static String ID = "editors.process";
	private Logger log = LoggerFactory.getLogger(getClass());

	public ProcessEditor() {
		super(Process.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProcessInfoPage(this));
			addPage(new ProcessExchangePage(this));
			addPage(new ProcessAdminInfoPage(this));
			addPage(new ProcessModelingPage(this));
			addPage(new ProcessParameterPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getModel().getDocumentation().setLastChange(
				Calendar.getInstance().getTime());
		super.doSave(monitor);
	}

}
