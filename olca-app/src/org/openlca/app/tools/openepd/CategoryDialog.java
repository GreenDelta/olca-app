package org.openlca.app.tools.openepd;

import java.util.Arrays;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.io.openepd.Ec3Category;
import org.openlca.io.openepd.Ec3CategoryTree;
import org.openlca.util.Strings;

public class CategoryDialog extends FormDialog {

	private final Ec3CategoryTree categories;
	private Ec3Category selected;

	public static Ec3Category selectFrom(Ec3CategoryTree tree) {
		if (tree == null || tree.isEmpty())
			return null;
		var dialog = new CategoryDialog(tree);
		return dialog.open() == Window.OK
			? dialog.selected
			: null;
	}

	private CategoryDialog(Ec3CategoryTree categories) {
		super(UI.shell());
		this.categories = categories;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select a category");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 500);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		var tree = new TreeViewer(body, SWT.SINGLE | SWT.BORDER);
		UI.gridData(tree.getControl(), true, true);
		tree.setAutoExpandLevel(2);
		tree.setContentProvider(new ContentProvider());
		ColumnViewerToolTipSupport.enableFor(tree);
		tree.setLabelProvider(new LabelProvider());
		tree.addSelectionChangedListener($ -> {
			Ec3Category category = Viewers.getFirstSelected(tree);
			if (category != null) {
				selected = category;
			}
		});
		Controls.onPainted(tree.getControl(),() -> tree.setInput(categories));
	}

	private static class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object input) {
			if (!(input instanceof Ec3CategoryTree tree))
				return null;
			return tree.isEmpty()
				? null
				: new Object[]{tree.root()};
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof Ec3Category category))
				return null;
			var subs = category.subCategories.toArray(new Ec3Category[0]);
			Arrays.sort(subs, (c1, c2) -> Strings.compare(c1.name, c2.name));
			return subs;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof Ec3Category category
				&& !category.subCategories.isEmpty();
		}
	}

	private static class LabelProvider extends ColumnLabelProvider {

		@Override
		public Image getImage(Object obj) {
			return Icon.FOLDER.get();
		}

		@Override
		public String getText(Object obj) {
			return obj instanceof Ec3Category category
				? category.name
				: null;
		}
	}

}
