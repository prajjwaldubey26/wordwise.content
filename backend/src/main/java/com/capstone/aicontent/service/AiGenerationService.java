package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.GenerationRequest;
import com.capstone.aicontent.dto.QuizQuestion;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AiGenerationService {


private final String provider;
private final String openAiKey;
private final String openAiModel;
private final String anthropicKey;
private final String anthropicModel;
private final String nvidiaKey;
private final String nvidiaModel;

private final RestTemplate http;
private final ObjectMapper mapper;

public AiGenerationService(
        @Value("${ai.provider:mock}") String provider,
        @Value("${ai.openai.api-key:}") String openAiKey,
        @Value("${ai.openai.model:gpt-4o-mini}") String openAiModel,
        @Value("${ai.anthropic.api-key:}") String anthropicKey,
        @Value("${ai.anthropic.model:claude-3-5-haiku-latest}") String anthropicModel,
        @Value("${ai.nvidia.api-key:}") String nvidiaKey,
        @Value("${ai.nvidia.model:meta/llama-3.1-8b-instruct}") String nvidiaModel,
        RestTemplate http,
        ObjectMapper mapper) {

    this.provider = provider.toLowerCase(Locale.ROOT);
    this.openAiKey = openAiKey;
    this.openAiModel = openAiModel;
    this.anthropicKey = anthropicKey;
    this.anthropicModel = anthropicModel;
    this.nvidiaKey = nvidiaKey;
    this.nvidiaModel = nvidiaModel;
    this.http = http;
    this.mapper = mapper;
}

public String generate(GenerationRequest request) {

    String instruction =
            "Write a " + request.targetWordCount()
                    + " word " + request.contentType()
                    + " in a " + request.tone()
                    + " tone about: " + request.prompt()
                    + ". Return only the final writing.";

    String response = callProvider(instruction);

    if (response == null || response.isBlank()) {
        return mockContent(
                request.prompt(),
                request.tone(),
                request.contentType(),
                request.targetWordCount()
        );
    }

    return response.trim();
}

public String summarize(String extractedText, int targetWords) {

    String clipped = clip(extractedText, 12000);

    String prompt =
            "Summarize this chapter in about "
                    + targetWords
                    + " words. Cover the key concepts, relationships, "
                    + "and takeaways in clear student-friendly language."
                    + "\n\nSource:\n"
                    + clipped;

    String response = callProvider(prompt);

    if (response == null || response.isBlank()) {
        return mockSummary(extractedText, targetWords);
    }

    return response.trim();
}

public List<QuizQuestion> quiz(String extractedText) {

    String source = clip(extractedText, 9000);

    String instruction =
            "From this chapter, create exactly 5 multiple-choice questions. "
                    + "Return valid JSON only with this shape: "
                    + "[{\"question\":\"...\","
                    + "\"options\":[\"...\",\"...\",\"...\",\"...\"],"
                    + "\"correctIndex\":0}]. "
                    + "Each item needs exactly four options and "
                    + "correctIndex must be 0-3."
                    + "\n\nChapter:\n"
                    + source;

    List<QuizQuestion> parsed =
            parseQuiz(callProvider(instruction));

    if (parsed.size() == 5) {
        return parsed;
    }

    parsed =
            parseQuiz(
                    callProvider(
                            "Respond with valid JSON only.\n" + instruction
                    )
            );

    if (parsed.size() == 5) {
        return parsed;
    }

    return mockQuiz(extractedText);
}

public String chat(List<Map<String, String>> messages, String preferredModel) {
    String response = callProvider(messages, preferredModel);
    if (response == null || response.isBlank()) {
        String lastUser = messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.get("role")))
                .reduce((a, b) -> b)
                .map(m -> m.get("content"))
                .orElse("your question");
        return mockChatReply(lastUser);
    }
    return response.trim();
}

public List<Map<String, Object>> availableChatModels() {
    List<Map<String, Object>> models = new ArrayList<>();
    models.add(Map.of("id", "mock", "label", "Demo (offline)", "available", true));
    models.add(Map.of("id", "nvidia", "label", "NVIDIA", "available", !nvidiaKey.isBlank()));
    models.add(Map.of("id", "openai", "label", "ChatGPT (OpenAI)", "available", !openAiKey.isBlank()));
    models.add(Map.of("id", "anthropic", "label", "Claude (Anthropic)", "available", !anthropicKey.isBlank()));
    return models;
}

public String defaultChatModel() {
    if (!nvidiaKey.isBlank() && "nvidia".equals(provider)) return "nvidia";
    if (!openAiKey.isBlank() && "openai".equals(provider)) return "openai";
    if (!anthropicKey.isBlank() && "anthropic".equals(provider)) return "anthropic";
    if (!nvidiaKey.isBlank()) return "nvidia";
    if (!openAiKey.isBlank()) return "openai";
    if (!anthropicKey.isBlank()) return "anthropic";
    return "mock";
}

