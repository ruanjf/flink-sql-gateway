package com.ververica.flink.table.gateway.utils;

import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Table API 工具类.
 */
public class TableUtil {

	private static final Pattern STATEMENT_SET_START = Pattern.compile("[\\s\\S]*?\\n*?BEGIN\\s+STATEMENT\\s+SET\\s*$");
	private static final Pattern STATEMENT_SET_END = Pattern.compile("[\\s\\S]*?\\n+END\\s*$");
	private static final Pattern CONFIG_SET = Pattern.compile("[\\s\\S]*?\\n*?(SET|set)\\s+'(.*?)'\\s*=\\s*'(.*?)'\\s*$");

	/**
	 * 执行SQL任务，支持替换SQL中的变量（变量格式：${xxx}其中xxx为变量名）.
	 *
	 * @param tableEnv Table环境
	 * @param input SQL输入流
	 * @return 返回执行结果
	 */
	public static List<TableResult> executeSql(TableEnvironment tableEnv, String input) {
		if (input == null) {
			throw new RuntimeException("SQL内容不存在");
		}

		List<String> sqlList = CliStatementSplitter.splitContent(input);
		sqlList = sqlList.stream()
				.map(s -> {
					if (s.endsWith(";")) {
						return s.substring(0, s.length() - 1);
					}
					return s;
				})
				.collect(Collectors.toList());

		List<TableResult> results = new ArrayList<>(sqlList.size());

		StatementSet statementSet = null;
		for (String sql : sqlList) {
			Matcher matcher;
			if ((matcher = CONFIG_SET.matcher(sql)).matches()) {
				final String key = matcher.group(2);
				final String value = matcher.group(3);
				tableEnv.getConfig().getConfiguration().setString(key, value);
			} else if (STATEMENT_SET_START.matcher(sql).matches()) {
				statementSet = tableEnv.createStatementSet();
			} else if (STATEMENT_SET_END.matcher(sql).matches()) {
				if (statementSet != null) {
					results.add(statementSet.execute());
				}
				statementSet = null;
			} else if (statementSet != null) {
				statementSet.addInsertSql(sql);
			} else {
				results.add(tableEnv.executeSql(sql));
			}
		}

		return results;
	}

}