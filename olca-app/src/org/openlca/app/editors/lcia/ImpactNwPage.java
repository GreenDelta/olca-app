package org.openlca.app.editors.lcia;

import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;

class ImpactNwPage extends ModelPage<ImpactMethod> {

	private final ImpactMethodEditor editor;
	private NwFactorViewer factorViewer;
	private NwSetViewer setViewer;

	ImpactNwPage(ImpactMethodEditor editor) {
		super(editor, "ImpactNormalizationWeightingPage",
				M.NormalizationWeighting);
		this.editor = editor;
		editor.onEvent(editor.IMPACT_CATEGORY_CHANGE, () -> {
			if (factorViewer != null) {
				factorViewer.refresh();
			}
		});
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var section = UI.section(body, tk, M.NormalizationWeightingSets);
		UI.gridData(section, true, true);
		var client = UI.composite(section, tk);
		section.setClient(client);
		UI.gridLayout(client, 1);

		var sashForm = createSash(client);
		setViewer = createNwSetViewer(section, sashForm);
		factorViewer = new NwFactorViewer(sashForm, editor);
		sashForm.setWeights(25, 75);
		setViewer.selectFirst();
		body.setFocus();
		form.reflow(true);
		editor.onSaved(this::updateInput);
	}

	private SashForm createSash(Composite client) {
		var sash = new SashForm(client, SWT.NONE);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 400;
		sash.setLayoutData(gd);
		sash.setLayout(new GridLayout(2, false));
		return sash;
	}

	private NwSetViewer createNwSetViewer(Section section, SashForm sashForm) {
		var viewer = new NwSetViewer(sashForm, editor);
		CommentAction.bindTo(section, viewer, "nwSets", editor.getComments());
		viewer.addSelectionChangedListener((selection) -> factorViewer.setInput(selection));
		viewer.setInput(getModel());
		return viewer;
	}

	private void updateInput() {
		var newNwSet = findNewNwSet();
		setViewer.setInput(getModel());
		setViewer.select(newNwSet);
	}

	private NwSet findNewNwSet() {
		List<NwSet> list = getModel().nwSets;
		if (list.isEmpty())
			return null;
		NwSet old = setViewer.getSelected();
		if (old == null)
			return list.get(0);
		for (NwSet set : list) {
			if (Objects.equals(old.refId, set.refId))
				return set;
		}
		return list.get(0);
	}
}
