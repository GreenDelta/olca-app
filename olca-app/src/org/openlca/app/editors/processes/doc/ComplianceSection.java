package org.openlca.app.editors.processes.doc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Source;
import org.openlca.core.model.doc.ComplianceDeclaration;
import org.openlca.core.model.doc.ProcessDoc;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

class ComplianceSection {

	private final ProcessEditor editor;
	private final List<Sec> secs = new ArrayList<>();

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
		var decs = declarations();
		for (int pos = 0; pos < decs.size(); pos++) {
			secs.add(new Sec(pos, decs.get(pos)));
		}
		if (editor.isEditable()) {
			var add = Actions.onAdd(this::addDeclaration);
			Actions.bind(section, add);
		}
		form.reflow(true);
	}

	private void addDeclaration() {
		var decs = declarations();
		var pos = decs.size();
		var dec = new ComplianceDeclaration();
		decs.add(dec);
		secs.add(new Sec(pos, dec));
		form.reflow(true);
		editor.setDirty();
	}

	private class Sec {

		private int pos;
		private ComplianceDeclaration _dec;
		private final Section section;

		Sec(int pos, ComplianceDeclaration dec) {
			this.pos = pos;
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
			declarations().remove(pos);
			secs.remove(pos);
			for (var sec : secs) {
				if (sec.pos > pos) {
					sec.pos--;
					sec.section.setText(sec.header());
				}
			}
			section.dispose();
			form.reflow(true);
			editor.setDirty();
		}

		private String header() {
			var h = "Compliance system #" + (pos + 1);
			if (_dec.system != null) {
				var name = Labels.name(_dec.system);
				if (Strings.notEmpty(name)) {
					h += " - " + name;
				}
			}
			return h;
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
			if (_dec.comment != null) {
				details.setText(_dec.comment);
			}
			details.setEditable(editor.isEditable());
			details.addModifyListener($ -> {
				sync().comment = details.getText();
				editor.setDirty();
			});
		}

		/**
		 * Before editing a compliance declaration, we need to sync
		 * it with the process first, because there may was an JPA
		 * update in the mean-time.
		 */
		private ComplianceDeclaration sync() {
			var decs = declarations();
			if (pos < decs.size()) {
				_dec = decs.get(pos);
				return _dec;
			}
			LoggerFactory.getLogger(getClass())
				.error("could not sync declaration at pos={}", pos);
			return _dec;
		}
	}
}
