package fr.zenabkissir.chapchap.transaction.dto;

import fr.zenabkissir.chapchap.shared.enums.Canal;
import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class TransactionDTO {

    private Long id;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotNull(message = "La devise est obligatoire")
    private Devise devise;

    @NotNull(message = "Le type est obligatoire")
    private TransactionType type;

    // Sélection d'une personne existante (optionnel si nouvelle personne saisie)
    private Long personneId;

    // Saisie d'une nouvelle personne
    private String nouveauNom;
    private String nouveauTelephone;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    private Canal canal;
    private String description;
    private MultipartFile preuveFile;
}
