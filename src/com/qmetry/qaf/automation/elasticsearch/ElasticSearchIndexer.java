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
import static com.qmetry.qaf.automation.elasticsearch.ElasticSerachService.close;
import static com.qmetry.qaf.automation.elasticsearch.ElasticSerachService.submit;

import com.qmetry.qaf.automation.integration.TestCaseResultUpdator;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.util.StringUtil;
/**
 * This is {@link TestCaseResultUpdator} implementation for Elastic search. It is uses following properties:
 * <ul>
 * <li>elasticsearch.host: required property to set elastic search host(s).</li>
 * <li>kibana.host: optional property to set kibana host. If provided it will export and objects to Kibana.</li>
 * <li>elasticsearch.index: property to set elastic search index name. Default value is "qaf_results"</li>
 * </ul>
 * @author chirag.jayswal
 *
 */
public class ElasticSearchIndexer implements TestCaseResultUpdator {

	@Override
	public String getToolName() {
		return "Elasticsearch Indexer";
	}

	@Override
	public boolean updateResult(TestCaseRunResult result) {
		try {
			return submit(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void beforeShutDown() {
		close();
	}

	@Override
	public boolean allowConfigAndRetry() {
		return false;
	}
	
	@Override
	public boolean enabled() {
		return  !ApplicationProperties.DRY_RUN_MODE.getBoolenVal(false)
				&&StringUtil.isNotBlank(getBundle().getString("elasticsearch.host"))
				&& getBundle().getBoolean("elasticsearch.reporter", true);
	}

	@Override
	public boolean equals(Object obj) {
		if(null==obj || !(obj instanceof ElasticSearchIndexer))
			return false;
		return getToolName().equalsIgnoreCase(((ElasticSearchIndexer)obj).getToolName());
	}
	
}