private String callProvider(String prompt) {
    return callProvider(List.of(Map.of("role", "user", "content", prompt)), null);
}

private String callProvider(List<Map<String, String>> messages, String preferredModel) {
    String selected = preferredModel == null || preferredModel.isBlank()
            ? provider
            : preferredModel.toLowerCase(Locale.ROOT);

    try {
        return switch (selected) {
            case "openai" ->
                    openAiKey.isBlank()
                            ? null
                            : callOpenAiCompatible(
                                    messages,
                                    openAiKey,
                                    openAiModel,
                                    "https://api.openai.com/v1/chat/completions"
                            );
            case "nvidia" ->
                    nvidiaKey.isBlank()
                            ? null
                            : callOpenAiCompatible(
                                    messages,
                                    nvidiaKey,
                                    nvidiaModel,
                                    "https://integrate.api.nvidia.com/v1/chat/completions"
                            );
            case "anthropic" ->
                    anthropicKey.isBlank()
                            ? null
                            : callAnthropic(messages);
            default -> null;
        };
    } catch (Exception e) {
        System.out.println("AI provider failed: " + e.getMessage());
        return null;
    }
}

@SuppressWarnings("unchecked")
private String callOpenAiCompatible(
        List<Map<String, String>> messages,
        String apiKey,
        String model,
        String endpoint) {

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(apiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    List<Map<String, String>> payloadMessages = messages.stream()
            .map(m -> Map.of(
                    "role", m.getOrDefault("role", "user"),
                    "content", m.getOrDefault("content", "")
            ))
            .toList();

    Map<String, Object> body = Map.of(
            "model", model,
            "messages", payloadMessages,
            "temperature", 0.55,
            "max_tokens", 1024
    );

    Map<String, Object> response = http.postForObject(
            endpoint,
            new HttpEntity<>(body, headers),
            Map.class
    );

    if (response == null) {
        return null;
    }

    List<Map<String, Object>> choices =
            (List<Map<String, Object>>) response.get("choices");

    if (choices == null || choices.isEmpty()) {
        return null;
    }

    Map<String, Object> message =
            (Map<String, Object>) choices.get(0).get("message");

    if (message == null) {
        return null;
    }

    return (String) message.get("content");
}

@SuppressWarnings("unchecked")
private String callAnthropic(List<Map<String, String>> messages) {

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-api-key", anthropicKey);
    headers.set("anthropic-version", "2023-06-01");
    headers.setContentType(MediaType.APPLICATION_JSON);

    List<Map<String, String>> payloadMessages = messages.stream()
            .filter(m -> {
                String role = m.getOrDefault("role", "user");
                return "user".equals(role) || "assistant".equals(role);
            })
            .map(m -> Map.of(
                    "role", m.get("role"),
                    "content", m.getOrDefault("content", "")
            ))
            .toList();

    Map<String, Object> body = Map.of(
            "model", anthropicModel,
            "max_tokens", 1400,
            "messages", payloadMessages
    );

    Map<String, Object> response = http.postForObject(
            "https://api.anthropic.com/v1/messages",
            new HttpEntity<>(body, headers),
            Map.class
    );

    if (response == null) {
        return null;
    }

    List<Map<String, Object>> content =
            (List<Map<String, Object>>) response.get("content");

    if (content == null || content.isEmpty()) {
        return null;
    }

    return (String) content.get(0).get("text");
}

private String mockChatReply(String question) {
    String topic = question == null || question.isBlank() ? "that" : question.trim();
    if (topic.length() > 120) {
        topic = topic.substring(0, 117) + "...";
    }
    return "Here's a clear take on \"" + topic + "\".\n\n"
            + "The short answer is that it depends on your goal, but a good approach is to start with the basics, "
            + "then build up with a few practical steps.\n\n"
            + "1. Clarify what you want to achieve.\n"
            + "2. Break the problem into smaller parts.\n"
            + "3. Use one simple example to check your understanding.\n\n"
            + "If you want, ask a follow-up and I can go deeper on any part.";
}

private List<QuizQuestion> parseQuiz(String json) {

    if (json == null || json.isBlank()) {
        return List.of();
    }

    try {

        String cleaned =
                json
                        .replaceAll(
                                "(?s)^```(?:json)?\\s*|\\s*```$",
                                ""
                        )
                        .trim();

        List<QuizQuestion> questions =
                mapper.readValue(
                        cleaned,
                        new TypeReference<List<QuizQuestion>>() {}
                );

        return questions
                .stream()
                .filter(
                        q ->
                                q.question() != null
                                        && q.options() != null
                                        && q.options().size() == 4
                                        && q.correctIndex() >= 0
                                        && q.correctIndex() < 4
                )
                .toList();

    } catch (Exception e) {

        return List.of();
    }
}

private String mockContent(
        String prompt,
        String tone,
        String type,
        int words) {

    String topic = sentenceCase(prompt.trim());

    List<String> blocks =
            new ArrayList<>(
                    List.of(
                            topic
                                    + " is an important topic that deserves careful consideration. "
                                    + "A useful starting point is to understand why this subject matters "
                                    + "and how it affects people in practical situations.",

                            "The strongest perspective balances ambition with practical detail. "
                                    + "Instead of relying on broad claims, it connects a clear purpose "
                                    + "to the people, habits, and constraints that shape the outcome.",

                            "A thoughtful approach also leaves room for questions. "
                                    + "Different experiences can reveal different priorities, "
                                    + "and those differences can improve the final result.",

                            "The next step is to turn ideas into practical action. "
                                    + "Progress rarely arrives all at once. "
                                    + "It grows through small decisions that remain aligned "
                                    + "with the original purpose.",

                            "Ultimately, the value of this work lies in the clarity it creates. "
                                    + "When the purpose is visible and the language is understandable, "
                                    + "people can participate with confidence."
                    )
            );

    String opening =
            switch (tone.toLowerCase()) {

                case "formal" ->
                        "This "
                                + type
                                + " examines "
                                + topic
                                + ".";

                case "casual" ->
                        "Let's talk about "
                                + topic
                                + ".";

                case "persuasive" ->
                        "It is time to pay closer attention to "
                                + topic
                                + ".";

                default ->
                        "Here is a clear look at "
                                + topic
                                + ".";
            };

    StringBuilder result =
            new StringBuilder(opening)
                    .append("\n\n");

    int index = 0;

    while (wordCount(result.toString()) < words) {

        result
                .append(blocks.get(index % blocks.size()))
                .append("\n\n");

        index++;
    }

    return trimToWords(
            result.toString(),
            words
    );
}

private String mockSummary(
        String text,
        int targetWords) {

    String normalized =
            text
                    .replaceAll("\\s+", " ")
                    .trim();

    String[] sentences =
            normalized.split(
                    "(?<=[.!?])\\s+"
            );

    StringBuilder summary =
            new StringBuilder(
                    "This chapter presents its main ideas through a connected set of concepts. "
            );

    for (
            int i = 0;
            i < sentences.length
                    && wordCount(summary.toString()) < targetWords;
            i++
    ) {

        String sentence =
                sentences[i].trim();

        if (sentence.length() > 25) {
            summary
                    .append(sentence)
                    .append(' ');
        }
    }

    summary.append(
            "Taken together, these points show how the topic works, "
                    + "why it matters, and what learners should remember "
                    + "when applying it."
    );

    return trimToWords(
            summary.toString(),
            targetWords
    );
}

private List<QuizQuestion> mockQuiz(
        String text) {

    List<String> terms =
            Pattern
                    .compile("[A-Za-z]{5,}")
                    .matcher(text)
                    .results()
                    .map(m -> m.group().toLowerCase())
                    .distinct()
                    .limit(5)
                    .toList();

    while (terms.size() < 5) {

        terms =
                append(
                        terms,
                        "chapter concept "
                                + (terms.size() + 1)
                );
    }

    List<QuizQuestion> questions =
            new ArrayList<>();

    for (int i = 0; i < 5; i++) {

        String answer =
                sentenceCase(terms.get(i));

        questions.add(
                new QuizQuestion(
                        "Which term is discussed in the chapter as a key idea?",
                        List.of(
                                answer,
                                "An unrelated historical event",
                                "A random calculation",
                                "A topic not mentioned in the chapter"
                        ),
                        0
                )
        );
    }

    return questions;
}

private List<String> append(
        List<String> source,
        String addition) {

    List<String> copy =
            new ArrayList<>(source);

    copy.add(addition);

    return copy;
}

private String clip(
        String text,
        int max) {

    if (text == null) {
        return "";
    }

    return text.length() <= max
            ? text
            : text.substring(0, max);
}

private int wordCount(
        String value) {

    if (value == null
            || value.trim().isEmpty()) {

        return 0;
    }

    return value
            .trim()
            .split("\\s+")
            .length;
}

private String trimToWords(
        String value,
        int max) {

    String[] words =
            value
                    .trim()
                    .split("\\s+");

    return String.join(
            " ",
            Arrays.copyOf(
                    words,
                    Math.min(words.length, max)
            )
    );
}

private String sentenceCase(
        String value) {

    if (value == null
            || value.isBlank()) {

        return "Your topic";
    }

    return Character.toUpperCase(
            value.charAt(0)
    ) + value.substring(1);
}


}
