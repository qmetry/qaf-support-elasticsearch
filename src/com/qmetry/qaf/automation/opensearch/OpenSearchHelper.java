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
package com.qmetry.qaf.automation.opensearch;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.qmetry.qaf.automation.elasticsearch.TestCaseRunResultDocument;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.step.WsStep;
import com.qmetry.qaf.automation.util.JSONUtil;
import com.qmetry.qaf.automation.ws.WsRequestBean;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Helper class that uses qaf-support-ws instead of elasticsearch client. 
 * 
 * @author chirag.jayswal
 *
 */
public final class OpenSearchHelper {
	/**
	 * version suffix to be updated on change in index pattern.
	 */
	private static final String VER_SUFFIX = "_v1";
	private static final String INDEX_NAME = getBundle().getString("opensearch.index", "qaf_results").toLowerCase()+VER_SUFFIX;
	private static final String LOG_INDEX_NAME = INDEX_NAME + "_commandlogs";
	private static final String CHKPONIT_INDEX_NAME = INDEX_NAME + "_checkpoints";
	
	private static final String REQ_TMPL_KEY = "opensearch.req.tmpl";
	private static final String REQ_TEMPLATE = "{'baseUrl':'${opensearch.host}','headers':{'Content-Type':'application/json'}}";
	
	private static final String CREATE_INDEX_REQ = String.format("{'reference':'%s','method':'PUT','endpoint':'${index}/'}", REQ_TMPL_KEY);
	private static final String CREATE_DOC_REQ = String.format("{'reference':'%s','method':'POST','endpoint':'${index}/_doc/${id}'}", REQ_TMPL_KEY);
	
	private static final String TC_CYCLE_STATUS_QUERY_REQ = String.format("{'reference':'%s','method':'POST','endpoint':'/_sql'",REQ_TMPL_KEY);
	private static final String TC_CYCLE_STATUS_UPDATE_REQ =String.format("{'reference':'%s','method':'POST','endpoint':'/_update_by_query?conflicts=proceed&refresh'}", REQ_TMPL_KEY);

	private OpenSearchHelper() {
		//static access only
	}
	
	public static void createAssets() {
		setIfNotConfigured(REQ_TMPL_KEY, REQ_TEMPLATE);

		if (createIndex(INDEX_NAME)) {
			createIndex(CHKPONIT_INDEX_NAME);
			createIndex(LOG_INDEX_NAME);
		}
	}
	public static boolean submit(TestCaseRunResult result) {
		TestCaseRunResultDocument doc = new TestCaseRunResultDocument(result);
		UUID id = doc.getUdid();
		Gson gson = new Gson();
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("index", INDEX_NAME);
		params.put("id", id);
		boolean success = performRequest(CREATE_DOC_REQ,gson.toJson(doc),params);
		
		if(success && getBundle().containsKey("project.cyclename")) {
			updateCycle(doc);
		}
		//check-points
		params.put("index", CHKPONIT_INDEX_NAME);
		doc = new TestCaseRunResultDocument();
		doc.setUdid(id);
		doc.setSteps(result.getCheckPoints());
		doc.setStTime(result.getStarttime());
		//doc.setName(result);
		//doc.setStatus(result.getStatus().name());
		
		success = performRequest(CREATE_DOC_REQ,gson.toJson(doc),params);

		//command-logs
		params.put("index", LOG_INDEX_NAME);
		doc = new TestCaseRunResultDocument();
		doc.setUdid(id);
		doc.setCommands(result.getCommandLogs());
		doc.setStTime(result.getStarttime());
		success = performRequest(CREATE_DOC_REQ,gson.toJson(doc),params);
		
		return success;
	}

	private static void updateCycle(TestCaseRunResultDocument doc ) {
		try {
			String cycle = getBundle().getString("project.cyclename");
			String status = doc.getStatus();
			String tcname = doc.getName();
			String lastsuccess=doc.getStTime();
			String lastsuccesscycle=cycle;
			HashMap<String, Object> params = new HashMap<String, Object>(0);

			if(!status.equalsIgnoreCase("pass")) {
				try {
					String jsonEntity = String.format(
							"{\"query\":\"select executionInfo.project.cyclename, stTime from \\\"%s\\\" where executionInfo.project.cyclename IS NOT NULL AND status = 'PASS' AND name = '%s' order by stTime DESC limit 1\"}",
							INDEX_NAME, tcname);
					//Request request = new Request(METHOD_POST, "/_sql");
					//request.setJsonEntity(jsonEntity);
					//Response res = SERVICE.elasticSerachClient.performRequest(request);
					ClientResponse res = request(TC_CYCLE_STATUS_QUERY_REQ, jsonEntity, params);
					String resStr = res.getEntity(String.class);
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
			
			/*Request request = new Request(METHOD_POST, INDEX_NAME + "/_update_by_query?conflicts=proceed&refresh");
			//request.setJsonEntity(updateReqBody);
			//Response res = SERVICE.elasticSerachClient.performRequest(request);

			if (res.getStatusLine().getStatusCode() != 200) {
				System.err.println(EntityUtils.toString(res.getEntity()));
			}*/
			ClientResponse res = request(TC_CYCLE_STATUS_UPDATE_REQ, updateReqBody, params);
			if (res.getStatus() != 200) {
				System.err.println(res.getEntity(String.class));
			}
		} catch (Exception | Error e) {
			System.err.println(e.getMessage());
		}
	}
	private static boolean createIndex(String name) {
		try {
			String body = getBundle().getString("opensearch."+name+".entity", "{\"settings\":{\"index.mapping.total_fields.limit\":10000}}");
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("index", name);
			ClientResponse res = request(CREATE_INDEX_REQ, body, data);
			return res.getStatus() == 201;
		} catch (Exception e) {
			return false;
		}
	}
	
	private static boolean performRequest(String reqcall, String body, Map<String, Object> data) {
		try {
			ClientResponse res = request(reqcall, body, data);
			int status =  res.getStatus();
			return status == 200 || status == 201;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}
	}
	

	private static ClientResponse request(String reqcall, String body, Map<String, Object> data) {
		WsRequestBean bean = new WsRequestBean();
		bean.fillData(reqcall);
		bean.setBody(body);
		bean.resolveParameters(data);
		return WsStep.request(bean);
	}
	
	private static void setIfNotConfigured(String key, Object val) {
		if(!getBundle().containsKey(key)) {
			getBundle().setProperty(key, val);
		}
	}
}
