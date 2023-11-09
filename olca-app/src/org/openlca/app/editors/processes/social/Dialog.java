package org.openlca.app.editors.processes.social;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.util.UI;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.Source;

class Dialog extends FormDialog {

	private final SocialAspect aspect;
	private final DQSystem system;

	public static int open(SocialAspect aspect, DQSystem system) {
		if (aspect == null || aspect.indicator == null)
			return CANCEL;
		Dialog d = new Dialog(aspect, system);
		return d.open();
	}

	private Dialog(SocialAspect aspect, DQSystem system) {
		super(UI.shell());
		this.aspect = aspect;
		this.system = system;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
			IDialogConstants.OK_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var title = aspect.indicator.name != null
				? aspect.indicator.name
				: M.SocialAspect;
		UI.header(form, title);
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 3);
		amountRow(body, tk);
		activityRow(body, tk);
		new RiskCombo(aspect).create(body, tk);
		sourceRow(body, tk);
		commentRow(body, tk);
		if (system != null)
			qualityRow(body, tk);
	}

	private void amountRow(Composite body, FormToolkit tk) {
		Text t = UI.labeledText(body, tk, M.RawValue);
		if (aspect.rawAmount != null)
			t.setText(aspect.rawAmount);
		t.addModifyListener(e -> aspect.rawAmount = t.getText());
		String unit = aspect.indicator.unitOfMeasurement;
		if (unit == null)
			unit = "";
		UI.label(body, tk, unit);
	}

	private void activityRow(Composite body, FormToolkit tk) {
		String label = M.ActivityVariable;
		if (aspect.indicator.activityVariable != null)
			label += " (" + aspect.indicator.activityVariable + ")";
		Text t = UI.labeledText(body, tk, label);
		t.setText(Double.toString(aspect.activityValue));
		t.addModifyListener(e -> {
			try {
				aspect.activityValue = Double.parseDouble(t.getText());
			} catch (Exception ignored) {
			}
		});
		String unit = "";
		if (aspect.indicator.activityUnit != null)
			unit = aspect.indicator.activityUnit.name;
		UI.label(body, tk, unit);
	}

	private void sourceRow(Composite body, FormToolkit tk) {
		ModelLink.of(Source.class)
			.setModel(aspect.source)
			.renderOn(body, tk, M.Source)
			.onChange(source -> aspect.source = source);
		UI.filler(body, tk);
	}

	private void commentRow(Composite body, FormToolkit tk) {
		Text t = UI.multiText(body, tk, M.Comment);
		if (aspect.comment != null)
			t.setText(aspect.comment);
		t.addModifyListener(e -> aspect.comment = t.getText());
		UI.filler(body, tk);
	}

	private void qualityRow(Composite body, FormToolkit tk) {
		UI.label(body, tk, M.DataQuality);
		new QualityPanel(aspect, system).create(body, tk);
		UI.filler(body, tk);
	}

	@Override
	protected Point getInitialSize() {
		int width = 1000;
		int height = 600;
		Rectangle shellBounds = getShell().getDisplay().getBounds();
		int shellWidth = shellBounds.x;
		int shellHeight = shellBounds.y;
		if (shellWidth > 0 && shellWidth < width)
			width = shellWidth;
		if (shellHeight > 0 && shellHeight < height)
			height = shellHeight;
		return new Point(width, height);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point loc = super.getInitialLocation(initialSize);
		int marginTop = (getParentShell().getSize().y - initialSize.y) / 3;
		if (marginTop < 0)
			marginTop = 0;
		return new Point(loc.x, loc.y + marginTop);
	}

}
