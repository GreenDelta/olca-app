package org.openlca.app.editors.sd;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.sd.xmile.Xmile;

class SdInfoPage extends FormPage {

	private final SdModelEditor editor;

	SdInfoPage(SdModelEditor editor) {
		super(editor, "SdModelInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		infoSection(body, tk);
		imageSection(body, tk);
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 3);

		var nameText = UI.labeledText(comp, tk, M.Name);
		nameText.setEditable(false);
		nameText.setText(editor.modelName());
		UI.filler(comp, tk);

		var specs = SimSpecs.of(editor.xmile());

		var methodText = UI.labeledText(comp, tk, "Solver method");
		methodText.setEditable(false);
		methodText.setText(specs.method);
		UI.filler(comp, tk);

		var startText = UI.labeledText(comp, tk, "Start time");
		startText.setEditable(false);
		startText.setText(Double.toString(specs.start));
		UI.label(comp, tk, specs.timeUnit);

		var endText = UI.labeledText(comp, tk, "Stop time");
		endText.setEditable(false);
		endText.setText(Double.toString(specs.stop));
		UI.label(comp, tk, specs.timeUnit);

		var dtText = UI.labeledText(comp, tk, "Δt");
		dtText.setEditable(false);
		dtText.setText(Double.toString(specs.dt));
		UI.label(comp, tk, specs.timeUnit);

		UI.filler(comp, tk);
		var btn = UI.button(comp, tk, "Run simulation");
		btn.setImage(Icon.RUN.get());
	}

	private void imageSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Model graph");
	}

	private record SimSpecs(
			double start,
			double stop,
			double dt,
			String timeUnit,
			String method
	) {

		static SimSpecs of(Xmile xmile) {
			if (xmile == null || xmile.simSpecs() == null)
				return new SimSpecs(0, 0, 0, "", "");
			var specs = xmile.simSpecs();
			double dt = 1;
			if (specs.dt() != null && specs.dt().value() != null) {
				dt = specs.dt().reciprocal() != null && specs.dt().reciprocal()
						? 1 / specs.dt().value()
						: specs.dt().value();
			}

			return new SimSpecs(
					specs.start() != null ? specs.start() : 0,
					specs.stop() != null ? specs.stop() : 0,
					dt,
					specs.timeUnits() != null ? specs.timeUnits() : "",
					specs.method() != null ? specs.method() : ""
			);
		}
	}

}
