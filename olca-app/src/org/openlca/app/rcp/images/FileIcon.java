package org.openlca.app.rcp.images;

enum FileIcon {

	DEFAULT("file.png"),
	CSV("file/csv.png"),
	EXCEL("file/excel.png"),
	IMAGE("file/image.png"),
	MARKUP("file/markup.png"),
	PDF("file/pdf.png"),
	POWERPOINT("file/powerpoint.png"),
	WORD("file/word.png"),
	XML("file/xml.png"),
	ZIP("file/zip.png");

	final String fileName;

	private FileIcon(String fileName) {
		this.fileName = fileName;
	}

}
