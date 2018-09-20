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
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.core.database.SourceDao;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.descriptors.Descriptors;

class Dialog extends FormDialog {

	private SocialAspect aspect;
	private DQSystem system;

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
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		String title = aspect.indicator.getName();
		if (title == null)
			title = M.SocialAspect;
		UI.formHeader(mform, title);
		Composite body = UI.formBody(mform.getForm(), tk);
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
		Text t = UI.formText(body, tk, M.RawValue);
		if (aspect.rawAmount != null)
			t.setText(aspect.rawAmount);
		t.addModifyListener(e -> {
			aspect.rawAmount = t.getText();
		});
		String unit = aspect.indicator.unitOfMeasurement;
		if (unit == null)
			unit = "";
		UI.formLabel(body, tk, unit);
	}

	private void activityRow(Composite body, FormToolkit tk) {
		String label = M.ActivityVariable;
		if (aspect.indicator.activityVariable != null)
			label += " (" + aspect.indicator.activityVariable + ")";
		Text t = UI.formText(body, tk, label);
		t.setText(Double.toString(aspect.activityValue));
		t.addModifyListener(e -> {
			try {
				double d = Double.parseDouble(t.getText());
				aspect.activityValue = d;
			} catch (Exception ex) {
			}
		});
		String unit = "";
		if (aspect.indicator.activityUnit != null)
			unit = aspect.indicator.activityUnit.getName();
		UI.formLabel(body, tk, unit);
	}

	private void sourceRow(Composite body, FormToolkit tk) {
		UI.formLabel(body, tk, M.Source);
		TextDropComponent drop = new TextDropComponent(body,
				tk, ModelType.SOURCE);
		UI.gridData(drop, true, false);
		if (aspect.source != null) {
			drop.setContent(Descriptors.toDescriptor(aspect.source));
		}
		drop.onChange(d -> {
			if (d == null) {
				aspect.source = null;
			} else {
				SourceDao dao = new SourceDao(Database.get());
				aspect.source = dao.getForId(d.getId());
			}
		});
		UI.filler(body, tk);
	}

	private void commentRow(Composite body, FormToolkit tk) {
		Text t = UI.formMultiText(body, tk, M.Comment);
		if (aspect.comment != null)
			t.setText(aspect.comment);
		t.addModifyListener(e -> aspect.comment = t.getText());
		UI.filler(body, tk);
	}

	private void qualityRow(Composite body, FormToolkit tk) {
		UI.formLabel(body, tk, M.DataQuality);
		new QualityPanel(aspect, system).create(body, tk);
		UI.filler(body, tk);
	}

	@Override
	protected Point getInitialSize() {
		int width = 600;
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
