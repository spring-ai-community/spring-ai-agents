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

package org.springaicommunity.agents.judge;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;
import reactor.core.publisher.Mono;

/**
 * Reactive interface for judging agent execution results.
 *
 * <p>
 * This interface is for Spring WebFlux applications that use reactive programming
 * patterns. It is completely separate from {@link Judge} and {@link AsyncJudge} following
 * Spring's pattern of separating sync, async, and reactive interfaces.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * &#64;Component
 * public class ReactiveJudgeAdapter implements ReactiveJudge {
 *     private final Judge delegate;
 *
 *     public ReactiveJudgeAdapter(Judge delegate) {
 *         this.delegate = delegate;
 *     }
 *
 *     public Mono<Judgment> judge(JudgmentContext context) {
 *         return Mono.fromCallable(() -> delegate.judge(context))
 *             .subscribeOn(Schedulers.boundedElastic());
 *     }
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Judge
 * @see AsyncJudge
 */
public interface ReactiveJudge {

	/**
	 * Reactively evaluate an agent execution result.
	 * @param context the judgment context containing all information about the agent
	 * execution
	 * @return a Mono that will emit the judgment
	 */
	Mono<Judgment> judge(JudgmentContext context);

	/**
	 * Get metadata about this judge (name, description, type).
	 * @return judge metadata
	 */
	JudgeMetadata getMetadata();

}
