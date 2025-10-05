/*
 * Copyright 2024 Spring AI Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agents.judge.config;

import java.util.Map;

/**
 * Configuration specification for Judge instances loaded from YAML or other configuration
 * sources.
 *
 * <p>
 * This is a pure data transfer object (DTO) with no behavior. It holds configuration data
 * that driver programs (like spring-ai-bench) can use to instantiate judges via Spring DI
 * or other mechanisms.
 * </p>
 *
 * <p>
 * Example YAML:
 * </p>
 *
 * <pre>{@code
 * judge:
 *   type: file-content
 *   path: hello.txt
 *   expected: "Hello World!"
 *   matchMode: EXACT
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class JudgeSpec {

	private String type;

	private String path;

	private String expected;

	private String matchMode;

	private Map<String, Object> config;

	public JudgeSpec() {
	}

	public JudgeSpec(String type, String path, String expected, String matchMode) {
		this.type = type;
		this.path = path;
		this.expected = expected;
		this.matchMode = matchMode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getExpected() {
		return expected;
	}

	public void setExpected(String expected) {
		this.expected = expected;
	}

	public String getMatchMode() {
		return matchMode;
	}

	public void setMatchMode(String matchMode) {
		this.matchMode = matchMode;
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

}
