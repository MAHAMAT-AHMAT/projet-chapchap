package fr.zenabkissir.chapchap.transaction.controller;

import fr.zenabkissir.chapchap.transaction.service.RecuService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/transactions")
public class RecuController {

    private final RecuService recuService;

    public RecuController(RecuService recuService) {
        this.recuService = recuService;
    }

    @GetMapping("/{id}/recu")
    public ResponseEntity<byte[]> telechargerRecu(@PathVariable Long id) {
        try {
            byte[] pdf = recuService.genererRecu(id);
            String filename = "recu-TXN-" + String.format("%06d", id) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
