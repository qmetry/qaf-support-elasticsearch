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

import com.qmetry.qaf.automation.integration.TestCaseResultUpdator;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.util.StringUtil;
import static com.qmetry.qaf.automation.opensearch.OpenSearchHelper.*;

/**
 * This is {@link TestCaseResultUpdator} implementation should work for both elastic search and open search. It is uses following properties:
 * <ul>
 * <li>opensearch.host: required property to set elastic search or opensearch host. If not provided or blank, listener will be disabled. </li>
 * <li>opensearch.index: (option) property to set elastic search index name. Default value is "qaf_results"</li>
 * <li>opensearch.req.tmpl: (option) request template, if provided will be used as reference for request call.
 * <li>opensearch.reporter: (optional) boolean to enable /disable reporter. Default value is true.
 * </ul>
 * 
 * It is using qaf-support-ws library instead of elastic search client library.
 * 
 * @author chirag.jayswal
 *
 */
public class OpenSearchIndexer implements TestCaseResultUpdator {
	
	
	public OpenSearchIndexer() {
		init();
	}

	

	@Override
	public boolean updateResult(TestCaseRunResult result) {
		return submit(result);
	}

	@Override
	public String getToolName() {
		return "OpenSearch Indexer";
	}
	@Override
	public boolean enabled() {
		return  !ApplicationProperties.DRY_RUN_MODE.getBoolenVal(false)
				&&StringUtil.isNotBlank(getBundle().getString("opensearch.host"))
				&& getBundle().getBoolean("opensearch.reporter", true);
	}
	
	@Override
	public boolean allowConfigAndRetry() {
		return false;
	}
	
	@Override
	public void beforeShutDown() {
		TestCaseResultUpdator.super.beforeShutDown();
	}
	
	private void init() {
		if(enabled()) {
			createAssets();
		}
	}



	
}
