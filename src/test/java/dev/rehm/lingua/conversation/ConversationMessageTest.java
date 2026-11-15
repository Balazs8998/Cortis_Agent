package dev.rehm.lingua.conversation;

import dev.rehm.lingua.llm.ollama.dto.OllamaMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConversationMessageTest {

    private ConversationMessage conversation;

    @BeforeEach
    void setUp() {
        conversation = new ConversationMessage();
    }

    private OllamaMessageDto createMessage(String content) {
        return new OllamaMessageDto(
                "user",
                null,
                content,
                null
        );
    }

    @Test
    void shouldGenerateUuidWhenConversationIsCreated() {
        UUID uuid = conversation.getUuid();

        assertNotNull(uuid);
    }

    @Test
    void shouldInitializeWithEmptyMessageList() {
        assertTrue(conversation.getMessages().isEmpty());
    }

    @Test
    void shouldAddMessageToConversation() {
        OllamaMessageDto message = createMessage("hello");

        conversation.addMessage(message);

        assertEquals(1, conversation.getMessages().size());
        assertSame(message, conversation.getMessages().getFirst());
    }

    @Test
    void shouldReturnImmutableMessageSnapshot() {
        conversation.addMessage(createMessage("hello"));

        List<OllamaMessageDto> snapshot = conversation.getMessages();

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(createMessage("test"))
        );

        conversation.addMessage(createMessage("second"));

        assertEquals(1, snapshot.size());
        assertEquals(2, conversation.getMessages().size());
    }

    @Test
    void shouldReturnResultFromSerializedAction() {
        String result = conversation.executeSerialized(
                () -> "hello"
        );

        assertEquals("hello", result);
    }

    @Test
    void shouldPropagateExceptionFromSerializedAction() {
        assertThrows(
                IllegalStateException.class,
                () -> conversation.executeSerialized(() -> {
                    throw new IllegalStateException("Test exception");
                })
        );
    }

    @Test
    void shouldSafelyAddMessagesFromMultipleThreads() throws Exception {
        int threadCount = 8;
        int messagesPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startSignal.await();

                    for (int j = 0; j < messagesPerThread; j++) {
                        conversation.addMessage(createMessage("hello"));
                    }

                    return null;
                }));
            }

            startSignal.countDown();

            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }

            assertEquals(
                    threadCount * messagesPerThread,
                    conversation.getMessages().size()
            );

        } finally {
            startSignal.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldExecuteConcurrentActionsSequentially() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAttempting = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(
                    () -> conversation.executeSerialized(() -> {
                        firstEntered.countDown();
                        awaitLatch(releaseFirst);
                        return "first";
                    })
            );

            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));

            Future<String> second = executor.submit(() -> {
                secondAttempting.countDown();

                return conversation.executeSerialized(() -> {
                    secondEntered.countDown();
                    return "second";
                });
            });

            assertTrue(secondAttempting.await(2, TimeUnit.SECONDS));

            // The first action still holds the lock.
            assertFalse(
                    secondEntered.await(200, TimeUnit.MILLISECONDS),
                    "Second action entered before the first finished"
            );

            releaseFirst.countDown();

            assertEquals("first", first.get(2, TimeUnit.SECONDS));
            assertEquals("second", second.get(2, TimeUnit.SECONDS));
            assertEquals(0, secondEntered.getCount());

        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted", e);
        }
    }
}
