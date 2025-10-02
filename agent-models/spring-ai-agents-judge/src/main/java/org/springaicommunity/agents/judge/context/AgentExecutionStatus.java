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

package org.springaicommunity.agents.judge.context;

/**
 * Status of agent execution.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public enum AgentExecutionStatus {

	/**
	 * Agent completed successfully.
	 */
	SUCCESS,

	/**
	 * Agent failed with an error.
	 */
	FAILED,

	/**
	 * Agent execution timed out.
	 */
	TIMEOUT,

	/**
	 * Agent execution was cancelled.
	 */
	CANCELLED,

	/**
	 * Agent execution status is unknown.
	 */
	UNKNOWN

}
