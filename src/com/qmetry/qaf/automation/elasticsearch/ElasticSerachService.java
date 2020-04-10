/*******************************************************************************
 * Copyright (c) 2019 Infostretch Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.qmetry.qaf.automation.elasticsearch;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.util.FileUtil;
import com.qmetry.qaf.automation.util.JSONUtil;

/**
 * @author chirag.jayswal
 *
 */
public class ElasticSerachService {
	private static final ElasticSerachService SERVICE = new ElasticSerachService();
	private static final String INDEX_NAME = getBundle().getString("elasticsearch.index", "qaf_results");
	private static final String LOG_INDEX_NAME = INDEX_NAME + "_commandlogs";
	private static final String CHKPONIT_INDEX_NAME = INDEX_NAME + "_checkpoints";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_PUT = "PUT";

	private RestClient elasticSerachClient;

	private ElasticSerachService() {
		init();
	}

	private void init() {
		try {
			String[] hoststr = getBundle().getStringArray("elasticsearch.host", new String[] {});
			elasticSerachClient = RestClient
					.builder(Arrays.stream(hoststr).map(s -> HttpHost.create((String) s)).toArray(HttpHost[]::new))
					.build();
			createAssets();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createAssets() throws URISyntaxException, IOException {
		if (createIndex(INDEX_NAME)) {
			createIndex(CHKPONIT_INDEX_NAME);
			createIndex(LOG_INDEX_NAME);
			String[] kibanahoststr = getBundle().getStringArray("kibana.host", new String[] {});
			if (kibanahoststr.length > 0) {
				RestClient kibanaClient = RestClient.builder(
						Arrays.stream(kibanahoststr).map(s -> HttpHost.create((String) s)).toArray(HttpHost[]::new))
						.build();
				Request request = new Request(METHOD_PUT, "/");
				URI kibanaDashBoard = this.getClass().getResource("objects.ndjson").toURI();
				String objects = FileUtil.readFileToString(new File(kibanaDashBoard), StandardCharsets.UTF_8);
				request.setJsonEntity(objects);
				kibanaClient.performRequest(request);
			}
		}
	}

	private boolean createIndex(String name) {
		try {
			Request request = new Request(METHOD_PUT, name + "/");
			// request.setJsonEntity(getMappings());
			Response response = elasticSerachClient.performRequest(request);
			return response.getStatusLine().getStatusCode() == 201;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean perform(Request request) {
		if (null != SERVICE.elasticSerachClient) {
			try {
				int status = SERVICE.elasticSerachClient.performRequest(request).getStatusLine().getStatusCode();
				return status == 200 || status == 201;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	public static boolean submit(TestCaseRunResult result) {
		TestCaseRunResultDocument doc = new TestCaseRunResultDocument(result);
		String id = doc.getUdid().toString();

		Request request = new Request(METHOD_POST, INDEX_NAME + "/_doc/" + id);
		request.setJsonEntity(JSONUtil.toString(doc));
		boolean success = ElasticSerachService.perform(request);

		request = new Request(METHOD_POST, CHKPONIT_INDEX_NAME + "/_doc/" + id);
		doc = new TestCaseRunResultDocument();
		doc.setSteps(result.getCheckPoints());
		request.setJsonEntity(JSONUtil.toString(doc));
		success = ElasticSerachService.perform(request);

		request = new Request(METHOD_POST, LOG_INDEX_NAME + "/_doc/" + id);
		doc = new TestCaseRunResultDocument();
		doc.setCommands(result.getCommandLogs());
		request.setJsonEntity(JSONUtil.toString(doc));
		success = ElasticSerachService.perform(request);

		return success;
	}

	public static void close() {
		if (null != SERVICE.elasticSerachClient) {
			try {
				SERVICE.elasticSerachClient.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
	private static String getMappings() {
		Map<String, Object> mappings = new HashMap<String, Object>();
		Map<String, Object> doc = new HashMap<String, Object>();
		Map<String, Object> pros = new HashMap<String, Object>();
		Map<String, Object> fieldsmap = new HashMap<String, Object>();
		Map<String, Object> dateMapping = new HashMap<String, Object>();
		dateMapping.put("type", "date");
		dateMapping.put("format", "epoch_millis");
		fieldsmap.put("stTime", dateMapping);
		fieldsmap.put("suite_stTime", dateMapping);
		pros.put("properties", fieldsmap);
		doc.put("_doc", pros);
		mappings.put("mappings", doc);
		return JSONUtil.toString(mappings);
	}

}
