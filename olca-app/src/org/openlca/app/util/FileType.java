package org.openlca.app.util;

import java.io.File;

public enum FileType {

	DEFAULT("*"),

	CSV("csv"),

	EXCEL("xls", "xlsx", "ods"),

	IMAGE("png", "jpg", "jpeg", "gif"),

	MARKUP("html", "htm", "xhtml"),

	PDF("pdf"),

	POWERPOINT("ppt", "pptx", "odp"),

	PYTHON("py", "pyc"),

	SQL("sql"),

	WORD("doc", "docx", "odt"),

	XML("xml", "spold"),

	ZIP("zip");

	private final String[] extensions;

	FileType(String... extensions) {
		this.extensions = extensions;
	}

	public static FileType of(File file) {
		return file != null
				? forName(file.getName())
				: DEFAULT;
	}

	public static FileType forName(String fileName) {
		if (fileName == null)
			return DEFAULT;
		var n = fileName.trim().toLowerCase();
		for (FileType type : values()) {
			for (String ext : type.extensions) {
				if (n.endsWith("." + ext))
					return type;
			}
		}
		return DEFAULT;
	}
}
