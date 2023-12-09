package org.openlca.app.util;

import static org.openlca.util.OS.WINDOWS;

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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.rcp.images.Images;
import org.openlca.util.Strings;
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
		new BrowserFunction(browser, name) {
			@Override
			public Object function(Object[] args) {
				try {
					return fn.apply(args);
				} catch (Exception e) {
					var log = LoggerFactory.getLogger(UI.class);
					log.error("failed to execute browser function " + name, e);
					return null;
				}
			}
		};
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

	public static void center(Shell comp, Shell child) {
		Rectangle shellBounds = comp.getBounds();
		Point size = child.getSize();
		int diffX = (shellBounds.width - size.x) / 2;
		int diffY = (shellBounds.height - size.y) / 2;
		child.setLocation(shellBounds.x + diffX, shellBounds.y + diffY);
	}

	public static GridData gridData(Control control, boolean hFill,
			boolean vFill) {
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

	public static ScrolledForm header(ModelPage<?> page) {
		var image = Images.get(page.getEditor().getModel());
		var form = header(page.getManagedForm(), page.getFormTitle(), image);
		// "" is 'general' comment on data set
		if (page.getEditor().hasComment("")) {
			form.getToolBarManager()
					.add(new CommentAction("", page.getEditor().getComments()));
		}
		Editors.addRefresh(form, page.getEditor());

		form.getToolBarManager().update(true);
		return form;
	}

	/**
	 * Creates a nice form header with the given title and returns the form.
	 */
	public static ScrolledForm header(IManagedForm mform, String title) {
		return header(mform, title, null);
	}

	public static ScrolledForm header(IManagedForm mform, String title,
			Image image) {
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
		return form;
	}

	public static List list(Composite parent) {
		return new List(parent, SWT.BORDER);
	}

	public static Composite header(IManagedForm mform, FormToolkit tk,
			String title, String description) {
		var form = UI.header(mform, title);
		var body = form.getBody();
		gridLayout(body, 1, 0, 0);
		tk.paintBordersFor(body);
		gridData(body, true, false);
		var descriptionComposite = tk.createComposite(body);
		gridLayout(descriptionComposite, 1).marginTop = 0;
		gridData(descriptionComposite, true, false);
		label(descriptionComposite, tk, description);
		var separator = label(body, tk, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return body;
	}

	/**
	 * Create a two columns section with a title.
	 */
	public static Composite formSection(Composite parent, FormToolkit tk,
			String label) {
		return formSection(parent, tk, label, 2);
	}

	/**
	 * Create a section with a title.
	 */
	public static Composite formSection(Composite parent, FormToolkit tk,
			String label, int columns) {
		Section section = section(parent, tk, label);
		return sectionClient(section, tk, columns);
	}

	public static Section section(Composite parent, FormToolkit tk, String title) {
		var s = tk.createSection(parent,
				ExpandableComposite.SHORT_TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		gridData(s, true, false);
		s.setText(title);

		if (org.openlca.util.OS.get() == WINDOWS) {
			s.setTitleBarBackground(Colors.white());
			s.setTitleBarBorderColor(Colors.get(122, 122, 122));
			s.setTitleBarForeground(Colors.get(38, 38, 38));
			s.setToggleColor(Colors.get(38, 38, 38));
		}

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
	public static Composite sectionClient(Section section, FormToolkit tk,
			int columns) {
		var composite = UI.composite(section, tk);
		section.setClient(composite);
		gridLayout(composite, columns);

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

	public static Composite body(ScrolledForm form, FormToolkit tk) {
		Composite body = form.getBody();
		bodyLayout(body, tk);
		return body;
	}

	/**
	 * This method is currently used to fix the white backgrounds in FormDialog
	 * instances in dark-mode on Windows.
	 */
	public static Composite dialogBody(ScrolledForm form, FormToolkit tk) {
		var body = form.getBody();
		body.setLayout(new FillLayout());
		var comp = tk.createComposite(body);
		bodyLayout(comp, tk);
		return comp;
	}

	public static GridLayout gridLayout(Composite comp, int columns) {
		return gridLayout(comp, columns, 10, 10);
	}

	public static GridLayout gridLayout(Composite parent, int columns,
			int spacing, int margin) {
		var layout = new GridLayout(columns, false);
		layout.verticalSpacing = spacing;
		layout.marginWidth = margin;
		layout.marginHeight = margin;
		layout.horizontalSpacing = spacing;
		parent.setLayout(layout);
		return layout;
	}

	/**
	 * Creates a composite.
	 */
	public static Composite composite(Composite parent) {
		return composite(parent, null, SWT.NONE);
	}

	/**
	 * Creates a composite.
	 */
	public static Composite composite(Composite parent, FormToolkit tk) {
		return composite(parent, tk, SWT.NONE);
	}

	/**
	 * Creates a composite.
	 */
	public static Composite composite(Composite parent, int style) {
		return composite(parent, null, style);
	}

	/**
	 * Creates a composite.
	 */
	public static Composite composite(Composite parent, FormToolkit tk,
			int style) {
		return tk == null
				? new Composite(parent, style)
				: tk.createComposite(parent, style);
	}

	public static Button button(Composite comp, FormToolkit tk, String text) {
		return button(comp, tk, text, SWT.NONE);
	}

	public static Button button(Composite comp) {
		return button(comp, null, null, SWT.NONE);
	}

	public static Button button(Composite comp, FormToolkit tk) {
		return button(comp, tk, null, SWT.NONE);
	}

	public static Button button(Composite comp, String text) {
		return button(comp, null, text, SWT.NONE);
	}

	public static Button button(Composite comp, FormToolkit tk, String text,
			int style) {
		return tk != null
				? tk.createButton(comp, text, style)
				: new Button(comp, style);
	}

	public static Button checkbox(Composite comp, String text) {
		return checkbox(comp, null, text);
	}

	/**
	 * Creates a checkbox without label.
	 */
	public static Button checkbox(Composite comp, FormToolkit tk) {
		return checkbox(comp, tk, null);
	}

	/**
	 * Creates a simple check box.
	 */
	public static Button checkbox(Composite comp, FormToolkit tk, String text) {
		var button = tk == null
				? new Button(comp, SWT.CHECK)
				: tk.createButton(comp, text, SWT.CHECK);
		if (text != null) {
			button.setText(text);
		}
		return button;
	}

	/**
	 * Creates a label and check box as two separate components.
	 */
	public static Button labeledCheckbox(Composite comp, FormToolkit toolkit,
			String label) {
		label(comp, toolkit, label);
		return checkbox(comp, toolkit);
	}

	/**
	 * Creates a radio button.
	 */
	public static Button radio(Composite comp, FormToolkit tk, String label) {
		var button = tk != null
				? tk.createButton(comp, label, SWT.RADIO)
				: new Button(comp, SWT.RADIO);
		if (tk == null && label != null) {
			button.setText(label);
		}
		return button;
	}

	/**
	 * Creates a radio button.
	 */
	public static Button radio(Composite comp) {
		return radio(comp, null, null);
	}

	/**
	 * Creates a radio button and a label as two components.
	 */
	public static Button labeledRadio(Composite comp, String label) {
		var button = new Button(comp, SWT.RADIO);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		label(comp, label);
		return button;
	}

	/**
	 * Creates a text as one component.
	 */
	public static Text text(Composite comp, int style) {
		return text(comp, null, null, style | SWT.BORDER);
	}

	/**
	 * Creates a text as one component.
	 */
	public static Text text(Composite comp) {
		return text(comp, null, null, SWT.BORDER);
	}

	/**
	 * Creates a text as one component.
	 */
	public static Text text(Composite comp, String label) {
		return text(comp, null, label, SWT.BORDER);
	}

	public static Text text(Composite comp, FormToolkit tk) {
		return text(comp, tk, null, SWT.BORDER);
	}

	public static Text text(Composite comp, FormToolkit tk, int style) {
		return text(comp, tk, null, style | SWT.BORDER);
	}

	/**
	 * Creates a text as one component.
	 */
	public static Text text(Composite comp, FormToolkit tk, String label,
			int style) {
		var text = tk != null
				? tk.createText(comp, null, style)
				: new Text(comp, style);
		if (label != null) {
			text.setText(label);
		}
		fillHorizontal(text);
		return text;
	}

	/**
	 * Creates a label and a text as two separate components.
	 */
	public static Text labeledText(Composite comp, String label, int style) {
		return labeledText(comp, null, label, style | SWT.BORDER);
	}

	/**
	 * Creates a label and a text as two separate components.
	 */
	public static Text labeledText(Composite comp, String label) {
		return labeledText(comp, null, label, SWT.BORDER);
	}

	/**
	 * Creates a label and a text as two separate components.
	 */
	public static Text labeledText(Composite comp, FormToolkit toolkit,
			String label) {
		return labeledText(comp, toolkit, label, SWT.BORDER);
	}

	/**
	 * Creates a label and a text as two separate components.
	 */
	public static Text labeledText(Composite comp, FormToolkit tk, String label,
			int style) {
		var lab = label(comp, tk, label);
		gridData(lab, false, false);
		return text(comp, tk, null, style);
	}

	public static FormText formText(Composite comp, FormToolkit tk,
			boolean trackFocus) {
		return tk.createFormText(comp, trackFocus);
	}

	public static Text emptyText(Composite comp, FormToolkit tk) {
		return emptyText(comp, tk, SWT.NONE);
	}

	public static Text emptyText(Composite comp, FormToolkit tk, int style) {
		return tk == null
				? new Text(comp, style)
				: tk.createText(comp, "", style);
	}

	public static Text emptyText(Composite comp, int style) {
		return new Text(comp, style);
	}

	/**
	 * Creates a multi text as one component.
	 */
	public static Text multiText(Composite comp, FormToolkit tk, String label) {
		return labeledMultiText(comp, tk, label, 100);
	}

	/**
	 * Creates a label and a multi text as two separate components.
	 */
	public static Text labeledMultiText(Composite comp, FormToolkit tk,
			String label, int heightHint) {
		var lab = label(comp, tk, label);
		var gd = gridData(lab, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		return multiText(comp, tk, heightHint);
	}

	/**
	 * Creates a multi text as one component.
	 */
	public static Text multiText(Composite comp, FormToolkit tk, int heightHint) {
		var style = SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI;
		var text = tk != null
				? tk.createText(comp, null, style)
				: new Text(comp, style);
		GridData gd = fillHorizontal(text);
		gd.minimumHeight = heightHint;
		gd.heightHint = heightHint;
		return text;
	}

	public static Text multiText(Composite comp, String label) {
		return labeledMultiText(comp, null, label, 100);
	}

	public static Text multiText(Composite comp) {
		return multiText(comp, null, 100);
	}

	/**
	 * Creates a label and a combo as two separate components.
	 */
	public static Combo labeledCombo(Composite comp, String label) {
		return labeledCombo(comp, null, label);
	}

	/**
	 * Creates a label and a combo as two separate components.
	 */
	public static Combo labeledCombo(Composite comp, FormToolkit tk,
			String label) {
		label(comp, tk, label);
		var combo = new Combo(comp, SWT.READ_ONLY);
		gridData(combo, true, false);
		return combo;
	}

	public static TableCombo tableCombo(Composite comp, FormToolkit tk,
			int style) {
		var combo = new TableCombo(comp, style);
		if (tk != null)
			tk.adapt(combo);
		return combo;
	}

	public static Label label(Composite comp) {
		return new Label(comp, SWT.NONE);
	}

	public static Label label(Composite comp, String text) {
		return label(comp, null, text);
	}

	public static Label label(Composite comp, FormToolkit tk, String text) {
		return label(comp, tk, text, SWT.NONE);
	}

	public static Label label(Composite comp, FormToolkit tk, int style) {
		return label(comp, tk, null, style);
	}

	public static Label label(Composite comp, FormToolkit tk, String text,
			int style) {
		Label label;
		var string = text != null ? text : "";
		if (tk != null) {
			label = tk.createLabel(comp, string, style);
		} else {
			label = new Label(comp, style);
			label.setText(string);
		}

		return label;
	}

	public static CLabel cLabel(Composite comp, FormToolkit tk, int style) {
		var cLabel = new CLabel(comp, style);
		tk.adapt(cLabel);
		return cLabel;
	}


	public static CLabel cLabel(Composite comp, FormToolkit tk) {
		return cLabel(comp, tk, SWT.NONE);
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static Label filler(Composite comp, FormToolkit tk) {
		return label(comp, tk, "");
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static Label filler(Composite comp) {
		return label(comp, null, "");
	}

	public static Hyperlink hyperlink(Composite comp, FormToolkit tk) {
		return hyperLink(comp, tk, null);
	}

	public static Hyperlink hyperLink(Composite comp, FormToolkit tk,
			String text) {
		Hyperlink link;
		var string = text != null ? text : "";
		if (tk != null)
			link = tk.createHyperlink(comp, string, SWT.NONE);
		else {
			link = new Hyperlink(comp, SWT.NONE);
			link.setText(string);
		}
		GridData gd = gridData(link, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		return link;
	}

	public static ImageHyperlink formCategoryLink(Composite comp, FormToolkit tk,
			String category, Image image) {
		var link = tk.createImageHyperlink(comp, SWT.NONE);
		link.setImage(image);
		link.setText(Strings.cutMid(category, 100));
		link.setToolTipText(category);
		return link;
	}

	public static ImageHyperlink imageHyperlink(Composite comp,
			FormToolkit tk, int style) {
		return tk.createImageHyperlink(comp, style);
	}

	public static ImageHyperlink imageHyperlink(Composite comp,
			FormToolkit tk) {
		return imageHyperlink(comp, tk, SWT.NONE);
	}

	public static Spinner spinner(Composite comp, FormToolkit tk, int style) {
		var spinner = new Spinner(comp, style);
		tk.adapt(spinner);
		return spinner;
	}

	public static Group group(Composite comp, FormToolkit tk) {
		return group(comp, tk, SWT.NONE);
	}

	public static Group group(Composite comp) {
		return group(comp, null, SWT.NONE);
	}

	public static Group group(Composite comp, int style) {
		return group(comp, null, style);
	}

	public static Group group(Composite comp, FormToolkit tk, int style) {
		var group = new Group(comp, style);
		if (tk != null)
			tk.adapt(group);
		return group;
	}

	public static StyledText styledText(Composite comp, FormToolkit tk) {
		var text = new StyledText(comp, SWT.BORDER);
		tk.adapt(text);
		return text;
	}

}
