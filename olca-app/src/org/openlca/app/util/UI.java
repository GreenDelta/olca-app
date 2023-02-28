package org.openlca.app.util;

import java.util.function.Function;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.rcp.images.Images;
import org.openlca.util.Strings;
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
		var data = new GridData(hStyle, vStyle, hFill, vFill);
		if (hFill && (control instanceof Text
				|| control instanceof Table
				|| control instanceof Tree)) {
			// fix for this bug:
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=215997#c4
			data.widthHint = 1;
		}
		control.setLayoutData(data);
		return data;
	}

	public static GridData fillHorizontal(Control control) {
		return gridData(control, true, false);
	}

	public static ScrolledForm formHeader(ModelPage<?> page) {
		var image = Images.get(page.getEditor().getModel());
		var form = formHeader(page.getManagedForm(), page.getFormTitle(), image);
		// "" is 'general' comment on data set
		if (page.getEditor().hasComment("")) {
			form.getToolBarManager()
					.add(new CommentAction("", page.getEditor().getComments()));
		}
		Editors.addRefresh(form, page.getEditor());

		form.setBackground(Colors.widgetBackground());
		form.setForeground(Colors.widgetForeground());

		form.getToolBarManager().update(true);
		return form;
	}

	/**
	 * Creates a nice form header with the given title and returns the form.
	 */
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
		form.setForeground(Colors.widgetForeground());
		form.setBackground(Colors.widgetBackground());
		// tk.decorateFormHeading(form.getForm());
		return form;
	}

	public static List formList(Composite comp) {
		var list = new org.eclipse.swt.widgets.List(comp, SWT.BORDER);
		list.setBackground(Colors.widgetBackground());
		list.setForeground(Colors.widgetForeground());
		return list;
	}

	public static Composite formWizardHeader(IManagedForm mform, FormToolkit toolkit, String title,
																					 String description) {
		var form = UI.formHeader(mform, title);
		var body = form.getBody();
		UI.gridLayout(body, 1, 0, 0);
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, false);
		var descriptionComposite = toolkit.createComposite(body);
		descriptionComposite.setForeground(Colors.widgetForeground());
		descriptionComposite.setBackground(Colors.widgetBackground());
		UI.gridLayout(descriptionComposite, 1).marginTop = 0;
		UI.gridData(descriptionComposite, true, false);
		UI.formLabel(descriptionComposite, toolkit, description);
		var separator = new Label(body, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return body;
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
		var s = tk.createSection(comp,
				ExpandableComposite.SHORT_TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		gridData(s, true, false);
		s.setText(title);

		s.setTitleBarBackground(Colors.titleBackground());
		s.setTitleBarBorderColor(Colors.widgetBorder());
		s.setTitleBarForeground(Colors.titleForeground());
		s.setToggleColor(Colors.widgetToggle());

		s.setBackground(Colors.widgetBackground());
		s.setForeground(Colors.widgetForeground());
		return s;
	}

	/**
	 * Creates a composite and sets it as section client of the given section.
	 * The created composite gets a 2-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit toolkit) {
		return sectionClient(section, toolkit, 2);
	}

	/**
	 * Creates a composite and sets it as section client of the given section.
	 * The created composite gets a n-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit tk, int columns) {
		var composite = UI.formComposite(section, tk);
		section.setClient(composite);
		gridLayout(composite, columns);

		composite.setBackground(Colors.widgetBackground());
		composite.setForeground(Colors.widgetForeground());

		return composite;
	}

	public static void bodyLayout(Composite comp, FormToolkit tk) {
		GridLayout layout = new GridLayout();
		layout.marginRight = 10;
		layout.marginLeft = 10;
		layout.horizontalSpacing = 10;
		layout.marginBottom = 10;
		layout.marginTop = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		comp.setLayout(layout);
		tk.paintBordersFor(comp);
		gridData(comp, true, true);
	}

	public static Composite formBody(ScrolledForm form, FormToolkit tk) {
		Composite body = form.getBody();
		bodyLayout(body, tk);

		body.setBackground(Colors.widgetBackground());
		body.setForeground(Colors.widgetForeground());

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
		return formComposite(parent, SWT.NONE);
	}

	public static Composite formComposite(Composite parent, int style) {
		Composite composite = new Composite(parent, style);
		gridLayout(composite, 2);
		composite.setBackground(Colors.widgetBackground());
		composite.setForeground(Colors.widgetBackground());
		return composite;
	}

	public static Composite formComposite(Composite parent, FormToolkit tk, int style) {
		if (tk == null)
			return formComposite(parent, style);
		var comp = tk.createComposite(parent, style);
		gridLayout(comp, 2);
		comp.setBackground(Colors.widgetBackground());
		comp.setForeground(Colors.widgetBackground());
		return comp;
	}

	public static Composite formComposite(Composite parent, FormToolkit tk) {
		return formComposite(parent, tk, SWT.NONE);
	}

	public static Button formButton(Composite comp, FormToolkit tk, String text) {
		return formButton(comp, tk, text, SWT.NONE);
	}

	public static Button formButton(Composite comp, FormToolkit tk, String text, int style) {
		var button = tk.createButton(comp, text, style);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	public static Button button(Composite comp, int style) {
		var button = new Button(comp, style);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	/**
	 * Creates a simple check box with the given text.
	 */
	public static Button checkBox(Composite comp, FormToolkit tk, String text) {
		Button button = tk == null
				? new Button(comp, SWT.CHECK)
				: tk.createButton(comp, text, SWT.CHECK);
		button.setText(text);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	public static Button checkBox(Composite comp, String text) {
		return checkBox(comp, null, text);
	}

	/**
	 * Creates a label and check box as two separate components.
	 */
	public static Button formCheckBox(Composite parent, String label) {
		return formCheckBox(parent, null, label);
	}

	/**
	 * Creates a label and check box as two separate components.
	 */
	public static Button formCheckBox(Composite parent, FormToolkit toolkit, String label) {
		formLabel(parent, toolkit, label);
		return formCheckBox(parent, toolkit);
	}

	public static Button formCheckBox(Composite parent, FormToolkit toolkit) {
		var button = toolkit != null
				? toolkit.createButton(parent, null, SWT.CHECK)
				: new Button(parent, SWT.CHECK);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	/**
	 * Create a radio button with a label.
	 */
	public static Button formRadio(Composite parent, String label) {
		var button = new Button(parent, SWT.RADIO);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		formLabel(parent, label);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	/**
	 * Create a radio button.
	 */
	public static Button formRadio(Composite parent) {
		var button = new Button(parent, SWT.RADIO);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	/**
	 * Create a radio button.
	 */
	public static Button formRadio(Composite parent, FormToolkit tk, String label) {
		var button = tk.createButton(parent, label, SWT.RADIO);
		button.setBackground(Colors.widgetBackground());
		button.setForeground(Colors.widgetForeground());
		return button;
	}

	public static Scale scale(Composite comp) {
		var scale = new Scale(comp, SWT.NONE);
		scale.setBackground(Colors.widgetBackground());
		scale.setForeground(Colors.widgetForeground());
		return scale;
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


	public static Text formEmptyText(Composite parent, FormToolkit toolkit) {
		return formEmptyText(parent, toolkit, SWT.NONE);
	}

	public static Text formEmptyText(Composite parent, int style) {
		return formEmptyText(parent, null, style);
	}

	public static Text formEmptyText(Composite parent, FormToolkit toolkit, int style) {
		var text = toolkit == null
				? new Text(parent, style)
				: UI.formText(parent, toolkit, "", style);
		text.setBackground(Colors.widgetBackground());
		text.setForeground(Colors.widgetForeground());
		return text;
	}

	public static Text formText(Composite parent, FormToolkit tk,
															String label, int flags) {
		if (label != null)
			formLabel(parent, tk, label);
		Text text = tk != null
				? tk.createText(parent, null, flags)
				: new Text(parent, flags);
		fillHorizontal(text);
		text.setBackground(Colors.widgetBackground());
		text.setForeground(Colors.widgetForeground());
		return text;
	}

	public static FormText formText(Composite parent, FormToolkit tk, boolean trackFocus) {
		var text = tk.createFormText(parent, trackFocus);
		text.setBackground(Colors.widgetBackground());
		text.setForeground(Colors.widgetForeground());
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
		GridData gd = fillHorizontal(text);
		gd.minimumHeight = heightHint;
		gd.heightHint = heightHint;

		text.setBackground(Colors.widgetBackground());
		text.setForeground(Colors.widgetForeground());

		return text;
	}

	public static Combo formCombo(Composite comp, String label) {
		return formCombo(comp, null, label);
	}

	public static Combo formCombo(Composite comp, FormToolkit tk, String label) {
		formLabel(comp, tk, label);
		var combo = new Combo(comp, SWT.READ_ONLY);
		combo.setBackground(Colors.widgetBackground());
		combo.setForeground(Colors.widgetForeground());
		gridData(combo, true, false);
		return combo;
	}

	public static TableCombo formTableCombo(Composite comp, FormToolkit tk, int style) {
		var combo = new TableCombo(comp, style);

		if (tk != null)
			tk.adapt(combo);
		combo.setBackground(Colors.widgetBackground());
		combo.setForeground(Colors.widgetForeground());

		return combo;
	}

	public static Label formLabel(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setBackground(Colors.widgetBackground());
		label.setForeground(Colors.widgetForeground());
		return label;
	}

	public static Label formLabel(Composite comp, String text) {
		return formLabel(comp, null, text);
	}

	public static Label formLabel(Composite comp, FormToolkit tk, String text) {
		return formLabel(comp, tk, text, SWT.NONE);
	}

	public static Label formLabel(Composite comp, FormToolkit tk, String text, int flag) {
		Label label;
		var s = text != null ? text : "";
		if (tk != null) {
			label = tk.createLabel(comp, s, flag);
		} else {
			label = new Label(comp, flag);
			label.setText(s);
		}
		var gd = gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		label.setBackground(Colors.widgetBackground());
		label.setForeground(Colors.widgetForeground());

		return label;
	}

	public static CLabel formCLabel(Composite parent, FormToolkit tk, int style) {
		var cLabel = new CLabel(parent, style);
		tk.adapt(cLabel);
		cLabel.setBackground(Colors.widgetBackground());
		cLabel.setForeground(Colors.widgetForeground());
		return cLabel;
	}


	public static CLabel formCLabel(Composite parent, FormToolkit tk) {
		return formCLabel(parent, tk, SWT.NONE);
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

		link.setBackground(Colors.widgetBackground());
		return link;
	}

	public static ImageHyperlink formCategoryLink(Composite comp, FormToolkit tk, String category,
																								Image image) {
		var link = tk.createImageHyperlink(comp, SWT.NONE);
		link.setImage(image);
		link.setText(Strings.cutMid(category, 100));
		link.setToolTipText(category);
		link.setBackground(Colors.widgetBackground());
		return link;
	}

	public static ImageHyperlink formImageHyperlink(Composite comp, FormToolkit tk,
																									int style) {
		var link = tk.createImageHyperlink(comp, style);
		link.setForeground(Colors.linkBlue());
		link.setBackground(Colors.widgetBackground());
		return link;
	}

	public static ImageHyperlink formImageHyperlink(Composite comp, FormToolkit tk) {
		return formImageHyperlink(comp, tk, SWT.NONE);
	}

	public static Spinner formSpinner(Composite comp, FormToolkit tk, int style) {
		var spinner = new Spinner(comp, style);
		tk.adapt(spinner);
		return spinner;
	}

	public static Group formGroup(Composite comp, FormToolkit tk, int style) {
		var group = new Group(comp, style);
		if (tk != null)
			tk.adapt(group);
		group.setBackground(Colors.widgetBackground());
		group.setForeground(Colors.widgetForeground());
		return group;
	}

	public static Group formGroup(Composite comp, FormToolkit tk) {
		return formGroup(comp, tk, SWT.NONE);
	}

	public static Group formGroup(Composite comp) {
		return formGroup(comp, null, SWT.NONE);
	}

	public static Hyperlink formHyperlink(Composite comp, FormToolkit tk) {
		var link = new Hyperlink(comp, SWT.NONE);
		if (tk != null)
			tk.adapt(link);
		link.setBackground(Colors.widgetBackground());
		link.setForeground(Colors.widgetForeground());
		return link;
	}

	public static Hyperlink formHyperlink(Composite comp) {
		return formHyperlink(comp, null);
	}

	public static StyledText formStyledText(Composite comp, FormToolkit tk) {
		var text = new StyledText(comp, SWT.BORDER);
		tk.adapt(text);
		text.setBackground(Colors.widgetBackground());
		text.setForeground(Colors.widgetForeground());
		return text;
	}

}
