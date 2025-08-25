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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the complete result of a Gemini query. Contains messages, metadata, and
 * status with convenient access methods. Follows Claude SDK signature pattern:
 * QueryResult(messages, metadata, status).
 */
public record QueryResult(@JsonProperty("messages") List<Message> messages, @JsonProperty("metadata") Metadata metadata,
		@JsonProperty("status") ResultStatus status) {

	@JsonCreator
	public QueryResult(@JsonProperty("messages") List<Message> messages, @JsonProperty("metadata") Metadata metadata,
			@JsonProperty("status") ResultStatus status) {
		this.messages = messages != null ? List.copyOf(messages) : List.of();
		this.metadata = metadata != null ? metadata : Metadata.empty();
		this.status = status != null ? status : ResultStatus.SUCCESS;
	}

	public static QueryResult of(List<Message> messages, Metadata metadata) {
		return new QueryResult(messages, metadata, ResultStatus.SUCCESS);
	}

	public static QueryResult of(List<Message> messages, Metadata metadata, ResultStatus status) {
		return new QueryResult(messages, metadata, status);
	}

	public static QueryResult of(Message message, Metadata metadata) {
		return new QueryResult(List.of(message), metadata, ResultStatus.SUCCESS);
	}

	public static QueryResult of(Message message, Metadata metadata, ResultStatus status) {
		return new QueryResult(List.of(message), metadata, status);
	}

	public static QueryResult empty() {
		return new QueryResult(List.of(), Metadata.empty(), ResultStatus.SUCCESS);
	}

	public static QueryResult error(String errorMessage, Metadata metadata) {
		Message errorMsg = TextMessage.error(errorMessage);
		return new QueryResult(List.of(errorMsg), metadata, ResultStatus.ERROR);
	}

	/**
	 * Gets the first message of the specified type.
	 */
	public Optional<Message> getFirstMessage(MessageType type) {
		return messages.stream().filter(msg -> msg.getType() == type).findFirst();
	}

	/**
	 * Gets all messages of the specified type.
	 */
	public List<Message> getMessages(MessageType type) {
		return messages.stream().filter(msg -> msg.getType() == type).collect(Collectors.toList());
	}

	/**
	 * Gets the primary response content (first assistant message).
	 */
	public Optional<String> getResponse() {
		return getFirstMessage(MessageType.ASSISTANT).map(Message::getContent);
	}

	/**
	 * Gets the full response content (all assistant messages concatenated).
	 */
	public String getFullResponse() {
		return getMessages(MessageType.ASSISTANT).stream().map(Message::getContent).collect(Collectors.joining("\n"));
	}

	/**
	 * Checks if the result contains any messages.
	 */
	public boolean hasMessages() {
		return !messages.isEmpty();
	}

	/**
	 * Checks if the result contains any assistant messages.
	 */
	public boolean hasResponse() {
		return getFirstMessage(MessageType.ASSISTANT).isPresent();
	}

	/**
	 * Checks if the result contains any error messages.
	 */
	public boolean hasErrors() {
		return getFirstMessage(MessageType.ERROR).isPresent();
	}

	/**
	 * Gets all error messages concatenated.
	 */
	public String getErrors() {
		return getMessages(MessageType.ERROR).stream().map(Message::getContent).collect(Collectors.joining("\n"));
	}

	/**
	 * Gets the total number of tokens used.
	 */
	public int getTotalTokens() {
		return metadata.usage().totalTokens();
	}

	/**
	 * Gets the total cost.
	 */
	public String getFormattedCost() {
		return metadata.cost().formatTotal();
	}

	/**
	 * Gets processing efficiency metrics.
	 */
	public String getEfficiencySummary() {
		return String.format("%.1f tokens/sec, %s cost/token, %s speed", metadata.getTokensPerSecond(),
				metadata.getCostPerToken(), metadata.getSpeedCategory());
	}

	/**
	 * Checks if the query was processed successfully.
	 */
	public boolean isSuccessful() {
		return status == ResultStatus.SUCCESS && hasResponse() && !hasErrors();
	}

	/**
	 * Checks if the query resulted in an error.
	 */
	public boolean isError() {
		return status == ResultStatus.ERROR;
	}

	/**
	 * Checks if the query timed out.
	 */
	public boolean isTimeout() {
		return status == ResultStatus.TIMEOUT;
	}

	/**
	 * Checks if the query was cancelled.
	 */
	public boolean isCancelled() {
		return status == ResultStatus.CANCELLED;
	}

	/**
	 * Checks if the query completed partially.
	 */
	public boolean isPartial() {
		return status == ResultStatus.PARTIAL;
	}
}