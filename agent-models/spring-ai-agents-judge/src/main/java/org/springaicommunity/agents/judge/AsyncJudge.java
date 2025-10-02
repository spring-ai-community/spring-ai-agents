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

import java.util.concurrent.CompletableFuture;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Asynchronous interface for judging agent execution results.
 *
 * <p>
 * This interface is completely separate from {@link Judge} following Spring's pattern of
 * separating sync and async interfaces. This allows for multiple async implementations
 * with varying sophistication levels.
 * </p>
 *
 * <p>
 * Async judges can be created by wrapping synchronous judges with an async adapter, or by
 * implementing this interface directly for truly asynchronous evaluation.
 * </p>
 *
 * <p>
 * <strong>Design Inspiration:</strong> Influenced by deepeval's async_mode pattern and
 * evals' parallel execution with ThreadPoolExecutor, but adapted to Spring's async
 * separation principle. Rather than default methods mixing sync/async (not Spring-like),
 * we provide completely separate interfaces for different execution models.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * &#64;Component
 * public class SimpleAsyncJudgeAdapter implements AsyncJudge {
 *     private final Judge delegate;
 *     private final Executor executor;
 *
 *     public SimpleAsyncJudgeAdapter(Judge delegate,
 *                                     &#64;Qualifier("judgeExecutor") Executor executor) {
 *         this.delegate = delegate;
 *         this.executor = executor;
 *     }
 *
 *     public CompletableFuture<Judgment> judgeAsync(JudgmentContext context) {
 *         return CompletableFuture.supplyAsync(
 *             () -> delegate.judge(context),
 *             executor
 *         );
 *     }
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Judge
 * @see ReactiveJudge
 */
public interface AsyncJudge {

	/**
	 * Asynchronously evaluate an agent execution result.
	 * @param context the judgment context containing all information about the agent
	 * execution
	 * @return a CompletableFuture that will complete with the judgment
	 */
	CompletableFuture<Judgment> judgeAsync(JudgmentContext context);

}
