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

package org.springaicommunity.agents.claude.autoconfigure;

import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.annotation.ClaudeHookBeanPostProcessor;
import org.springaicommunity.claude.agent.sdk.hooks.HookRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Claude hook annotation processing.
 *
 * <p>
 * This configuration provides:
 * </p>
 * <ul>
 * <li>A {@link HookRegistry} bean for hook registration</li>
 * <li>A {@link ClaudeHookBeanPostProcessor} that discovers annotated hook methods</li>
 * </ul>
 *
 * <p>
 * With this configuration, methods annotated with {@code @PreToolUse},
 * {@code @PostToolUse}, {@code @UserPromptSubmit}, or {@code @Stop} will be automatically
 * registered as hooks.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
@AutoConfiguration(before = ClaudeAgentAutoConfiguration.class)
@ConditionalOnClass(ClaudeAgentModel.class)
public class ClaudeHookAutoConfiguration {

	/**
	 * Creates a HookRegistry bean if one doesn't exist.
	 *
	 * <p>
	 * The registry stores all hook registrations and is used by both:
	 * </p>
	 * <ul>
	 * <li>The annotation processor to register discovered hooks</li>
	 * <li>The ClaudeAgentModel to execute hooks during tool operations</li>
	 * </ul>
	 * @return a new HookRegistry instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public HookRegistry hookRegistry() {
		return new HookRegistry();
	}

	/**
	 * Creates the bean post processor that discovers and registers annotated hooks.
	 *
	 * <p>
	 * This processor scans all Spring beans for methods annotated with:
	 * </p>
	 * <ul>
	 * <li>{@link org.springaicommunity.agents.claude.annotation.PreToolUse}</li>
	 * <li>{@link org.springaicommunity.agents.claude.annotation.PostToolUse}</li>
	 * <li>{@link org.springaicommunity.agents.claude.annotation.UserPromptSubmit}</li>
	 * <li>{@link org.springaicommunity.agents.claude.annotation.Stop}</li>
	 * </ul>
	 * @param hookRegistry the registry to add discovered hooks to
	 * @return the configured bean post processor
	 */
	@Bean
	public ClaudeHookBeanPostProcessor claudeHookBeanPostProcessor(HookRegistry hookRegistry) {
		return new ClaudeHookBeanPostProcessor(hookRegistry);
	}

}
