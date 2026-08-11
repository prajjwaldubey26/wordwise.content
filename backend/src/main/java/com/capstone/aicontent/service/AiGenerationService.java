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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AiGenerationService {

private static final String CHAT_SYSTEM_PROMPT = """
        You are WordWise, a helpful AI writing and study coach.
        Answer like ChatGPT: clear, natural, specific to the user's question, and useful.
        Prefer plain text. Do not wrap titles in markdown asterisks or hashtags.
        Structure longer answers with a short direct answer first, then brief sections or numbered steps when helpful.
        Use concrete examples when they improve understanding.
        If the user attaches a PDF or image transcript, use that content carefully and say what you inferred.
        If something is uncertain, say so briefly and ask one focused follow-up.
        Keep a warm, professional tone. Avoid filler phrases and generic advice that ignores the question.
        """;

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
                    + ". Return only the final writing as plain text. "
                    + "Start with a clear title on the first line, then an optional short subtitle on the next line, "
                    + "then a blank line, then the body paragraphs. "
                    + "Do not use Markdown. Do not use asterisks, hashtags, backticks, or bold/italic markers.";

    String response = callProvider(instruction);

    if (response == null || response.isBlank()) {
        return mockContent(
                request.prompt(),
                request.tone(),
                request.contentType(),
                request.targetWordCount()
        );
    }

    return stripMarkdownMarkers(response.trim());
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
    List<Map<String, String>> withSystem = withChatSystemPrompt(messages);
    String selected = preferredModel == null || preferredModel.isBlank()
            ? defaultChatModel()
            : preferredModel.toLowerCase(Locale.ROOT);

    // Only auto-upgrade away from mock when the user left demo mode selected.
    if ("mock".equals(selected)) {
        String live = firstAvailableLiveModel(null);
        if (live != null) {
            selected = live;
        }
    }

    ProviderResult primary = callProviderDetailed(withSystem, selected);
    if (primary.content() != null && !primary.content().isBlank()) {
        return stripMarkdownMarkers(primary.content().trim());
    }

    // If a live model was chosen, never hide failures behind generic mock text.
    if (!"mock".equals(selected)) {
        String detail = primary.error() == null || primary.error().isBlank()
                ? "No response was returned."
                : primary.error();
        return "I couldn't reach " + labelForModel(selected) + " just now.\n\n"
                + detail + "\n\n"
                + "Check that the API key is set on Render ("
                + envHintForModel(selected)
                + "), redeploy the backend, then start a New chat and try again.";
    }

    String lastUser = messages.stream()
            .filter(m -> "user".equalsIgnoreCase(m.get("role")))
            .reduce((a, b) -> b)
            .map(m -> m.get("content"))
            .orElse("your question");
    return mockChatReply(lastUser);
}

private String labelForModel(String model) {
    return switch (model) {
        case "openai" -> "ChatGPT (OpenAI)";
        case "nvidia" -> "NVIDIA";
        case "anthropic" -> "Claude (Anthropic)";
        default -> model;
    };
}

private String envHintForModel(String model) {
    return switch (model) {
        case "openai" -> "OPENAI_API_KEY";
        case "nvidia" -> "NVIDIA_API_KEY";
        case "anthropic" -> "ANTHROPIC_API_KEY";
        default -> "the provider API key";
    };
}

private record ProviderResult(String content, String error) {}

private List<Map<String, String>> withChatSystemPrompt(List<Map<String, String>> messages) {
    List<Map<String, String>> payload = new ArrayList<>();
    payload.add(Map.of("role", "system", "content", CHAT_SYSTEM_PROMPT));
    if (messages != null) {
        for (Map<String, String> message : messages) {
            if (message == null) continue;
            String role = message.getOrDefault("role", "user");
            if ("system".equalsIgnoreCase(role)) continue;
            payload.add(Map.of(
                    "role", role,
                    "content", message.getOrDefault("content", "")
            ));
        }
    }
    return payload;
}

