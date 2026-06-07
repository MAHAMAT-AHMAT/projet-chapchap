package fr.zenabkissir.chapchap.transaction.controller;

import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import fr.zenabkissir.chapchap.transaction.service.FactureService;
import fr.zenabkissir.chapchap.transaction.service.TransactionService;
import fr.zenabkissir.chapchap.user.service.CustomUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class FactureController {

    private final TransactionService transactionService;
    private final FactureService factureService;

    public FactureController(TransactionService transactionService,
                              FactureService factureService) {
        this.transactionService = transactionService;
        this.factureService = factureService;
    }

    /**
     * Génère un rapport PDF pour les transactions filtrées.
     * - ADMIN : tous les filtres libres
     * - MANAGER : voit toutes les transactions (pays=null), filtres libres
     * - USER : restreint à son pays (le service filtre automatiquement)
     */
    @GetMapping("/facture")
    public ResponseEntity<byte[]> genererFacture(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String devise,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        Pays pays = currentUser != null ? currentUser.getPays() : null;

        TransactionStatus statutEnum = (statut != null && !statut.isBlank())
                ? TransactionStatus.valueOf(statut) : null;
        Devise deviseEnum = (devise != null && !devise.isBlank())
                ? Devise.valueOf(devise) : null;

        List<Transaction> transactions =
                transactionService.findAll(pays, statutEnum, deviseEnum, dateDebut, dateFin);

        byte[] pdf = factureService.genererFacture(transactions, pays, dateDebut, dateFin);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "facture-" + date + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
