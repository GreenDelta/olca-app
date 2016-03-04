package refdata;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A small utility class for sorting the reference data by UUID. With this it is
 * easier to see updates in the diff-files.
 */
public class RefDataSort {

	public static void main(String[] args) {
		try {
			sortFiles(Paths.get("data", "all"));
			sortFiles(Paths.get("data", "units"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sortFiles(Path dir) throws Exception {
		Files.list(dir).forEach(file -> {
			if (!file.toString().endsWith(".csv"))
				return;
			try {
				sortFile(file);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void sortFile(Path file) throws Exception {
		System.out.println("  Sort:" + file);
		Charset utf8 = Charset.forName("utf-8");
		List<String> raws = Files.readAllLines(file, utf8);
		List<Line> lines = new ArrayList<>();
		for (String raw : raws) {
			String s = stripByteOrderMark(raw, utf8);
			lines.add(new Line(s));
		}
		lines.sort((line1, line2) -> line1.uuid.compareTo(line2.uuid));
		List<String> sorted = new ArrayList<>();
		for (Line line : lines) {
			sorted.add(line.rawLine);
		}
		Files.write(file, sorted, utf8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * The first line of a file may starts with a byte-order-mark
	 * (https://en.wikipedia.org/wiki/Byte_order_mark). When we sort the lines
	 * this line may is not the first line anymore which will lead to errors in
	 * the CSV import. Thus, we remove a byte-order-mark if the line starts with
	 * this.
	 */
	private static String stripByteOrderMark(String raw, Charset utf8) {
		byte[] bytes = raw.getBytes(utf8);
		if (bytes.length < 3)
			return raw;
		if ((bytes[0] == (byte) 0xEF) && (bytes[1] == (byte) 0xBB) && (bytes[2] == (byte) 0xBF))
			return new String(bytes, 3, bytes.length - 3, utf8);
		else
			return raw;
	}

	private static class Line {
		String uuid;
		String rawLine;

		Line(String rawLine) {
			this.rawLine = rawLine;
			uuid = rawLine.split(";")[0];
			if (uuid.startsWith("\"") && uuid.endsWith("\"")) {
				uuid = uuid.substring(1, uuid.length() - 1);
			}
			int length = uuid.length();
			if (length != 36) {
				System.out.println("  Invalid UUID: " + uuid);
			}
		}
	}

}
