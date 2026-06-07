package fr.zenabkissir.chapchap.simulateur.chatbot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulateur")
public class ChatbotController {

    private final GeminiService geminiService;

    public ChatbotController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "").strip();
        if (question.isEmpty()) {
            return Map.of("reponse", "Veuillez poser une question.");
        }
        return Map.of("reponse", geminiService.chat(question));
    }
}
