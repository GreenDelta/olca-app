package org.openlca.app.editors.reports;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.resources.ImageType;
import org.openlca.util.Strings;

class ReportEditorInput implements IEditorInput {

	// TODO: use report descriptor later when report is stored in database
	// an editor input should be always lightweight
	private final Report report;

	public ReportEditorInput(Report report) {
		this.report = report;
	}

	public Report getReport() {
		return report;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return report != null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.PROJECT_ICON.getDescriptor();
	}

	@Override
	public String getName() {
		String name = report.getTitle() != null ? report.getTitle() : "Report";
		return Strings.cut(name, 75);
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return report.getTitle() != null ? report.getTitle() : "Report";
	}

}