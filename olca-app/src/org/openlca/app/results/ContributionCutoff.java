package org.openlca.app.results;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;

public class ContributionCutoff {

	private Spinner spinner;

	public static ContributionCutoff create(Composite parent, FormToolkit toolkit) {
		return create(parent, toolkit, 0.01);
	}

	public static ContributionCutoff create(Composite parent, FormToolkit toolkit, double cutoff) {
		if (cutoff < 0 || cutoff > 1)
			throw new IllegalArgumentException("Cutoff must be between 0 and 1");
		ContributionCutoff spinner = new ContributionCutoff(parent, toolkit);
		spinner.spinner.setSelection((int) (cutoff * 1000d));
		toolkit.adapt(spinner.spinner);
		return spinner;
	}

	private ContributionCutoff(Composite parent, FormToolkit toolkit) {
		Composite composite = UI.formComposite(parent, toolkit);
		UI.gridLayout(composite, 3, 10, 0);
		UI.gridData(composite, false, false).horizontalAlignment = SWT.RIGHT;
		UI.formLabel(composite, toolkit, M.Cutoff);
		spinner = new Spinner(composite, SWT.BORDER);
		UI.formLabel(composite, toolkit, "%");
		initDefaults();
	}

	private void initDefaults() {
		spinner.setIncrement(1);
		spinner.setMinimum(0);
		spinner.setMaximum(1000);
		spinner.setDigits(1);
	}

	public void register(StructuredViewer viewer) {
		setCutoff(viewer, spinner.getSelection() / 100d);
		spinner.addModifyListener((e) -> {
			setCutoff(viewer, spinner.getSelection() / 100d);
		});
	}

	private void setCutoff(StructuredViewer viewer, double value) {
		if (!(viewer.getContentProvider() instanceof CutoffContentProvider))
			throw new IllegalArgumentException(
					"Content provider of viewer for cutoff spinner must implement CutoffContentProvider");
		CutoffContentProvider content = (CutoffContentProvider) viewer.getContentProvider();
		content.setCutoff(value);
		viewer.refresh();
	}

	public static interface CutoffContentProvider {

		void setCutoff(double cutoff);

	}

}
