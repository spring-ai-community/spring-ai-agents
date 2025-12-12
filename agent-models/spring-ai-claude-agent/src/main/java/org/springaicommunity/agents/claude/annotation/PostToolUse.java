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
 * Marks a method as a post-tool-use hook that is called after a tool has executed.
 *
 * <p>
 * The annotated method will be invoked after Claude executes the matched tool. The method
 * can inspect the tool result and perform logging, auditing, or other post-processing.
 * </p>
 *
 * <p>
 * Method signature requirements:
 * </p>
 * <ul>
 * <li>Parameter: {@link org.springaicommunity.claude.agent.sdk.types.control.HookInput}
 * or
 * {@link org.springaicommunity.claude.agent.sdk.types.control.HookInput.PostToolUseInput}</li>
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
 * public class AuditHooks {
 *

 *     &#64;PostToolUse
 *     public void auditAllTools(HookInput.PostToolUseInput input) {
 *         log.info("Tool {} completed with result: {}",
 *             input.toolName(), input.toolResult());
 *     }
 *
 *
&#64;PostToolUse(pattern = "Bash")
 *     public HookOutput checkBashResults(HookInput.PostToolUseInput input) {
 *         String result = input.toolResult();
 *         if (result != null && result.contains("error")) {
 *             log.warn("Bash command returned error: {}", result);
 *         }
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
public @interface PostToolUse {

	/**
	 * Regex pattern to match tool names.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>{@code "Bash"} - matches only Bash tool</li>
	 * <li>{@code "Bash|Write|Edit"} - matches Bash, Write, or Edit tools</li>
	 * <li>{@code ".*"} - matches all tools (same as empty string)</li>
	 * </ul>
	 * @return the tool pattern, or empty string to match all tools
	 */
	String pattern() default "";

	/**
	 * Timeout in seconds for hook execution. If the hook takes longer than this, the
	 * execution will be blocked with an error.
	 * @return timeout in seconds (default 60)
	 */
	int timeout() default 60;

}
