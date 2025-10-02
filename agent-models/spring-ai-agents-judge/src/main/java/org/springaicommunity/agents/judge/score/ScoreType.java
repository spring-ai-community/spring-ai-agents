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

package org.springaicommunity.agents.judge.score;

/**
 * Types of scores used in judgments.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public enum ScoreType {

	/**
	 * Boolean score - pass/fail, true/false.
	 */
	BOOLEAN,

	/**
	 * Numerical score - typically 0-1 or 0-10 range.
	 */
	NUMERICAL,

	/**
	 * Categorical score - one of a set of allowed values (e.g., good/fair/poor).
	 */
	CATEGORICAL

}
