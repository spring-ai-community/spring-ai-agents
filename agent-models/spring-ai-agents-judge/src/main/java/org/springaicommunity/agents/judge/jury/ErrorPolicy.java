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

package org.springaicommunity.agents.judge.jury;

/**
 * Policy for handling error judgments in voting strategies.
 *
 * <p>
 * Defines how voting strategies should treat judgments with ERROR status when aggregating
 * results.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public enum ErrorPolicy {

	/**
	 * Treat errors as failures (safest default).
	 */
	TREAT_AS_FAIL,

	/**
	 * Treat errors as abstentions (neutral).
	 */
	TREAT_AS_ABSTAIN,

	/**
	 * Ignore errored judgments entirely (skip in vote counting).
	 */
	IGNORE

}
