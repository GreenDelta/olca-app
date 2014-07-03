package org.openlca.app.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;

public class FileSelection implements SelectionListener {

	private List<SelectionListener> externalListener = new ArrayList<>();
	private FormToolkit toolkit;
	private Text text;
	private String defaultFileName;
	private String filter;
	private File file;
	private boolean selectDirectory;

	public void addSelectionListener(SelectionListener listener) {
		externalListener.add(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		externalListener.remove(listener);
	}

	public void setSelectDirectory(boolean selectDirectory) {
		this.selectDirectory = selectDirectory;
	}

	public FileSelection(Composite parent) {
		render(parent, null);
	}

	public FileSelection(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		render(parent, null);
	}

	public FileSelection(Composite parent, FormToolkit toolkit, String label) {
		this.toolkit = toolkit;
		render(parent, label);
	}

	public void setDefaultFileName(String defaultFileName) {
		this.defaultFileName = defaultFileName;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public File getFile() {
		return file;
	}

	private void render(Composite parent, String label) {
		if (label != null)
			if (toolkit != null)
				toolkit.createLabel(parent, label, SWT.NONE);
			else
				new Label(parent, SWT.NONE);

		Composite composite = null;
		if (toolkit != null)
			composite = toolkit.createComposite(parent, SWT.NONE);
		else
			composite = new Composite(parent, SWT.NONE);

		UI.gridData(composite, true, false);
		if (toolkit != null)
			toolkit.paintBordersFor(composite);
		UI.gridLayout(composite, 2).marginTop = 0;
		if (toolkit != null)
			text = toolkit.createText(composite, null, SWT.READ_ONLY);
		else {
			text = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
			text.setBackground(new Color(Display.getCurrent(), 255, 255, 255));
		}
		UI.gridData(text, true, false);
		Button button = null;
		if (toolkit != null)
			button = toolkit.createButton(composite, Messages.Browse, SWT.NONE);
		else {
			button = new Button(composite, SWT.NONE);
			button.setText(Messages.Browse);
		}
		button.addSelectionListener(this);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		selected();
		for (SelectionListener listener : externalListener) {
			listener.widgetDefaultSelected(e);
		}
	}

	private void selected() {
		File file = null;
		if (selectDirectory) {
			file = FileChooser.forExport(FileChooser.DIRECTORY_DIALOG);
		} else {
			file = FileChooser.forExport(filter, defaultFileName);
		}
		if (file != null)
			text.setText(file.getAbsolutePath());
		else
			text.setText("");
		this.file = file;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		selected();
		for (SelectionListener listener : externalListener) {
			listener.widgetSelected(e);
		}
	}
}