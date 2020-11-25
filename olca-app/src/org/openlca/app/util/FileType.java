package org.openlca.app.util;

import java.io.File;

public enum FileType {

	DEFAULT("*"),

	CSV("csv"),

	EXCEL("xls","xlsx", "ods"),

	IMAGE("png", "jpg", "jpeg", "gif"),

	MARKUP("html", "spold", "htm", "xhtml"),

	PDF("pdf"),

	POWERPOINT("ppt", "pptx","odp"),

	PYTHON("py", "pyc"),

	SQL("sql"),

	WORD("doc", "docx", "odt"),

	XML("xml"),

	ZIP("zip");

	private final String[] extensions;
	
	FileType(String... extensions) {
		this.extensions = extensions;
	}
	
	public static FileType of(File file) {
		if (file == null)
			return DEFAULT;
		return forName(file.getName());
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
