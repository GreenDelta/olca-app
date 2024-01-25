package org.openlca.app.editors.processes.doc;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.doc.ProcessDoc;
import org.openlca.core.model.Source;
import org.openlca.core.model.doc.ComplianceDeclaration;

import java.util.List;

class ComplianceSection {

	private final ProcessEditor editor;

	private Composite parent;
	private FormToolkit tk;
	private ScrolledForm form;

	ComplianceSection(ProcessEditor editor) {
		this.editor = editor;
	}

	private List<ComplianceDeclaration> declarations() {
		var p = editor.getModel();
		if (p.documentation == null) {
			p.documentation = new ProcessDoc();
		}
		return p.documentation.complianceDeclarations;
	}

	void render(Composite body, FormToolkit tk, ScrolledForm form) {
		this.tk = tk;
		this.form = form;
		var section = UI.section(body, tk, "Compliance declarations");
		parent = UI.sectionClient(section, tk, 1);
		for (var dec : declarations()) {
			new Sec(dec);
		}
		if (editor.isEditable()) {
			var add = Actions.onAdd(this::addDeclaration);
			Actions.bind(section, add);
		}
		form.reflow(true);
	}

	private void addDeclaration() {
		var dec = new ComplianceDeclaration();
		declarations().add(dec);
		new Sec(dec);
		form.reflow(true);
		editor.setDirty();
	}

	private class Sec {

		private ComplianceDeclaration _dec;
		private final Section section;

		Sec(ComplianceDeclaration dec) {
			this._dec = dec;
			section = UI.section(parent, tk, header());
			var root = UI.sectionClient(section, tk, 1);
			renderTop(root);
			new ComplianceTable(editor, this::sync).render(root, tk);
			if (editor.isEditable()) {
				var del = Actions.onRemove(this::delete);
				Actions.bind(section, del);
			}
		}

		private void delete() {
			var dec = sync();
			declarations().remove(dec);
			section.dispose();
			form.reflow(true);
			editor.setDirty();
		}

		private String header() {
			return _dec.system != null
					? Labels.name(_dec.system)
					: "Unknown compliance system";
		}

		private void renderTop(Composite root) {
			var comp = tk.createComposite(root);
			UI.gridLayout(comp, 2, 10, 0);
			UI.fillHorizontal(comp);

			UI.label(comp, tk, "Compliance system");
			ModelLink.of(Source.class)
					.setModel(_dec.system)
					.onChange(source -> {
						sync().system = source;
						section.setText(header());
						editor.setDirty();
					})
					.setEditable(editor.isEditable())
					.renderOn(comp, tk);

			var details = UI.labeledMultiText(comp, tk, "Comment", 40);
			if (_dec.details != null) {
				details.setText(_dec.details);
			}
			details.setEditable(editor.isEditable());
			details.addModifyListener($ -> {
				sync().details = details.getText();
				editor.setDirty();
			});
		}

		/**
		 * Before editing a compliance declaration, we need to sync
		 * it with the process first, because there may was an JPA
		 * update in the mean-time.
		 */
		private ComplianceDeclaration sync() {
			for (var d : declarations()) {
				if (d == _dec || (d.id > 0 && d.id == _dec.id)) {
					_dec = d;
					return _dec;
				}
			}
			return _dec;
		}
	}
}