private String firstAvailableLiveModel(String preferredModel) {
    String preferred = preferredModel == null ? "" : preferredModel.toLowerCase(Locale.ROOT);
    List<String> order = new ArrayList<>();
    if (!preferred.isBlank() && !"mock".equals(preferred)) {
        order.add(preferred);
    }
    for (String candidate : List.of("openai", "nvidia", "anthropic")) {
        if (!order.contains(candidate)) {
            order.add(candidate);
        }
    }
    for (String candidate : order) {
        if ("openai".equals(candidate) && !openAiKey.isBlank()) return "openai";
        if ("nvidia".equals(candidate) && !nvidiaKey.isBlank()) return "nvidia";
        if ("anthropic".equals(candidate) && !anthropicKey.isBlank()) return "anthropic";
    }
    return null;
}

public String describeImage(byte[] imageBytes, String mimeType, String preferredModel) {
    if (imageBytes == null || imageBytes.length == 0) {
        return "An empty image was uploaded.";
    }
    String selected = preferredModel == null || preferredModel.isBlank()
            ? provider
            : preferredModel.toLowerCase(Locale.ROOT);
    String mediaType = mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType;
    String dataUrl = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
    String prompt = "Describe this image in clear detail. Include any visible text (OCR), objects, layout, and useful context so another assistant can answer questions about it.";

    try {
        String result = switch (selected) {
            case "openai" -> openAiKey.isBlank() ? null : callOpenAiVision(
                    prompt,
                    dataUrl,
                    openAiKey,
                    openAiModel,
                    "https://api.openai.com/v1/chat/completions"
            );
            case "nvidia" -> nvidiaKey.isBlank() ? null : callOpenAiVision(
                    prompt,
                    dataUrl,
                    nvidiaKey,
                    nvidiaModel,
                    "https://integrate.api.nvidia.com/v1/chat/completions"
            );
            case "anthropic" -> anthropicKey.isBlank() ? null : callAnthropicVision(prompt, imageBytes, mediaType);
            default -> null;
        };
        if (result != null && !result.isBlank()) {
            return result.trim();
        }
    } catch (Exception e) {
        System.out.println("Image description failed: " + e.getMessage());
    }
    return "An image was attached (" + mediaType + "). Visible details could not be extracted automatically; "
            + "answer using the user's question and ask for clarification if needed.";
}

@SuppressWarnings("unchecked")
private String callOpenAiVision(
        String prompt,
        String dataUrl,
        String apiKey,
        String model,
        String endpoint) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(apiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    List<Map<String, Object>> content = new ArrayList<>();
    content.add(Map.of("type", "text", "text", prompt));
    Map<String, Object> imageUrl = new HashMap<>();
    imageUrl.put("url", dataUrl);
    content.add(Map.of("type", "image_url", "image_url", imageUrl));

    Map<String, Object> message = new HashMap<>();
    message.put("role", "user");
    message.put("content", content);

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("messages", List.of(message));
    body.put("temperature", 0.2);
    body.put("max_tokens", 900);

    Map<String, Object> response = http.postForObject(
            endpoint,
            new HttpEntity<>(body, headers),
            Map.class
    );

    if (response == null) {
        return null;
    }
    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
    if (choices == null || choices.isEmpty()) {
        return null;
    }
    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
    return msg == null ? null : (String) msg.get("content");
}

@SuppressWarnings("unchecked")
private String callAnthropicVision(String prompt, byte[] imageBytes, String mediaType) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-api-key", anthropicKey);
    headers.set("anthropic-version", "2023-06-01");
    headers.setContentType(MediaType.APPLICATION_JSON);

    String base64 = Base64.getEncoder().encodeToString(imageBytes);
    List<Map<String, Object>> content = new ArrayList<>();
    Map<String, Object> imageBlock = new HashMap<>();
    imageBlock.put("type", "image");
    imageBlock.put("source", Map.of(
            "type", "base64",
            "media_type", mediaType,
            "data", base64
    ));
    content.add(imageBlock);
    content.add(Map.of("type", "text", "text", prompt));

    Map<String, Object> body = new HashMap<>();
    body.put("model", anthropicModel);
    body.put("max_tokens", 900);
    body.put("messages", List.of(Map.of("role", "user", "content", content)));

    Map<String, Object> response = http.postForObject(
            "https://api.anthropic.com/v1/messages",
            new HttpEntity<>(body, headers),
            Map.class
    );
    if (response == null) {
        return null;
    }
    List<Map<String, Object>> blocks = (List<Map<String, Object>>) response.get("content");
    if (blocks == null || blocks.isEmpty()) {
        return null;
    }
    return (String) blocks.get(0).get("text");
}

