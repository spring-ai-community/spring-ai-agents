/*
 * Copyright 2025 Spring AI Community
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

package org.springaicommunity.agents.claude.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a stop hook that is called when the agent execution stops.
 *
 * <p>
 * The annotated method will be invoked when Claude's execution completes or is stopped.
 * This is useful for cleanup, logging, or final validation.
 * </p>
 *
 * <p>
 * Method signature requirements:
 * </p>
 * <ul>
 * <li>Parameter: {@link org.springaicommunity.claude.agent.sdk.types.control.HookInput}
 * or
 * {@link org.springaicommunity.claude.agent.sdk.types.control.HookInput.StopInput}</li>
 * <li>Return type:
 * {@link org.springaicommunity.claude.agent.sdk.types.control.HookOutput} or {@code void}
 * (void implies allow)</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * &#64;Component
 * public class SessionHooks {
 *

 *     &#64;Stop
 *     public void onSessionEnd(HookInput.StopInput input) {
 *         log.info("Agent execution stopped: {}", input.reason());
 *         metrics.recordSessionEnd();
 *     }
 *
 *
&#64;Stop
 *     public HookOutput cleanup(HookInput.StopInput input) {
 *         // Perform cleanup tasks
 *         cleanupTemporaryFiles();
 *         return HookOutput.allow();
 *     }
 * }
 * }</pre>
 *
 * @author Spring AI Community
 * @since 0.1.0
 * @see org.springaicommunity.claude.agent.sdk.hooks.HookCallback
 * @see org.springaicommunity.claude.agent.sdk.types.control.HookOutput
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Stop {

	/**
	 * Timeout in seconds for hook execution. If the hook takes longer than this, the
	 * execution will be blocked with an error.
	 * @return timeout in seconds (default 60)
	 */
	int timeout() default 60;

}
