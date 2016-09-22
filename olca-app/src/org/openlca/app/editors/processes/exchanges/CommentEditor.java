package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.core.model.Exchange;

class CommentEditor extends DialogCellEditor {

	private ProcessEditor editor;
	private Exchange exchange;
	private String oldValue;

	CommentEditor(TableViewer viewer, ProcessEditor editor) {
		super(viewer.getTable());
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof Exchange) {
			exchange = (Exchange) value;
			oldValue = exchange.description;
			super.doSetValue(oldValue);
		} else {
			exchange = null;
			oldValue = null;
		}
	}

	@Override
	protected Object openDialogBox(Control control) {
		Box box = new Box(control.getShell(), oldValue);
		if (box.open() != Dialog.OK)
			return null;
		String newValue = box.value;
		if (Objects.equals(oldValue, newValue))
			return null;
		exchange.description = newValue;
		updateContents(newValue);
		editor.setDirty(true);
		return null;
	}

	private class Box extends Dialog {

		private String value;

		Box(Shell shell, String value) {
			super(shell);
			this.value = value;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite c = (Composite) super.createDialogArea(parent);
			Shell shell = getShell();
			if (shell != null) {
				shell.setText(M.Comment);
			}
			FillLayout layout = new FillLayout();
			layout.marginHeight = 20;
			layout.marginWidth = 20;
			c.setLayout(layout);
			Text t = new Text(c, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
			if (value != null) {
				t.setText(value);
			}
			t.addModifyListener(e -> {
				value = t.getText();
			});
			return c;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 400);
		}
	}
}
