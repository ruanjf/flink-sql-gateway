/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.flink.table.gateway.rest.handler;

import com.ververica.flink.table.gateway.context.ExecutionContext;
import com.ververica.flink.table.gateway.rest.message.SessionIdPathParameter;
import com.ververica.flink.table.gateway.rest.message.SessionMessageParameters;
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.rest.session.Session;
import com.ververica.flink.table.gateway.rest.session.SessionManager;
import com.ververica.flink.table.gateway.utils.SqlGatewayException;
import com.ververica.flink.table.gateway.utils.TableUtil;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.rest.handler.HandlerRequest;
import org.apache.flink.runtime.rest.handler.RestHandlerException;
import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.types.Row;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Request handler for executing a statement.
 */
public class SqlExecuteHandler
	extends AbstractRestHandler<SqlExecuteHeaders.ExecuteRequestBody, SqlExecuteHeaders.ExecuteResponseBody, SessionMessageParameters> {

	private final SessionManager sessionManager;

	public SqlExecuteHandler(
		SessionManager sessionManager,
		Time timeout,
		Map<String, String> responseHeaders,
		MessageHeaders<
				SqlExecuteHeaders.ExecuteRequestBody,
				SqlExecuteHeaders.ExecuteResponseBody,
			SessionMessageParameters> messageHeaders) {

		super(timeout, responseHeaders, messageHeaders);
		this.sessionManager = sessionManager;
	}

	@Override
	protected CompletableFuture<SqlExecuteHeaders.ExecuteResponseBody> handleRequest(
		@Nonnull HandlerRequest<SqlExecuteHeaders.ExecuteRequestBody, SessionMessageParameters> request)
		throws RestHandlerException {

		String sessionId = request.getPathParameter(SessionIdPathParameter.class);

		String sqlText = request.getRequestBody().getSql();
		if (sqlText == null || sqlText.trim().isEmpty()) {
			throw new RestHandlerException("SQL must be provided.", HttpResponseStatus.BAD_REQUEST);
		}

		try {
			final Session session = sessionManager.getSession(sessionId);

			final ExecutionContext<?> context = session.getContext().getExecutionContext();
			final List<TableResult> results = context.wrapClassLoader(() -> TableUtil.executeSql(context.getTableEnvironment(), sqlText));

			final List<ResultSet> list = results.stream()
					.map(tr -> {
						ResolvedSchema schema = tr.getResolvedSchema();

						List<ColumnInfo> columns = schema.getColumns()
								.stream()
								.map(col -> ColumnInfo.create(col.getName(), col.getDataType().getLogicalType()))
								.collect(Collectors.toList());
						List<Row> data = new ArrayList<>();
						tr.collect().forEachRemaining(data::add);

						final ResultKind resultKind;
						switch (tr.getResultKind()) {
							case SUCCESS:
								resultKind = ResultKind.SUCCESS;
								break;
							case SUCCESS_WITH_CONTENT:
								resultKind = ResultKind.SUCCESS_WITH_CONTENT;
								break;
							default:
								throw new IllegalStateException("Unexpected value: " + tr.getResultKind());
						}
						return ResultSet.builder()
								.resultKind(resultKind)
								.columns(columns)
								.data(data)
								.build();
					})
					.collect(Collectors.toList());

			return CompletableFuture.completedFuture(
				new SqlExecuteHeaders.ExecuteResponseBody(list)
			);
		} catch (SqlGatewayException e) {
			throw new RestHandlerException(e.getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

}
