package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.cloud.ui.compare.ModelLabelProvider;
import org.openlca.app.cloud.ui.compare.ModelUtil;
import org.openlca.app.cloud.ui.compare.PropertyLabels;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.diff.ActionType;
import org.openlca.app.cloud.ui.diff.Site;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;

public class TextDiffDialog extends FormDialog {

	private final JsonNode node;
	private final ActionType action;
	
	public TextDiffDialog(JsonNode node, ActionType action) {
		super(UI.shell());
		this.node = node;
		this.action = action;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 300);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,	true);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		String property = PropertyLabels.get(ModelUtil.getType(node.parent.getElement()), node.property);
		ScrolledForm form = UI.formHeader(mform, "Compare - " + property);
		Composite body = form.getBody();
		UI.gridLayout(body, 2, 0, 0).makeColumnsEqualWidth = true;
		UI.gridData(body, true, true);
		ModelLabelProvider label = new ModelLabelProvider();
		String leftText = label.getValueText(node, Site.LOCAL);
		String rightText = label.getValueText(node, Site.REMOTE);
		createText(body, leftText, rightText, Site.LOCAL);
		createText(body, rightText, leftText, Site.REMOTE);
	}
	
	private void createText(Composite parent, String value, String otherValue, Site site) {
		StyledString styled = new StyledString(value);
		new DiffStyle().applyTo(styled, otherValue, site, action);
		StyledText text = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		text.setText(styled.toString());
		text.setStyleRanges(styled.getStyleRanges());
		text.setEditable(false);
		text.setBackground(Colors.white());
		UI.gridData(text, true, true);
	}

}