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
import static org.elasticsearch.client.RestClient.builder;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.util.JSONUtil;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * This class used by {@link ElasticSearchIndexer} to index result documents. Please refer {@link ElasticSearchIndexer}.
 * 
 * @author chirag.jayswal
 *
 */
public class ElasticSerachService {
	/**
	 * version suffix to be updated on change in index pattern
	 */
	private static final String VER_SUFFIX = "_v1";
	private static final String INDEX_NAME = getBundle().getString("elasticsearch.index", "qaf_results").toLowerCase()+VER_SUFFIX;
	private static final String LOG_INDEX_NAME = INDEX_NAME + "_commandlogs";
	private static final String CHKPONIT_INDEX_NAME = INDEX_NAME + "_checkpoints";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_PUT = "PUT";
	private static final ElasticSerachService SERVICE = new ElasticSerachService();
	private RestClient elasticSerachClient;

	private ElasticSerachService() {
		init();
	}

	private void init() {
		try {
			elasticSerachClient = buildElasticSerachClient();
			createAssets();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createAssets() throws IOException {
		if (createIndex(INDEX_NAME)) {
			createIndex(CHKPONIT_INDEX_NAME);
			createIndex(LOG_INDEX_NAME);
		}
	}

	private boolean createIndex(String name) {
		try {
			Request request = new Request(METHOD_PUT, name + "/");
			request.setJsonEntity(getBundle().getString("elasticsearch."+name+".entity", "{\"settings\":{\"index.mapping.total_fields.limit\":10000}}"));
			Response response = elasticSerachClient.performRequest(request);
			return response.getStatusLine().getStatusCode() == 201;
		} catch (Exception e) {
			return false;
		}
	}
	
	private RestClient buildElasticSerachClient() {
		String providerClass = getBundle().getString("elasticsearch.client.provider", "");
		if(StringUtil.isNotBlank(providerClass)) {
			try {
				ElasticSearchClientProvider provider = (ElasticSearchClientProvider) Class.forName(providerClass).newInstance();
				return provider.buildElasticSerachClient();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}else {
			String[] hoststr = getBundle().getStringArray("elasticsearch.host", new String[] {});
			return builder(
					Arrays.stream(hoststr).map(s -> HttpHost.create((String) s)).toArray(HttpHost[]::new)).build();
		}
	}

	public static RestClient getElasticSerachClient() {
		return SERVICE.elasticSerachClient;
	}
	public static boolean perform(Request request) {
		if (null != SERVICE.elasticSerachClient) {
			try {
				int status = SERVICE.elasticSerachClient.performRequest(request).getStatusLine().getStatusCode();
				return status == 200 || status == 201;
			} catch (Exception e) {
				System.err.println(e.getMessage());
				return false;
			}
		}
		return false;
	}

	public static boolean submit(TestCaseRunResult result) {
		TestCaseRunResultDocument doc = new TestCaseRunResultDocument(result);
		UUID id = doc.getUdid();
		Gson gson = new Gson();
		Request request = new Request(METHOD_POST, INDEX_NAME + "/_doc/" + id);
		request.setJsonEntity(gson.toJson(doc));
		boolean success = ElasticSerachService.perform(request);

		request = new Request(METHOD_POST, CHKPONIT_INDEX_NAME + "/_doc/" + id);
		doc = new TestCaseRunResultDocument();
		doc.setUdid(id);
		doc.setSteps(result.getCheckPoints());
		doc.setStTime(result.getStarttime());
		//doc.setName(result);
		//doc.setStatus(result.getStatus().name());

		request.setJsonEntity(gson.toJson(doc));
		success = ElasticSerachService.perform(request);

		request = new Request(METHOD_POST, LOG_INDEX_NAME + "/_doc/" + id);
		doc = new TestCaseRunResultDocument();
		doc.setUdid(id);
		doc.setCommands(result.getCommandLogs());
		doc.setStTime(result.getStarttime());
		//doc.setName(result);

		request.setJsonEntity(gson.toJson(doc));
		success = ElasticSerachService.perform(request);
		
		if(success && getBundle().containsKey("project.cyclename")) {
			updateCycle(doc);
		}
		return success;
	}

	private static void updateCycle(TestCaseRunResultDocument doc ) {
		try {
			String cycle = getBundle().getString("project.cyclename");
			String status = doc.getStatus();
			String tcname = doc.getName();
			String lastsuccess=doc.getStTime();
			String lastsuccesscycle=cycle;

			if(!status.equalsIgnoreCase("pass")) {
				try {
					String jsonEntity = String.format(
							"{\"query\":\"select executionInfo.project.cyclename, stTime from \\\"%s\\\" where executionInfo.project.cyclename IS NOT NULL AND status = 'PASS' AND name = '%s' order by stTime DESC limit 1\"}",
							INDEX_NAME, tcname);
					Request request = new Request(METHOD_POST, "/_sql");
					request.setJsonEntity(jsonEntity);
					Response res = SERVICE.elasticSerachClient.performRequest(request);
					String resStr = EntityUtils.toString(res.getEntity());
					JsonArray result = JSONUtil.getGsonElement(resStr).getAsJsonObject().get("rows").getAsJsonArray();
					if (null!=result && result.size() > 0) {
						result = result.get(0).getAsJsonArray();
						if (null!=result && result.size() == 2) {
							lastsuccesscycle = result.get(0).getAsString();
							lastsuccess = result.get(1).getAsString();
						}
					}
				} catch (Exception | Error e) {
					lastsuccesscycle = null;
					lastsuccess = null;
				}
			}
			String updateReqBody = String.format("{" + 
					"\"script\": {" + 
					" \"source\": \"ctx._source['laststatus'] = '%s'; if('%s'!='null'){ctx._source['lastsuccess'] = '%s'; ctx._source['lastsuccesscycle'] = '%s';}\"" + 
					"}," + 
					" \"query\": {" + 
					" \"query_string\": {" + 
					" \"query\": \"executionInfo.project.cyclename: '%s' AND name: '%s'\"" + 
					" }" + 
					" }" + 
					"}", status,lastsuccess,lastsuccess,lastsuccesscycle,cycle,tcname);
			
			Request request = new Request(METHOD_POST, INDEX_NAME + "/_update_by_query?conflicts=proceed&refresh");
			request.setJsonEntity(updateReqBody);
			Response res = SERVICE.elasticSerachClient.performRequest(request);

			if (res.getStatusLine().getStatusCode() != 200) {
				System.err.println(EntityUtils.toString(res.getEntity()));
			}
		} catch (Exception | Error e) {
			System.err.println(e.getMessage());
		}
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
}