public List<Map<String, Object>> availableChatModels() {
    List<Map<String, Object>> models = new ArrayList<>();
    models.add(Map.of("id", "mock", "label", "Demo (offline)", "available", true));
    models.add(Map.of("id", "openai", "label", "ChatGPT (OpenAI)", "available", !openAiKey.isBlank()));
    models.add(Map.of("id", "nvidia", "label", "NVIDIA Llama", "available", !nvidiaKey.isBlank()));
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
    return callProviderDetailed(List.of(Map.of("role", "user", "content", prompt)), null).content();
}

private String callProvider(List<Map<String, String>> messages, String preferredModel) {
    return callProviderDetailed(messages, preferredModel).content();
}

private ProviderResult callProviderDetailed(List<Map<String, String>> messages, String preferredModel) {
    String selected = preferredModel == null || preferredModel.isBlank()
            ? provider
            : preferredModel.toLowerCase(Locale.ROOT);

    try {
        return switch (selected) {
            case "openai" -> {
                if (openAiKey.isBlank()) {
                    yield new ProviderResult(null, "OPENAI_API_KEY is missing on the server.");
                }
                yield new ProviderResult(callOpenAiCompatible(
                        messages,
                        openAiKey,
                        openAiModel,
                        "https://api.openai.com/v1/chat/completions"
                ), null);
            }
            case "nvidia" -> {
                if (nvidiaKey.isBlank()) {
                    yield new ProviderResult(null, "NVIDIA_API_KEY is missing on the server.");
                }
                yield new ProviderResult(callOpenAiCompatible(
                        messages,
                        nvidiaKey,
                        nvidiaModel,
                        "https://integrate.api.nvidia.com/v1/chat/completions"
                ), null);
            }
            case "anthropic" -> {
                if (anthropicKey.isBlank()) {
                    yield new ProviderResult(null, "ANTHROPIC_API_KEY is missing on the server.");
                }
                yield new ProviderResult(callAnthropic(messages), null);
            }
            default -> new ProviderResult(null, "Demo mode does not call a live model.");
        };
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        String snippet = body == null || body.isBlank()
                ? e.getStatusText()
                : body.substring(0, Math.min(240, body.length()));
        System.out.println("AI provider HTTP error: " + e.getStatusCode() + " " + snippet);
        return new ProviderResult(null,
                "Provider returned HTTP " + e.getStatusCode().value() + ": " + snippet);
    } catch (Exception e) {
        System.out.println("AI provider failed: " + e.getMessage());
        return new ProviderResult(null, "Provider error: " + e.getMessage());
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

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("messages", payloadMessages);
    body.put("temperature", 0.7);
    body.put("max_tokens", 1800);

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

    String system = messages.stream()
            .filter(m -> "system".equalsIgnoreCase(m.getOrDefault("role", "")))
            .map(m -> m.getOrDefault("content", ""))
            .filter(content -> !content.isBlank())
            .findFirst()
            .orElse("");

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

    Map<String, Object> body = new HashMap<>();
    body.put("model", anthropicModel);
    body.put("max_tokens", 1800);
    body.put("temperature", 0.7);
    body.put("messages", payloadMessages);
    if (!system.isBlank()) {
        body.put("system", system);
    }

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
    String raw = question == null ? "" : question.trim();
    String lower = raw.toLowerCase(Locale.ROOT);
    String topic = raw.isBlank() ? "your question" : raw;
    if (topic.length() > 160) {
        topic = topic.substring(0, 157) + "...";
    }

    if (lower.matches("^(hi+|hii+|hello|hey|yo|sup|good (morning|afternoon|evening))[!?\\.]*$")) {
        return "Hi! I'm WordWise.\n\n"
                + "I can help you write drafts, explain topics, summarize PDFs, or check originality ideas.\n\n"
                + "What would you like to work on?";
    }

    if (lower.contains("--- begin attached")) {
        return "I read the attached file content you shared.\n\n"
                + "Here is a practical summary:\n"
                + "1. Identify the main topic and purpose of the document.\n"
                + "2. Pull out the key claims, definitions, or steps.\n"
                + "3. Note any examples, formulas, or conclusions that matter most.\n\n"
                + "Ask me to explain any section in simpler language, turn it into notes, "
                + "or quiz you on it, and I will go deeper.";
    }

    if (lower.startsWith("what is") || lower.startsWith("what's") || lower.startsWith("define") || lower.contains("explain")) {
        return "Here is a clear explanation of " + topic + ".\n\n"
                + "In plain terms, it is the idea or process you asked about, described so a student can use it right away.\n\n"
                + "Why it matters:\n"
                + "- It helps you understand the core concept before details pile up.\n"
                + "- It gives you language you can reuse in notes, essays, or exams.\n\n"
                + "A simple way to remember it:\n"
                + "1. State the definition in one sentence.\n"
                + "2. Add one real-world example.\n"
                + "3. Connect it to a related idea you already know.\n\n"
                + "If you want, tell me your level (beginner/intermediate) and I will tailor the depth.";
    }

    if (lower.startsWith("how to") || lower.startsWith("how do") || lower.contains("steps")) {
        return "Here is a practical plan for " + topic + ".\n\n"
                + "1. Clarify the outcome you want in one sentence.\n"
                + "2. Gather the minimum information or materials you need.\n"
                + "3. Do the first small step that creates visible progress.\n"
                + "4. Check the result, then adjust before moving on.\n"
                + "5. Repeat until the draft, solution, or answer feels solid.\n\n"
                + "Common mistake to avoid: jumping to advanced details before the basics are clear.\n\n"
                + "Tell me where you are stuck and I will give the next exact step.";
    }

    if (lower.contains("code") || lower.contains("program") || lower.contains("java") || lower.contains("python")) {
        return "Here is a practical coding-focused answer for " + topic + ".\n\n"
                + "Approach:\n"
                + "1. Restate the problem in your own words.\n"
                + "2. Break it into inputs, process, and expected output.\n"
                + "3. Write the simplest working version first.\n"
                + "4. Test with one normal case and one edge case.\n"
                + "5. Then clean names, structure, and comments.\n\n"
                + "If you paste your current code or error message, I can debug it line by line.";
    }

    return "Here is a direct answer to: " + topic + "\n\n"
            + "Start with the core point: focus on what the question is really asking, then support it with a short explanation and one example.\n\n"
            + "Useful structure:\n"
            + "1. Direct answer in 1-2 sentences.\n"
            + "2. Why that answer makes sense.\n"
            + "3. One example or application.\n"
            + "4. A quick check so you know you understood it.\n\n"
            + "I can go deeper, rewrite this for an exam, or turn it into a short essay draft — tell me which you want.";
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
            new StringBuilder(topic)
                    .append("\n\n")
                    .append(opening)
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

/** Removes common Markdown markers so drafts show clean titles instead of **asterisks**. */
private String stripMarkdownMarkers(String text) {
    if (text == null || text.isBlank()) {
        return text;
    }
    String cleaned = text.replace("\r\n", "\n");
    cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s+", "");
    cleaned = cleaned.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
    cleaned = cleaned.replaceAll("__(.+?)__", "$1");
    cleaned = cleaned.replaceAll("(?m)^\\*\\s+", "");
    cleaned = cleaned.replaceAll("`([^`]+)`", "$1");
    return cleaned.trim();
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
