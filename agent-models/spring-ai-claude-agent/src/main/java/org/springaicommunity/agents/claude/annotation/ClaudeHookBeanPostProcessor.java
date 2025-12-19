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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.claude.sdk.hooks.HookCallback;
import org.springaicommunity.agents.claude.sdk.hooks.HookRegistration;
import org.springaicommunity.agents.claude.sdk.hooks.HookRegistry;
import org.springaicommunity.agents.claude.sdk.types.control.HookEvent;
import org.springaicommunity.agents.claude.sdk.types.control.HookInput;
import org.springaicommunity.agents.claude.sdk.types.control.HookOutput;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * BeanPostProcessor that discovers methods annotated with Claude hook annotations and
 * registers them with the {@link HookRegistry}.
 *
 * <p>
 * This processor scans Spring beans for methods annotated with:
 * </p>
 * <ul>
 * <li>{@link PreToolUse} - pre-tool-use hooks</li>
 * <li>{@link PostToolUse} - post-tool-use hooks</li>
 * <li>{@link UserPromptSubmit} - user-prompt-submit hooks</li>
 * <li>{@link Stop} - stop hooks</li>
 * </ul>
 *
 * <p>
 * Each annotated method is wrapped in a {@link HookCallback} and registered with the
 * {@link HookRegistry}. This allows annotation-based hook definition while using the same
 * underlying infrastructure as programmatic registration.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ClaudeHookBeanPostProcessor implements BeanPostProcessor, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeHookBeanPostProcessor.class);

	private final HookRegistry hookRegistry;

	public ClaudeHookBeanPostProcessor(HookRegistry hookRegistry) {
		this.hookRegistry = hookRegistry;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);

		ReflectionUtils.doWithMethods(targetClass, method -> {
			processPreToolUse(bean, beanName, method);
			processPostToolUse(bean, beanName, method);
			processUserPromptSubmit(bean, beanName, method);
			processStop(bean, beanName, method);
		});

		return bean;
	}

	private void processPreToolUse(Object bean, String beanName, Method method) {
		PreToolUse annotation = AnnotationUtils.findAnnotation(method, PreToolUse.class);
		if (annotation == null) {
			return;
		}

		validateMethodSignature(method, PreToolUse.class);

		String hookId = generateHookId(beanName, method);
		Pattern pattern = parsePattern(annotation.pattern());

		HookCallback callback = createCallback(bean, method);
		HookRegistration registration = new HookRegistration(hookId, HookEvent.PRE_TOOL_USE, pattern, callback,
				annotation.timeout());

		hookRegistry.register(registration);
		logger.info("Registered @PreToolUse hook: {} (pattern: {})", hookId, pattern != null ? pattern.pattern() : "*");
	}

	private void processPostToolUse(Object bean, String beanName, Method method) {
		PostToolUse annotation = AnnotationUtils.findAnnotation(method, PostToolUse.class);
		if (annotation == null) {
			return;
		}

		validateMethodSignature(method, PostToolUse.class);

		String hookId = generateHookId(beanName, method);
		Pattern pattern = parsePattern(annotation.pattern());

		HookCallback callback = createCallback(bean, method);
		HookRegistration registration = new HookRegistration(hookId, HookEvent.POST_TOOL_USE, pattern, callback,
				annotation.timeout());

		hookRegistry.register(registration);
		logger.info("Registered @PostToolUse hook: {} (pattern: {})", hookId,
				pattern != null ? pattern.pattern() : "*");
	}

	private void processUserPromptSubmit(Object bean, String beanName, Method method) {
		UserPromptSubmit annotation = AnnotationUtils.findAnnotation(method, UserPromptSubmit.class);
		if (annotation == null) {
			return;
		}

		validateMethodSignature(method, UserPromptSubmit.class);

		String hookId = generateHookId(beanName, method);

		HookCallback callback = createCallback(bean, method);
		HookRegistration registration = new HookRegistration(hookId, HookEvent.USER_PROMPT_SUBMIT, null, callback,
				annotation.timeout());

		hookRegistry.register(registration);
		logger.info("Registered @UserPromptSubmit hook: {}", hookId);
	}

	private void processStop(Object bean, String beanName, Method method) {
		Stop annotation = AnnotationUtils.findAnnotation(method, Stop.class);
		if (annotation == null) {
			return;
		}

		validateMethodSignature(method, Stop.class);

		String hookId = generateHookId(beanName, method);

		HookCallback callback = createCallback(bean, method);
		HookRegistration registration = new HookRegistration(hookId, HookEvent.STOP, null, callback,
				annotation.timeout());

		hookRegistry.register(registration);
		logger.info("Registered @Stop hook: {}", hookId);
	}

	private String generateHookId(String beanName, Method method) {
		return beanName + "." + method.getName();
	}

	private Pattern parsePattern(String patternString) {
		if (patternString == null || patternString.isEmpty()) {
			return null; // null pattern matches all tools
		}
		return Pattern.compile(patternString);
	}

	private HookCallback createCallback(Object bean, Method method) {
		ReflectionUtils.makeAccessible(method);
		Class<?> returnType = method.getReturnType();
		boolean voidReturn = void.class.equals(returnType) || Void.class.equals(returnType);

		return input -> {
			try {
				Object result = method.invoke(bean, input);
				if (voidReturn || result == null) {
					return HookOutput.allow();
				}
				return (HookOutput) result;
			}
			catch (Exception e) {
				logger.error("Hook execution failed: {}.{}", bean.getClass().getSimpleName(), method.getName(), e);
				Throwable cause = e.getCause() != null ? e.getCause() : e;
				return HookOutput.block("Hook execution failed: " + cause.getMessage());
			}
		};
	}

	private void validateMethodSignature(Method method, Class<? extends Annotation> annotationType) {
		// Validate parameter count
		Class<?>[] paramTypes = method.getParameterTypes();
		if (paramTypes.length != 1) {
			throw new IllegalStateException(String.format(
					"Method %s.%s annotated with @%s must have exactly one parameter of type HookInput or a subtype",
					method.getDeclaringClass().getSimpleName(), method.getName(), annotationType.getSimpleName()));
		}

		// Validate parameter type
		Class<?> paramType = paramTypes[0];
		if (!HookInput.class.isAssignableFrom(paramType)) {
			throw new IllegalStateException(String.format(
					"Method %s.%s annotated with @%s must have a parameter of type HookInput or a subtype, but found %s",
					method.getDeclaringClass().getSimpleName(), method.getName(), annotationType.getSimpleName(),
					paramType.getSimpleName()));
		}

		// Validate return type
		Class<?> returnType = method.getReturnType();
		if (!void.class.equals(returnType) && !Void.class.equals(returnType)
				&& !HookOutput.class.isAssignableFrom(returnType)) {
			throw new IllegalStateException(
					String.format("Method %s.%s annotated with @%s must return void or HookOutput, but found %s",
							method.getDeclaringClass().getSimpleName(), method.getName(),
							annotationType.getSimpleName(), returnType.getSimpleName()));
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
