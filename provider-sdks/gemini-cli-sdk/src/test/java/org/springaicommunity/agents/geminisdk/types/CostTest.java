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

package org.springaicommunity.agents.geminisdk.types;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class CostTest {

	@Test
	void testCostCreation() {
		Cost cost = Cost.of(0.001, 0.002);

		assertThat(cost.promptCost()).isEqualByComparingTo(BigDecimal.valueOf(0.001));
		assertThat(cost.completionCost()).isEqualByComparingTo(BigDecimal.valueOf(0.002));
		assertThat(cost.totalCost()).isEqualByComparingTo(BigDecimal.valueOf(0.003));
	}

	@Test
	void testCostCalculations() {
		Cost cost = Cost.of(0.001, 0.002);

		BigDecimal withMarkup = cost.calculateWithMarkup(0.20); // 20% markup
		assertThat(withMarkup).isEqualByComparingTo(BigDecimal.valueOf(0.0036));

		BigDecimal perToken = cost.calculatePerToken(100);
		assertThat(perToken).isEqualByComparingTo(BigDecimal.valueOf(0.00003));
	}

	@Test
	void testZeroCost() {
		Cost cost = Cost.zero();

		assertThat(cost.promptCost()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(cost.completionCost()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(cost.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(cost.hasCost()).isFalse();
	}

	@Test
	void testCostAddition() {
		Cost cost1 = Cost.of(0.001, 0.002);
		Cost cost2 = Cost.of(0.0005, 0.001);

		Cost combined = cost1.add(cost2);

		assertThat(combined.promptCost()).isEqualByComparingTo(BigDecimal.valueOf(0.0015));
		assertThat(combined.completionCost()).isEqualByComparingTo(BigDecimal.valueOf(0.003));
		assertThat(combined.totalCost()).isEqualByComparingTo(BigDecimal.valueOf(0.0045));
	}

	@Test
	void testCostPercentages() {
		Cost cost = Cost.of(0.001, 0.002); // 33.33% prompt, 66.67% completion

		assertThat(cost.getPromptCostPercentage()).isCloseTo(33.33, within(0.01));
		assertThat(cost.getCompletionCostPercentage()).isCloseTo(66.67, within(0.01));
	}

	@Test
	void testFormatting() {
		Cost cost = Cost.of(0.001234, 0.002345);

		String formatted = cost.formatTotal();
		assertThat(formatted).isEqualTo("$0.003579");
	}

}