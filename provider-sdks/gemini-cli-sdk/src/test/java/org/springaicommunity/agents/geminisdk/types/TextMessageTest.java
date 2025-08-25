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
import static org.assertj.core.api.Assertions.*;

class TextMessageTest {

	@Test
	void testUserMessage() {
		TextMessage message = TextMessage.user("Hello");

		assertThat(message.getType()).isEqualTo(MessageType.USER);
		assertThat(message.getContent()).isEqualTo("Hello");
		assertThat(message.isEmpty()).isFalse();
	}

	@Test
	void testAssistantMessage() {
		TextMessage message = TextMessage.assistant("Hi there!");

		assertThat(message.getType()).isEqualTo(MessageType.ASSISTANT);
		assertThat(message.getContent()).isEqualTo("Hi there!");
		assertThat(message.isEmpty()).isFalse();
	}

	@Test
	void testSystemMessage() {
		TextMessage message = TextMessage.system("System info");

		assertThat(message.getType()).isEqualTo(MessageType.SYSTEM);
		assertThat(message.getContent()).isEqualTo("System info");
	}

	@Test
	void testErrorMessage() {
		TextMessage message = TextMessage.error("Error occurred");

		assertThat(message.getType()).isEqualTo(MessageType.ERROR);
		assertThat(message.getContent()).isEqualTo("Error occurred");
	}

	@Test
	void testEmptyMessage() {
		TextMessage message = TextMessage.assistant("");

		assertThat(message.isEmpty()).isTrue();

		TextMessage nullMessage = TextMessage.assistant(null);
		assertThat(nullMessage.isEmpty()).isTrue();

		TextMessage whitespaceMessage = TextMessage.assistant("   ");
		assertThat(whitespaceMessage.isEmpty()).isTrue();
	}

	@Test
	void testDefaultValues() {
		TextMessage message = new TextMessage(null, null);

		assertThat(message.getType()).isEqualTo(MessageType.ASSISTANT);
		assertThat(message.getContent()).isEqualTo("");
	}

}