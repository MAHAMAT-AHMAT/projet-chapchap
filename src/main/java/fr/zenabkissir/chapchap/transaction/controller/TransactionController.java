package fr.zenabkissir.chapchap.transaction.controller;

import fr.zenabkissir.chapchap.personne.service.PersonneService;
import fr.zenabkissir.chapchap.shared.enums.Canal;
import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import fr.zenabkissir.chapchap.transaction.dto.TransactionDTO;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import fr.zenabkissir.chapchap.transaction.service.TransactionService;
import fr.zenabkissir.chapchap.user.service.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final PersonneService personneService;

    public TransactionController(TransactionService transactionService,
                                  PersonneService personneService) {
        this.transactionService = transactionService;
        this.personneService = personneService;
    }

    // ─── DASHBOARD ───────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            Model model) {

        Pays pays = currentUser != null ? currentUser.getPays() : null;
        Map<String, BigDecimal> soldes = transactionService.calculerSoldes(pays, dateDebut, dateFin);

        model.addAttribute("nbEnAttente",  transactionService.countByStatut(TransactionStatus.EN_ATTENTE, pays));
        model.addAttribute("nbConfirmees", transactionService.countByStatut(TransactionStatus.CONFIRMEE, pays));
        model.addAttribute("nbRejetees",   transactionService.countByStatut(TransactionStatus.REJETEE, pays));
        model.addAttribute("entreesMAD",   soldes.get("entreesMAD"));
        model.addAttribute("sortiesMAD",   soldes.get("sortiesMAD"));
        model.addAttribute("soldeMAD",     soldes.get("soldeMAD"));
        model.addAttribute("entreesXAF",   soldes.get("entreesXAF"));
        model.addAttribute("sortiesXAF",   soldes.get("sortiesXAF"));
        model.addAttribute("soldeXAF",     soldes.get("soldeXAF"));
        model.addAttribute("dernieresTransactions", transactionService.findTop10(pays));
        model.addAttribute("dateDebut", dateDebut);
        model.addAttribute("dateFin",   dateFin);

        return "dashboard";
    }

    // ─── LISTE ───────────────────────────────────────────────────────
    @GetMapping
    public String liste(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String devise,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            Model model) {

        Pays pays = currentUser != null ? currentUser.getPays() : null;

        TransactionStatus statutEnum = (statut != null && !statut.isBlank())
                ? TransactionStatus.valueOf(statut) : null;
        Devise deviseEnum = (devise != null && !devise.isBlank())
                ? Devise.valueOf(devise) : null;

        List<Transaction> transactions =
                transactionService.findAll(pays, statutEnum, deviseEnum, dateDebut, dateFin);

        model.addAttribute("transactions",   transactions);
        model.addAttribute("statuts",        TransactionStatus.values());
        model.addAttribute("devises",        devisesForPays(pays));
        model.addAttribute("selectedStatut", statut);
        model.addAttribute("selectedDevise", devise);
        model.addAttribute("dateDebut",      dateDebut);
        model.addAttribute("dateFin",        dateFin);

        return "liste";
    }

    // ─── NOUVELLE TRANSACTION ────────────────────────────────────────
    @GetMapping("/nouveau")
    public String nouveauForm(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        Pays pays = currentUser != null ? currentUser.getPays() : null;
        TransactionDTO dto = new TransactionDTO();
        if (pays != null) dto.setDevise(toDevise(pays));

        model.addAttribute("transactionDTO", dto);
        model.addAttribute("personnes",      personneService.findAll());
        model.addAttribute("devises",        devisesForPays(pays));
        model.addAttribute("types",          TransactionType.values());
        model.addAttribute("canaux",         Canal.values());
        model.addAttribute("modeEdition",    false);
        return "formulaire";
    }

    @PostMapping("/nouveau")
    public String creer(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {

        Pays pays = currentUser != null ? currentUser.getPays() : null;
        if (pays != null) dto.setDevise(toDevise(pays));

        if (result.hasErrors()) {
            model.addAttribute("personnes",   personneService.findAll());
            model.addAttribute("devises",     devisesForPays(pays));
            model.addAttribute("types",       TransactionType.values());
            model.addAttribute("canaux",      Canal.values());
            model.addAttribute("modeEdition", false);
            return "formulaire";
        }

        try {
            transactionService.creer(dto);
            redirect.addFlashAttribute("successMessage", "Transaction créée avec succès.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    // ─── MODIFIER ────────────────────────────────────────────────────
    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                Model model, RedirectAttributes redirect) {
        Optional<Transaction> opt = transactionService.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("errorMessage", "Transaction introuvable.");
            return "redirect:/transactions";
        }
        Transaction t = opt.get();

        if (!peutAcceder(currentUser, t)) {
            redirect.addFlashAttribute("errorMessage", "Accès refusé : vous ne pouvez pas modifier cette transaction.");
            return "redirect:/transactions";
        }
        if (t.getStatut() != TransactionStatus.EN_ATTENTE) {
            redirect.addFlashAttribute("errorMessage", "Seules les transactions EN_ATTENTE peuvent être modifiées.");
            return "redirect:/transactions";
        }

        Pays pays = currentUser != null ? currentUser.getPays() : null;
        model.addAttribute("transactionDTO", toDTO(t));
        model.addAttribute("personnes",      personneService.findAll());
        model.addAttribute("devises",        devisesForPays(pays));
        model.addAttribute("types",          TransactionType.values());
        model.addAttribute("canaux",         Canal.values());
        model.addAttribute("modeEdition",    true);
        model.addAttribute("transactionId",  id);
        return "formulaire";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {

        Optional<Transaction> opt = transactionService.findById(id);
        if (opt.isPresent() && !peutAcceder(currentUser, opt.get())) {
            redirect.addFlashAttribute("errorMessage", "Accès refusé.");
            return "redirect:/transactions";
        }

        Pays pays = currentUser != null ? currentUser.getPays() : null;
        if (pays != null) dto.setDevise(toDevise(pays));

        if (result.hasErrors()) {
            model.addAttribute("personnes",    personneService.findAll());
            model.addAttribute("devises",      devisesForPays(pays));
            model.addAttribute("types",        TransactionType.values());
            model.addAttribute("canaux",       Canal.values());
            model.addAttribute("modeEdition",  true);
            model.addAttribute("transactionId", id);
            return "formulaire";
        }

        try {
            transactionService.modifier(id, dto);
            redirect.addFlashAttribute("successMessage", "Transaction modifiée avec succès.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    // ─── VALIDER / REJETER ───────────────────────────────────────────
    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails currentUser,
                          RedirectAttributes redirect) {
        Optional<Transaction> opt = transactionService.findById(id);
        if (opt.isPresent() && !peutAcceder(currentUser, opt.get())) {
            redirect.addFlashAttribute("errorMessage", "Accès refusé : vous ne pouvez pas confirmer cette transaction.");
            return "redirect:/transactions";
        }
        try {
            transactionService.valider(id);
            redirect.addFlashAttribute("successMessage", "Transaction confirmée avec succès.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/rejeter")
    public String rejeter(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            transactionService.rejeter(id);
            redirect.addFlashAttribute("successMessage", "Transaction rejetée.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    // ─── SUPPRIMER ───────────────────────────────────────────────────
    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             RedirectAttributes redirect) {
        Optional<Transaction> opt = transactionService.findById(id);
        if (opt.isPresent() && !peutAcceder(currentUser, opt.get())) {
            redirect.addFlashAttribute("errorMessage", "Accès refusé : vous ne pouvez pas supprimer cette transaction.");
            return "redirect:/transactions";
        }
        try {
            transactionService.supprimer(id);
            redirect.addFlashAttribute("successMessage", "Transaction supprimée.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    // ─── HELPERS ─────────────────────────────────────────────────────

    private boolean peutAcceder(CustomUserDetails user, Transaction t) {
        if (user == null || user.getPays() == null) return true;
        return toDevise(user.getPays()) == t.getDevise();
    }

    private Devise toDevise(Pays pays) {
        return pays == Pays.MAROC ? Devise.MAD : Devise.XAF;
    }

    private Devise[] devisesForPays(Pays pays) {
        if (pays == Pays.MAROC) return new Devise[]{Devise.MAD};
        if (pays == Pays.TCHAD) return new Devise[]{Devise.XAF};
        return Devise.values();
    }

    private TransactionDTO toDTO(Transaction t) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(t.getId());
        dto.setMontant(t.getMontant());
        dto.setDevise(t.getDevise());
        dto.setType(t.getType());
        dto.setPersonneId(t.getPersonne() != null ? t.getPersonne().getId() : null);
        dto.setDate(t.getDate());
        dto.setCanal(t.getCanal());
        dto.setDescription(t.getDescription());
        return dto;
    }
}
