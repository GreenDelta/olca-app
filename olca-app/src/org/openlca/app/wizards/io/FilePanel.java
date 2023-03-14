package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.FileType;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.util.Strings;

class FilePanel {

	private final Consumer<List<File>> handler;
	private final List<File> files = new ArrayList<>();
	private  String title = "Select one or more files";
	private String[] extensions;

	private FilePanel(Consumer<List<File>> handler) {
		this.handler = handler;
	}

	static FilePanel on(Consumer<List<File>> handler) {
		return new FilePanel(handler);
	}

	FilePanel withTitle(String title) {
		if (title != null) {
			this.title = title;
		}
		return this;
	}

	FilePanel withFiles(List<File> files) {
		if (files == null)
			return this;
		this.files.addAll(files);
		return this;
	}

	FilePanel withExtensions(String... exts) {
		if (exts == null || exts.length == 0) {
			extensions = null;
			return this;
		}
		extensions = Arrays.stream(exts)
				.filter(Strings::notEmpty)
				.map(e -> e.startsWith("*.") ? e : "*." + e)
				.toArray(String[]::new);
		return this;
	}

	void render(Composite body) {

		var link = new Hyperlink(body, SWT.NONE);
		link.setText(title);
		link.setForeground(Colors.linkBlue());

		var viewer = Tables.createViewer(body, M.File);
		var table = viewer.getTable();
		table.setHeaderVisible(false);
		table.setLinesVisible(false);
		Tables.bindColumnWidths(table, 1.0);
		viewer.setLabelProvider(new FileLabel());
		viewer.setInput(files);

		var addFiles = Actions.create(M.Add, Icon.ADD.descriptor(), () -> {
			var next = FileChooser.openFile()
					.withExtensions(extensions)
					.withTitle(title)
					.selectMultiple();
			if (next.isEmpty())
				return;
			for (var f : next) {
				if (!files.contains(f)) {
					files.add(f);
				}
			}
			viewer.setInput(files);
			handler.accept(files);
		});

		var removeFiles = Actions.create(M.Remove, Icon.DELETE.descriptor(), () -> {
			List<File> removals = Viewers.getAllSelected(viewer);
			if (removals.isEmpty())
				return;
			files.removeAll(removals);
			viewer.setInput(files);
			handler.accept(files);
		});

		Actions.bind(viewer, addFiles, removeFiles);


		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				addFiles.run();
			}

			@Override
			public void linkEntered(HyperlinkEvent e) {
				link.setUnderlined(true);
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				link.setUnderlined(false);
			}
		});

	}


	private static class FileLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return obj instanceof File file
					? Images.get(FileType.of(file))
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof File file
					? file.getAbsolutePath()
					: null;
		}
	}
}
