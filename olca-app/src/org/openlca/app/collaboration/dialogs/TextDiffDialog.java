package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.DiffStyle;
import org.openlca.app.collaboration.viewers.json.olca.ModelLabelProvider;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;

public class TextDiffDialog extends FormDialog {

	private final JsonNode node;

	public TextDiffDialog(JsonNode node) {
		super(UI.shell());
		this.node = node;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 300);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, "Compare");
		var body = form.getBody();
		UI.gridLayout(body, 2, 0, 0).makeColumnsEqualWidth = true;
		UI.gridData(body, true, true);
		var label = new ModelLabelProvider();
		var leftText = label.getValueText(node, Side.OLD);
		var rightText = label.getValueText(node, Side.NEW);
		createText(body, leftText, rightText, Side.OLD);
		createText(body, rightText, leftText, Side.NEW);
	}

	private void createText(Composite parent, String value, String otherValue, Side side) {
		var styled = new StyledString(value);
		new DiffStyle().applyTo(styled, otherValue, side);
		var text = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		text.setText(styled.toString());
		text.setStyleRanges(styled.getStyleRanges());
		text.setEditable(false);
		text.setBackground(Colors.white());
		UI.gridData(text, true, true);
	}

}