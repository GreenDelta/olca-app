package org.openlca.core.editors.result;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.UI;
import org.openlca.app.UIFactory;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;

/**
 * Info page for the analyze editor. Displays the product system and LCIA
 * information
 * 
 * @author Sebastian Greve
 * 
 */
public class ResultInfoPage extends ModelEditorPage {

	private ResultInfo resultInfo;
	private FormToolkit toolkit;

	public ResultInfoPage(ModelEditor editor, ResultInfo info) {
		super(editor, "org.openlca.core.editors.analyze",
				Messages.Common_GeneralInformation);
		this.resultInfo = info;
	}

	@Override
	protected void createContents(Composite body, FormToolkit toolkit) {
		this.toolkit = toolkit;
		GridLayout layout = (GridLayout) UIFactory
				.createGridLayout(2, false, 5);
		layout.marginTop = 25;
		body.setLayout(layout);
		createTexts(body);
	}

	private void createTexts(Composite body) {
		if (resultInfo == null)
			return;
		createText(body, Messages.Results_ProductSystem,
				resultInfo.getProductSystem());
		createText(body, Messages.Results_TargetAmount,
				resultInfo.getProductFlow());
		createText(body, Messages.Results_LCIAMethod,
				resultInfo.getImpactMethod());
		createText(body, Messages.Results_NormalizationWeightingSet,
				resultInfo.getNwSet());
		createText(body, Messages.Results_CalculationMethod,
				Messages.Results_MatrixMethod);
	}

	private void createText(Composite body, String label, String value) {
		if (value == null)
			return;
		Text text = UI.formText(body, toolkit, label);
		text.setText(value);
		text.setEditable(false);
	}

	@Override
	protected String getFormTitle() {
		String sys = resultInfo != null ? resultInfo.getProductSystem() : "";
		return NLS.bind(Messages.Results_ResultsOf, sys);
	}

	@Override
	protected void initListeners() {
	}

	@Override
	protected void setData() {
	}

}
