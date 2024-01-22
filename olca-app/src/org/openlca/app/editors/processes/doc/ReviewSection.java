package org.openlca.app.editors.processes.doc;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProcessDoc;
import org.openlca.core.model.doc.Review;
import org.openlca.ilcd.commons.ReviewType;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ReviewSection {

	private final ProcessEditor editor;

	private Composite parent;
	private FormToolkit tk;
	private ScrolledForm form;

	ReviewSection(ProcessEditor editor) {
		this.editor = editor;
	}

	private List<Review> reviews() {
		var p = editor.getModel();
		if (p.documentation == null) {
			p.documentation = new ProcessDoc();
		}
		return p.documentation.reviews;
	}

	void render(Composite body, FormToolkit tk, ScrolledForm form) {
		this.tk = tk;
		this.form = form;
		var section = UI.section(body, tk, "Reviews");
		parent = UI.sectionClient(section, tk, 1);
		for (var review : reviews()) {
			new Sec(review);
		}

		var add = Actions.onAdd(this::addReview);
		Actions.bind(section, add);
		form.reflow(true);
	}

	private void addReview() {
		var review = new Review();
		reviews().add(review);
		new Sec(review);
		form.reflow(true);
		editor.setDirty();
	}

	private class Sec {

		private Review _rev;
		private final Section section;

		Sec(Review review) {
			this._rev = review;
			section = UI.section(parent, tk, header());
			var comp = UI.sectionClient(section, tk, 1);
			typeCombo(comp);
			var del = Actions.onRemove(this::delete);
			Actions.bind(section, del);
		}

		private void delete() {
			var rev = sync();
			reviews().remove(rev);
			section.dispose();
			form.reflow(true);
			editor.setDirty();
		}

		private String header() {
			var type = Strings.notEmpty(_rev.type)
					? _rev.type
					: "Unknown review";
			if (_rev.reviewers.isEmpty())
				return type;
			var buff = new StringBuilder(type);
			for (int i = 0; i < _rev.reviewers.size(); i++) {
				var r = _rev.reviewers.get(0);
				buff.append(i == 0 ? ": " : "; ");
				var name = Labels.name(r);
				buff.append(Strings.notEmpty(name) ? name : "?");
			}
			return Strings.cut(buff.toString(), 50);
		}

		private void typeCombo(Composite parent) {
			var comp = tk.createComposite(parent);
			UI.gridLayout(comp, 2);
			var combo = UI.labeledCombo(comp, tk, "Review type");
			var values = ReviewType.values();
			var items = new ArrayList<String>(1 + values.length);
			items.add("");
			int selected = 0;
			for (int i = 0; i < values.length; i++) {
				var value = values[i].value();
				items.add(value);
				if (Objects.equals(value, _rev.type)) {
					selected = i + 1;
				}
			}
			if (Strings.notEmpty(_rev.type) && !items.contains(_rev.type)) {
				selected = items.size();
				items.add(_rev.type);
			}
			combo.setItems(items.toArray(String[]::new));
			combo.select(selected);

			Controls.onSelect(combo, $ -> {
				int i = combo.getSelectionIndex();
				sync().type = items.get(i);
				editor.setDirty();
			});
		}

		/**
		 * Before editing a review, we always need to sync it with
		 * the process first, because there may was a JPA update
		 * in the mean-time.
		 */
		private Review sync() {
			for (var r : reviews()) {
				if (r == _rev || (r.id > 0 && r.id == _rev.id)) {
					_rev = r;
					return _rev;
				}
			}
			return _rev;
		}

	}

}
