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

import com.ververica.flink.table.gateway.rest.message.SessionIdPathParameter;
import com.ververica.flink.table.gateway.rest.message.SessionMessageParameters;
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.runtime.rest.HttpMethodWrapper;
import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.runtime.rest.messages.RequestBody;
import org.apache.flink.runtime.rest.messages.ResponseBody;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;

/**
 * Message headers for executing a statement.
 */
public class SqlExecuteHeaders
	implements MessageHeaders<SqlExecuteHeaders.ExecuteRequestBody, SqlExecuteHeaders.ExecuteResponseBody, SessionMessageParameters> {

	private static final SqlExecuteHeaders INSTANCE = new SqlExecuteHeaders();

	public static final String URL = "/sessions/:" + SessionIdPathParameter.KEY + "/sql";

	private SqlExecuteHeaders() {
	}

	@Override
	public Class<ExecuteResponseBody> getResponseClass() {
		return ExecuteResponseBody.class;
	}

	@Override
	public HttpResponseStatus getResponseStatusCode() {
		return HttpResponseStatus.OK;
	}

	@Override
	public String getDescription() {
		return "Runs the provided SQL and returns the result. ";
	}

	@Override
	public Class<ExecuteRequestBody> getRequestClass() {
		return ExecuteRequestBody.class;
	}

	@Override
	public SessionMessageParameters getUnresolvedMessageParameters() {
		return new SessionMessageParameters();
	}

	@Override
	public HttpMethodWrapper getHttpMethod() {
		return HttpMethodWrapper.POST;
	}

	@Override
	public String getTargetRestEndpointURL() {
		return URL;
	}

	public static SqlExecuteHeaders getInstance() {
		return INSTANCE;
	}

	/**
	 * 请求.
	 */
	public static class ExecuteRequestBody implements RequestBody {

		private String sql;

		public String getSql() {
			return sql;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}
	}

	/**
	 * 响应.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ExecuteResponseBody implements ResponseBody {


		private final List<ResultSet> results;

		public ExecuteResponseBody(List<ResultSet> results) {
			this.results = results;
		}

		public List<ResultSet> getResults() {
			return results;
		}
	}

}
