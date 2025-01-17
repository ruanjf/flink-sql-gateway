package com.ververica.flink.table.gateway.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 参考 org.apache.flink.table.client.cli.CliStatementSplitter.
 */
public class CliStatementSplitter {

	private static final String MASK = "--.*$";
	private static final String BEGINNING_MASK = "^(\\s)*--.*$";

	public static boolean isStatementComplete(String statement) {
		String[] lines = statement.split("\n");
		// fix input statement is "\n"
		if (lines.length == 0) {
			return false;
		} else {
			return isEndOfStatement(lines[lines.length - 1]);
		}
	}

	public static List<String> splitContent(String content) {
		List<String> statements = new ArrayList<>();
		List<String> buffer = new ArrayList<>();

		for (String line : content.split("\n")) {
			if (isEndOfStatement(line)) {
				buffer.add(line);
				statements.add(normalize(buffer));
				buffer.clear();
			} else {
				buffer.add(line);
			}
		}
		if (!buffer.isEmpty()) {
			statements.add(normalize(buffer));
		}
		return statements;
	}

	private static String normalize(List<String> buffer) {
		// remove comment lines
		return buffer.stream()
				.map(statementLine -> statementLine.replaceAll(BEGINNING_MASK, ""))
				.collect(Collectors.joining("\n"));
	}

	private static boolean isEndOfStatement(String line) {
		return line.replaceAll(MASK, "").trim().endsWith(";");
	}

}
