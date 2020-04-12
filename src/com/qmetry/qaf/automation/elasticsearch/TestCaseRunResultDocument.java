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

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.configuration.ConfigurationConverter;

import com.qmetry.qaf.automation.core.CheckpointResultBean;
import com.qmetry.qaf.automation.core.LoggingBean;
import com.qmetry.qaf.automation.integration.TestCaseRunResult;
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
	private Throwable exception;
	private Collection<CheckpointResultBean> steps;
	private Collection<LoggingBean> commands;
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

	public TestCaseRunResultDocument() {
	}

	public TestCaseRunResultDocument(TestCaseRunResult result) {
		name = result.getName();
		status = result.getStatus().name();
		stTime = DateUtil.getFormatedDate(new Date(result.getStarttime()),DATE_FORMAT);
		duration = result.getEndtime() - result.getStarttime();
		exception = result.getThrowable();
		executionInfo = result.getExecutionInfo();
		suite_stTime = DateUtil.getFormatedDate(new Date(getBundle().getLong("suit.start.ts", sttime)),DATE_FORMAT);
		metadata = result.getMetaData();
		if (!getBundle().subset("project").isEmpty()) {
			executionInfo.put("project", ConfigurationConverter.getMap(getBundle().subset("project")));
		}
		udid = UUID.nameUUIDFromBytes((stTime + name).getBytes());
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

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
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
}
