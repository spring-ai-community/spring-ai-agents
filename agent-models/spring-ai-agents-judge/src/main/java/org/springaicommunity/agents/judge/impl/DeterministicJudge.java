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

package org.springaicommunity.agents.judge.impl;

import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.JudgeMetadata;
import org.springaicommunity.agents.judge.JudgeType;

/**
 * Base class for deterministic (rule-based) judges.
 *
 * <p>
 * Deterministic judges use programmatic rules without LLMs. Examples: file existence
 * checks, command execution validation, build success verification, test result parsing.
 * </p>
 *
 * <p>
 * Subclasses implement the
 * {@link #judge(org.springaicommunity.agents.judge.context.JudgmentContext)} method with
 * their specific evaluation logic.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public abstract class DeterministicJudge implements Judge {

	private final JudgeMetadata metadata;

	protected DeterministicJudge(String name, String description) {
		this.metadata = new JudgeMetadata(name, description, JudgeType.DETERMINISTIC);
	}

	@Override
	public JudgeMetadata getMetadata() {
		return this.metadata;
	}

}
