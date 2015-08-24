package org.openlca.app.editors.processes.social;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.database.SourceDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.SocialAspect;

class Dialog extends FormDialog {

	private SocialAspect aspect;

	public static int open(SocialAspect aspect) {
		if (aspect == null || aspect.indicator == null)
			return CANCEL;
		Dialog d = new Dialog(aspect);
		return d.open();
	}

	private Dialog(SocialAspect aspect) {
		super(UI.shell());
		this.aspect = aspect;
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
			title = "#Social aspect";
		UI.formHeader(mform, title);
		Composite body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 3);
		amountRow(body, tk);
		new RiskCombo(aspect).create(body, tk);
		sourceRow(body, tk);
		commentRow(body, tk);
		qualityRow(body, tk);
	}

	private void amountRow(Composite body, FormToolkit tk) {
		Text t = UI.formText(body, tk, "#Raw amount");
		if (aspect.rawAmount != null)
			t.setText(aspect.rawAmount);
		t.addModifyListener((e) -> {
			aspect.rawAmount = t.getText();
		});
		String unit = aspect.indicator.unitOfMeasurement;
		if (unit == null)
			unit = "";
		UI.formLabel(body, tk, unit);
	}

	private void sourceRow(Composite body, FormToolkit tk) {
		TextDropComponent drop = UIFactory.createDropComponent(body,
				Messages.Source, tk, ModelType.SOURCE);
		drop.setHandler((d) -> {
			if (d == null) {
				aspect.source = null;
			} else {
				SourceDao dao = new SourceDao(Database.get());
				aspect.source = dao.getForId(d.getId());
			}
		});
		UI.formLabel(body, tk, "");
	}

	private void commentRow(Composite body, FormToolkit tk) {
		Text t = UI.formMultiText(body, tk, "#Comment");
		if (aspect.comment != null)
			t.setText(aspect.comment);
		t.addModifyListener((e) -> {
			aspect.comment = t.getText();
		});
		UI.formLabel(body, tk, "");
	}

	private void qualityRow(Composite body, FormToolkit tk) {

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
