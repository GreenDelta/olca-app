package org.openlca.app.util;

import java.util.function.Function;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.rcp.images.Images;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UI {

	private UI() {
	}

	/**
	 * Calls the given function when the browser has loaded the given URL. No
	 * threads are spawned here. You have to make sure that the given function
	 * accesses the browser in the UI thread.
	 */
	public static void onLoaded(Browser browser, String url, Runnable fn) {
		if (browser == null || url == null)
			return;
		browser.addProgressListener(new ProgressListener() {

			@Override
			public void completed(ProgressEvent event) {
				if (fn != null) {
					fn.run();
					browser.removeProgressListener(this);
				}
			}

			@Override
			public void changed(ProgressEvent event) {
			}
		});
		browser.setUrl(url);
	}

	/**
	 * Bind the given function with the given name to the `window` object of the
	 * given browser.
	 */
	public static void bindFunction(Browser browser, String name,
			Function<Object[], Object> fn) {
		if (browser == null || name == null || fn == null)
			return;
		BrowserFunction func = new BrowserFunction(browser, name) {
			@Override
			public Object function(Object[] args) {
				try {
					return fn.apply(args);
				} catch (Exception e) {
					Logger log = LoggerFactory.getLogger(UI.class);
					log.error("failed to execute browser function " + name, e);
					return null;
				}
			}
		};
		browser.addDisposeListener(e -> {
			if (!func.isDisposed()) {
				func.dispose();
			}
		});
	}

	public static Shell shell() {
		// first, we try to get the shell from the active workbench window
		Shell shell = null;
		try {
			IWorkbenchWindow wb = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			if (wb != null) {
				shell = wb.getShell();
			}
			if (shell != null)
				return shell;
		} catch (Exception ignored) {
		}

		// then, try to get it from the display
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		if (display != null) {
			shell = display.getActiveShell();
		}
		if (shell != null)
			return shell;
		return display != null
				? new Shell(display)
				: new Shell();
	}

	public static Point initialSizeOf(
		IShellProvider dialog, int maxWidth, int maxHeight) {
		if (dialog == null)
			return new Point(maxWidth, maxHeight);
		var displaySize = dialog.getShell().getDisplay().getBounds();
		int width = displaySize.x > 0 && displaySize.x < maxWidth
			? displaySize.x
			: maxWidth;
		int height = displaySize.y > 0 && displaySize.y < maxHeight
			? displaySize.y
			: maxHeight;
		return new Point(width, height);
	}

	public static Font boldFont() {
		return JFaceResources.getFontRegistry().getBold(
				JFaceResources.DEFAULT_FONT);
	}

	public static Font italicFont() {
		return JFaceResources.getFontRegistry().getItalic(
				JFaceResources.DEFAULT_FONT);
	}

	public static Font defaultFont() {
		return JFaceResources.getFontRegistry().defaultFont();
	}

	public static void center(Shell parent, Shell child) {
		Rectangle shellBounds = parent.getBounds();
		Point size = child.getSize();
		int diffX = (shellBounds.width - size.x) / 2;
		int diffY = (shellBounds.height - size.y) / 2;
		child.setLocation(shellBounds.x + diffX, shellBounds.y + diffY);
	}

	public static GridData gridData(Control control, boolean hFill, boolean vFill) {
		int hStyle = hFill ? SWT.FILL : SWT.LEFT;
		int vStyle = vFill ? SWT.FILL : SWT.CENTER;
		GridData data = new GridData(hStyle, vStyle, hFill, vFill);
		control.setLayoutData(data);
		return data;
	}

	public static GridData fillHorizontal(Control control) {
		return gridData(control, true, false);
	}

	public static ScrolledForm formHeader(ModelPage<?> page) {
		Image image = Images.get(page.getEditor().getModel());
		ScrolledForm form = formHeader(page.getManagedForm(), page.getFormTitle(), image);
		// "" is 'general' comment on data set
		if (page.getEditor().hasComment("")) {
			form.getToolBarManager().add(new CommentAction("", page.getEditor().getComments()));
		}
		Editors.addRefresh(form, page.getEditor());
		form.getToolBarManager().update(true);
		return form;
	}

	/** Creates a nice form header with the given title and returns the form. */
	public static ScrolledForm formHeader(IManagedForm mform, String title) {
		return formHeader(mform, title, null);
	}

	public static ScrolledForm formHeader(
			IManagedForm mform, String title, Image image) {
		var form = mform.getForm();
		var tk = mform.getToolkit();
		tk.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		if (title != null) {
			form.setText(title);
		}
		if (image != null) {
			form.setImage(image);
		}
		form.setForeground(Colors.get(38, 38, 38));
		// tk.decorateFormHeading(form.getForm());
		return form;
	}

	public static Composite formSection(
			Composite parent, FormToolkit tk, String label) {
		return formSection(parent, tk, label, 2);
	}

	public static Composite formSection(Composite parent, FormToolkit tk,
			String label, int columns) {
		Section section = section(parent, tk, label);
		return sectionClient(section, tk, columns);
	}

	public static Section section(Composite comp, FormToolkit tk, String title) {
		var s = tk.createSection(comp, ExpandableComposite.TITLE_BAR
				| ExpandableComposite.FOCUS_TITLE
				| ExpandableComposite.EXPANDED
				| ExpandableComposite.TWISTIE);
		gridData(s, true, false);
		s.setText(title);
		// s.setTitleBarBackground(Colors.get(214, 214, 255));
		s.setTitleBarBackground(Colors.white());
		s.setTitleBarBorderColor(Colors.get(122, 122, 122));
		s.setTitleBarForeground(Colors.get(38, 38, 38));
		s.setToggleColor(Colors.get(38, 38, 38));
		return s;
	}

	/**
	 * Creates a composite and sets it as section client of the given section. The
	 * created composite gets a 2-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit toolkit) {
		return sectionClient(section, toolkit, 2);
	}

	/**
	 * Creates a composite and sets it as section client of the given section. The
	 * created composite gets a n-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit toolkit, int columns) {
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		gridLayout(composite, columns);
		return composite;
	}

	public static Composite formBody(ScrolledForm form, FormToolkit tk) {
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.marginRight = 10;
		layout.marginLeft = 10;
		layout.horizontalSpacing = 10;
		layout.marginBottom = 10;
		layout.marginTop = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		body.setLayout(layout);
		tk.paintBordersFor(body);
		gridData(body, true, true);
		return body;
	}

	public static GridLayout gridLayout(Composite composite, int columns) {
		return gridLayout(composite, columns, 10, 10);
	}

	public static GridLayout gridLayout(Composite composite, int columns,
			int spacing, int margin) {
		var layout = new GridLayout(columns, false);
		layout.verticalSpacing = spacing;
		layout.marginWidth = margin;
		layout.marginHeight = margin;
		layout.horizontalSpacing = spacing;
		composite.setLayout(layout);
		return layout;
	}

	public static Composite formComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		gridLayout(composite, 2);
		return composite;
	}

	public static Composite formComposite(Composite parent, FormToolkit tk) {
		if (tk == null)
			return formComposite(parent);
		var comp = tk.createComposite(parent);
		gridLayout(comp, 2);
		return comp;
	}

	/** Creates a simple check box with the given text. */
	public static Button checkBox(Composite parent, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);
		return button;
	}

	/** Creates a label and check box as two separate components. */
	public static Button formCheckBox(Composite parent, String label) {
		return formCheckBox(parent, null, label);
	}

	/** Creates a label and check box as two separate components. */
	public static Button formCheckBox(Composite parent, FormToolkit toolkit, String label) {
		formLabel(parent, toolkit, label);
		return formCheckbox(parent, toolkit);
	}

	public static Button formCheckbox(Composite parent, FormToolkit toolkit) {
		var button = toolkit != null
			? toolkit.createButton(parent, null, SWT.CHECK)
			: new Button(parent, SWT.CHECK);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		return button;
	}

	public static Button formRadio(Composite parent, String label) {
		Button button = new Button(parent, SWT.RADIO);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		formLabel(parent, label);
		return button;
	}

	public static Text formText(Composite parent, int flags) {
		return formText(parent, null, null, flags | SWT.BORDER);
	}

	public static Text formText(Composite parent, String label) {
		return formText(parent, null, label);
	}

	public static Text formText(Composite parent, String label, int flags) {
		return formText(parent, null, label, flags | SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit toolkit,
			String label) {
		return formText(parent, toolkit, label, SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit toolkit,
			String label, int flags) {
		if (label != null)
			formLabel(parent, toolkit, label);
		Text text = toolkit != null
			? toolkit.createText(parent, null, flags)
			: new Text(parent, flags);
		gridData(text, true, false);
		return text;
	}

	public static Text formMultiText(Composite parent, String label) {
		return formMultiText(parent, null, label);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk, String label) {
		return formMultiText(comp, tk, label, 100);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk, String label, int heightHint) {
		formLabel(comp, tk, label);
		return formMultiText(comp, tk, heightHint);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk, int heightHint) {
		Text text = tk != null
			? tk.createText(comp, null, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI)
			: new Text(comp, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		GridData gd = gridData(text, true, false);
		gd.minimumHeight = heightHint;
		gd.heightHint = heightHint;
		return text;
	}

	public static Combo formCombo(Composite comp, String label) {
		return formCombo(comp, null, label);
	}

	public static Combo formCombo(Composite comp, FormToolkit tk, String label) {
		formLabel(comp, tk, label);
		Combo combo = new Combo(comp, SWT.READ_ONLY);
		gridData(combo, true, false);
		return combo;
	}

	public static Label formLabel(Composite comp, String text) {
		return formLabel(comp, null, text);
	}

	public static Label formLabel(Composite comp, FormToolkit tk, String text) {
		Label label;
		if (tk != null) {
			label = tk.createLabel(comp, text, SWT.NONE);
		} else {
			label = new Label(comp, SWT.NONE);
			label.setText(text);
		}
		GridData gd = gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		return label;
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static Label filler(Composite comp, FormToolkit tk) {
		return formLabel(comp, tk, "");
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static Label filler(Composite comp) {
		return formLabel(comp, null, "");
	}

	public static Hyperlink formLink(Composite comp, FormToolkit tk, String text) {
		Hyperlink link;
		if (tk != null)
			link = tk.createHyperlink(comp, text, SWT.NONE);
		else {
			link = new Hyperlink(comp, SWT.NONE);
			link.setText(text);
		}
		GridData gd = gridData(link, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		return link;
	}

}
