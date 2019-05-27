package org.openlca.app.results;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContributionCutoff {

	private Spinner spinner;

	public static ContributionCutoff create(Composite parent, FormToolkit toolkit) {
		ContributionCutoff spinner = new ContributionCutoff(parent, toolkit);
		toolkit.adapt(spinner.spinner);
		return spinner;
	}

	private ContributionCutoff(Composite parent, FormToolkit toolkit) {
		Composite composite = UI.formComposite(parent, toolkit);
		UI.gridLayout(composite, 3, 10, 0);
		UI.gridData(composite, false, false).horizontalAlignment = SWT.RIGHT;
		UI.formLabel(composite, toolkit, M.DontShowSmallerThen);
		spinner = new Spinner(composite, SWT.BORDER);
		UI.formLabel(composite, toolkit, "%");
		spinner.setValues(1, 0, 100, 0, 1, 10);
	}

	public void register(StructuredViewer viewer) {
		setCutoff(viewer, spinner.getSelection() / 100d);
		spinner.addModifyListener((e) -> {
			setCutoff(viewer, spinner.getSelection() / 100d);
		});
	}

	private void setCutoff(StructuredViewer viewer, double value) {
		IContentProvider cp = viewer.getContentProvider();
		if (!(cp instanceof CutoffContentProvider)) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Content provider of viewer with cutoff spinner should "
					+ "implement CutoffContentProvider");
			return;
		}
		CutoffContentProvider provider = (CutoffContentProvider) cp;
		provider.setCutoff(value);
		viewer.refresh();
	}

	public static interface CutoffContentProvider {

		void setCutoff(double cutoff);

	}

}
