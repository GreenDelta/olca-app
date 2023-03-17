package org.openlca.app.results;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.slf4j.LoggerFactory;

public class ContributionCutoff {

	private final Spinner spinner;

	public static ContributionCutoff create(Composite parent, FormToolkit tk) {
		var spinner = new ContributionCutoff(parent, tk);
		return spinner;
	}

	private ContributionCutoff(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 3, 10, 0);
		UI.gridData(comp, false, false).horizontalAlignment = SWT.RIGHT;
		UI.label(comp, tk, M.DontShowSmallerThen);
		spinner = UI.spinner(comp, tk, SWT.BORDER);
		spinner.setValues(1, 0, 100, 0, 1, 10);
		UI.label(comp, tk, "%");
	}

	public void register(StructuredViewer viewer) {
		setCutoff(viewer, spinner.getSelection() / 100d);
		spinner.addModifyListener(
				$ -> setCutoff(viewer, spinner.getSelection() / 100d));
	}

	private void setCutoff(StructuredViewer viewer, double value) {
		var cp = viewer.getContentProvider();
		if (!(cp instanceof CutoffContentProvider provider)) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("Content provider of viewer with cutoff spinner should "
					+ "implement CutoffContentProvider");
			return;
		}
		provider.setCutoff(value);
		viewer.refresh();
	}

	public interface CutoffContentProvider {

		void setCutoff(double cutoff);

	}

}
