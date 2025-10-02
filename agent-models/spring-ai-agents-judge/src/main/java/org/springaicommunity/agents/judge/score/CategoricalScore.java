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

import java.util.List;

/**
 * Categorical score representing one of a set of allowed values.
 *
 * @param value the selected category value
 * @param allowedValues the list of valid category values
 * @author Mark Pollack
 * @since 0.1.0
 */
public record CategoricalScore(String value, List<String> allowedValues) implements Score {

	public CategoricalScore {
		if (!allowedValues.contains(value)) {
			throw new IllegalArgumentException(String.format("Score '%s' must be one of %s", value, allowedValues));
		}
	}

	@Override
	public ScoreType type() {
		return ScoreType.CATEGORICAL;
	}

}
