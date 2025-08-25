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

package org.springaicommunity.agents.claudecode.sdk.types;

import java.util.List;
import java.util.Optional;

/**
 * Top-level response object containing messages and metadata. Provides convenient
 * accessors and domain-specific operations.
 */
public record QueryResult(List<Message> messages, Metadata metadata, ResultStatus status) {

	/**
	 * Returns the first assistant response text, if any.
	 */
	public Optional<String> getFirstAssistantResponse() {
		return messages.stream()
			.filter(m -> m instanceof AssistantMessage)
			.findFirst()
			.flatMap(m -> ((AssistantMessage) m).getTextContent());
	}

	/**
	 * Returns true if any message contains tool use.
	 */
	public boolean hasToolUse() {
		return messages.stream()
			.filter(m -> m instanceof AssistantMessage)
			.anyMatch(m -> ((AssistantMessage) m).hasToolUse());
	}

	/**
	 * Returns all tool use blocks across all messages.
	 */
	public List<ToolUseBlock> getAllToolUses() {
		return messages.stream()
			.filter(m -> m instanceof AssistantMessage)
			.flatMap(m -> ((AssistantMessage) m).getToolUses().stream())
			.toList();
	}

	/**
	 * Returns all assistant messages in the result.
	 */
	public List<AssistantMessage> getAssistantMessages() {
		return messages.stream().filter(m -> m instanceof AssistantMessage).map(m -> (AssistantMessage) m).toList();
	}

	/**
	 * Returns all user messages in the result.
	 */
	public List<UserMessage> getUserMessages() {
		return messages.stream().filter(m -> m instanceof UserMessage).map(m -> (UserMessage) m).toList();
	}

	/**
	 * Returns the result message if present.
	 */
	public Optional<ResultMessage> getResultMessage() {
		return messages.stream().filter(m -> m instanceof ResultMessage).map(m -> (ResultMessage) m).findFirst();
	}

	/**
	 * Returns true if the query was successful (no errors).
	 */
	public boolean isSuccessful() {
		return status == ResultStatus.SUCCESS;
	}

	/**
	 * Returns true if the query resulted in an error.
	 */
	public boolean isError() {
		return status == ResultStatus.ERROR;
	}

	/**
	 * Returns the total number of messages.
	 */
	public int getMessageCount() {
		return messages.size();
	}

	/**
	 * Returns true if this was an expensive query.
	 */
	public boolean isExpensive() {
		return metadata.isExpensive();
	}

	/**
	 * Returns true if this was a fast response.
	 */
	public boolean isFastResponse() {
		return metadata.isFastResponse();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private List<Message> messages;

		private Metadata metadata;

		private ResultStatus status = ResultStatus.SUCCESS;

		public Builder messages(List<Message> messages) {
			this.messages = messages;
			return this;
		}

		public Builder addMessage(Message message) {
			if (this.messages == null) {
				this.messages = new java.util.ArrayList<>();
			}
			this.messages.add(message);
			return this;
		}

		public Builder metadata(Metadata metadata) {
			this.metadata = metadata;
			return this;
		}

		public Builder status(ResultStatus status) {
			this.status = status;
			return this;
		}

		public QueryResult build() {
			return new QueryResult(messages != null ? List.copyOf(messages) : List.of(), metadata, status);
		}

	}
}