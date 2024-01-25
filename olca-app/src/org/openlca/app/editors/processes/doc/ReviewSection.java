package org.openlca.app.editors.processes.doc;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.ModelLink;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.doc.ProcessDoc;
import org.openlca.core.model.Source;
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

		if (editor.isEditable()) {
			var add = Actions.onAdd(this::addReview);
			Actions.bind(section, add);
		}
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
			var root = UI.sectionClient(section, tk, 1);
			renderTop(root);
			reviewerTable(root);
			new ReviewScopeSection(editor, this::sync)
					.render(root, tk);
			if (editor.isEditable()) {
				var del = Actions.onRemove(this::delete);
				Actions.bind(section, del);
			}
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
				var r = _rev.reviewers.get(i);
				buff.append(i == 0 ? ": " : "; ");
				var name = Labels.name(r);
				buff.append(Strings.notEmpty(name) ? name : "?");
			}
			return Strings.cut(buff.toString(), 100);
		}

		private void renderTop(Composite root) {
			var comp = tk.createComposite(root);
			UI.gridLayout(comp, 2, 10, 0);
			UI.fillHorizontal(comp);
			typeCombo(comp);

			UI.label(comp, tk, "Review report");
			ModelLink.of(Source.class)
					.setModel(_rev.report)
					.onChange(source -> {
						sync().report = source;
						editor.setDirty();
					})
					.setEditable(editor.isEditable())
					.renderOn(comp, tk);

			var details = UI.labeledMultiText(comp, tk, "Review details", 40);
			if (_rev.details != null) {
				details.setText(_rev.details);
			}
			details.setEditable(editor.isEditable());
			details.addModifyListener($ -> {
				sync().details = details.getText();
				editor.setDirty();
			});
		}

		private void typeCombo(Composite comp) {
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

			if (!editor.isEditable()) {
				combo.setEnabled(false);
				return;
			}

			Controls.onSelect(combo, $ -> {
				var rev = sync();
				int i = combo.getSelectionIndex();
				rev.type = items.get(i);
				section.setText(header());
				editor.setDirty();
			});
		}

		private void reviewerTable(Composite root) {
			var section = UI.section(root, tk, "Reviewers");
			var comp = UI.sectionClient(section, tk, 1);
			var table = Tables.createViewer(comp, "Reviewer");
			Tables.bindColumnWidths(table, 1.0);
			table.setLabelProvider(new ActorTableLabel());
			table.setInput(_rev.reviewers);
			if (!editor.isEditable())
				return;

			var add = Actions.onAdd(() -> {
				var ds = ModelSelector.multiSelect(ModelType.ACTOR);
				if (ds.isEmpty())
					return;
				var db = Database.get();
				var rev = sync();
				for (var d : ds) {
					var actor = db.get(Actor.class, d.id);
					if (actor == null)
						continue;
					if (!rev.reviewers.contains(actor)) {
						rev.reviewers.add(actor);
					}
				}
				table.setInput(rev.reviewers);
				Sec.this.section.setText(header());
				editor.setDirty();
			});

			var del = Actions.onRemove(() -> {
				var obj = Viewers.getFirstSelected(table);
				if (!(obj instanceof Actor actor))
					return;
				var rev = sync();
				rev.reviewers.remove(actor);
				table.setInput(rev.reviewers);
				editor.setDirty();
			});

			Actions.bind(section, add, del);
			Actions.bind(table, add, del);
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

	private static class ActorTableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return obj instanceof Actor actor
					? Images.get(actor)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof Actor actor
					? Labels.name(actor)
					: null;
		}
	}
}
