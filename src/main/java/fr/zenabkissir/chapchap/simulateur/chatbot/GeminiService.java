package fr.zenabkissir.chapchap.simulateur.chatbot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class GeminiService {

    @Value("${groq.api.url}")
    private String geminiApiUrl;

    @Value("${groq.api.key}")
    private String geminiApiKey;

    private String ragContext;

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("simulateur-rag.txt");
        ragContext = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public String chat(String question) {
        try {
            String url = geminiApiUrl + "/openai/v1/chat/completions";

            String systemPrompt = "Tu es un assistant chatbot intégré au simulateur de transfert ChapChap. "
                + "Tu réponds uniquement aux questions sur les taux de change, les conversions et les calculs du simulateur. "
                + "Si la question ne concerne pas le simulateur, dis poliment que tu ne peux répondre qu'aux questions du simulateur. "
                + "Réponds toujours en français, de façon concise (2-3 phrases maximum).\n\n"
                + "=== DONNÉES DU SIMULATEUR ===\n"
                + ragContext;

            String jsonBody = "{\"model\":\"llama-3.3-70b-versatile\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(question) + "\"}"
                + "]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + geminiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return extractText(response.body());

        } catch (Exception e) {
            return "Désolé, je rencontre une difficulté technique. Veuillez réessayer.";
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractText(String json) {
        // Format Groq/OpenAI : {"choices":[{"message":{"content":"..."}}]}
        int idx = json.indexOf("\"content\":\"");
        if (idx == -1) return "Je n'ai pas pu générer une réponse.";
        int start = idx + 11;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> sb.append(next);
                }
                i++;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
