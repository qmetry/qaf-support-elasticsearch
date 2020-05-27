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
import static com.qmetry.qaf.automation.util.StringMatcher.containsIgnoringCase;
import static com.qmetry.qaf.automation.util.StringUtil.abbreviate;
import static com.qmetry.qaf.automation.util.StringUtil.defaultString;
import static com.qmetry.qaf.automation.util.StringUtil.isBlank;
import static com.qmetry.qaf.automation.util.StringUtil.isNotBlank;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;
import static org.apache.commons.lang.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang.exception.ExceptionUtils.getThrowableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.lang.ClassUtils;

import com.qmetry.qaf.automation.core.CheckpointResultBean;
import com.qmetry.qaf.automation.core.LoggingBean;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.util.DateUtil;

/**
 * @author chirag.jayswal
 *
 */
public class TestCaseRunResultDocument {
	private static final long sttime = System.currentTimeMillis();
	private UUID udid;
	private String name;
	private String status;
	private String className;
	private String stTime;
	private String suite_stTime;
	private Long duration;
	private Map<String, Object> executionInfo;
	private Map<String, Object> metadata;
	private Collection<Object> testdata;
	private Map<String, Object> exception;
	private Collection<CheckpointResultBean> steps;
	private Collection<LoggingBean> commands;
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

	public TestCaseRunResultDocument() {
	}

	public TestCaseRunResultDocument(TestCaseRunResult result) {
		setName(result);
		status = result.getStatus().name();
		setStTime(result.getStarttime());
		duration = result.getEndtime() - result.getStarttime();
		setException(result);
		className=result.getClassName();
		executionInfo = result.getExecutionInfo();
		suite_stTime = DateUtil.getFormatedDate(new Date(getBundle().getLong("execution.start.ts", sttime)),DATE_FORMAT);
		metadata = result.getMetaData();
		if (!getBundle().subset("project").isEmpty()) {
			executionInfo.put("project", ConfigurationConverter.getMap(getBundle().subset("project")));
		}
		
		//map with key "a" and "a.b" will cause problem while indexing
		dotInKeyTo_(executionInfo);
		dotInKeyTo_(metadata);
		udid = UUID.nameUUIDFromBytes((stTime + name).getBytes());
		if(result.getTestData()!=null && !result.getTestData().isEmpty()) {
			setTestdata(result.getTestData());
		}
	}
	void setName(TestCaseRunResult result) {
		name = result.getName();
		if(result.getTestData()!=null && !result.getTestData().isEmpty()) {
			Object testData1 = result.getTestData().iterator().next();
			if (testData1 instanceof Map<?, ?>) {
				String identifierKey = ApplicationProperties.TESTCASE_IDENTIFIER_KEY.getStringVal("testCaseId");
				@SuppressWarnings("unchecked")
				Map<String, Object> testDataMap = (Map<String, Object>) testData1;
				String identifierVal = testDataMap.getOrDefault(identifierKey, "").toString();
				if(isBlank(identifierVal)) {
					identifierVal = testDataMap.getOrDefault("__index", "").toString();
				}
				if(isNotBlank(identifierVal)) {
					name = result.getName()+"-"+identifierVal;
				}
				wrapObj(testDataMap);
			}
		}
	}
	public UUID getUdid() {
		return udid;
	}

	public void setUdid(UUID udid) {
		this.udid = udid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getStTime() {
		return stTime;
	}

	public void setStTime(String stTime) {
		this.stTime = stTime;
	}
	
	public void setStTime(long stTime) {
		this.stTime = DateUtil.getFormatedDate(new Date(stTime),DATE_FORMAT);
	}

	public String getSuite_stTime() {
		return suite_stTime;
	}

	public void setSuite_stTime(String suite_stTime) {
		this.suite_stTime = suite_stTime;
	}

	public static long getSttime() {
		return sttime;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Map<String, Object> getExecutionInfo() {
		return executionInfo;
	}

	public void setExecutionInfo(Map<String, Object> executionInfo) {
		this.executionInfo = executionInfo;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public Collection<Object> getTestdata() {
		return testdata;
	}

	public void setTestdata(Collection<Object> testdata) {
		this.testdata = testdata;
	}

	public Map<String, Object> getException() {
		return exception;
	}

	public void setException(TestCaseRunResult result) {
		try {
			Throwable throwable = result.getThrowable();
			if (null != throwable) {
				exception = new HashMap<String, Object>();
				exception.put("class", ClassUtils.getShortClassName(throwable, "Unknown"));
		        List<String> messages = new ArrayList<String>();
				for(Object o : getThrowableList(throwable)) {
					Throwable cause = (Throwable)o;
					if(null!=cause && isNotBlank(cause.getMessage())) {
						messages.add(abbreviate(cause.getMessage(), 255));
					}
				}
				if(messages.isEmpty()) {
					String message = defaultString(throwable.getMessage(), ClassUtils.getShortClassName(throwable, "Unknown"));
					messages.add(abbreviate(message,255));
				}
				exception.put("messages", messages);
				exception.put("detailMessage", getMessage(throwable));
				exception.put("rootCauseMessage", abbreviate(getRootCauseMessage(throwable),255));
				exception.put("detailStackTrace", getFullStackTrace(throwable));
			} else if (!status.equalsIgnoreCase("PASS")) {
				Collection<CheckpointResultBean> chkPoints = result.getCheckPoints();
				List<String> failures = chkPoints.stream()
						.filter(c -> containsIgnoringCase("fail").match(c.getType())).map(c -> c.getMessage())
						.collect(Collectors.toList());
				if (failures != null && !failures.isEmpty()) {
					exception = new HashMap<String, Object>();
					exception.put("class", "VerificationFailure");
					exception.put("messages", failures.stream().map(s->abbreviate(s,255)).collect(Collectors.toList()));
					exception.put("detailMessage", String.format("%d verification failed", failures.size()));
					exception.put("rootCauseMessage", "verification failed");
					exception.put("detailstackTrace", failures.toString());
				}
			}
		} catch (Throwable e) {
			System.err.println("[TestCaseRunResultDocument] Unable to setException: " + e.getMessage());
		}
	}

	public Collection<CheckpointResultBean> getSteps() {
		return steps;
	}

	public void setSteps(Collection<CheckpointResultBean> steps) {
		this.steps = steps;
	}

	public Collection<LoggingBean> getCommands() {
		return commands;
	}

	public void setCommands(Collection<LoggingBean> commands) {
		this.commands = commands;
	}
	
	@SuppressWarnings("unchecked")
	private void dotInKeyTo_(Map<String, Object> map) {
		List<String> values = map.keySet().stream().filter(string -> string.indexOf(".") > 0)
				.collect(Collectors.toList());
		if (values != null && !values.isEmpty()) {
        	values.forEach(key ->map.put(key.replace('.', '_'), map.remove(key)));
		}
		
		map.values().stream().filter(val -> val instanceof Map)
		.collect(Collectors.toList()).forEach(m->dotInKeyTo_((Map<String, Object>)m));
	}

	@SuppressWarnings("unchecked")
	private void wrapObj(Map<String, Object> map) {

		for (Entry<String, Object> entry : map.entrySet()) {
			Object val = entry.getValue();
			if (null != val) {
				if (val instanceof Map) {
					wrapObj((Map<String, Object>) val);
				} else if (!val.getClass().isArray() && !(val instanceof Collection)) {
					entry.setValue(String.valueOf(val));
				} else {
					entry.setValue(String.valueOf(val));
				}
			}
		}

	}
}
