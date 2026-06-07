package fr.zenabkissir.chapchap.transaction.entity;

import fr.zenabkissir.chapchap.personne.entity.Personne;
import fr.zenabkissir.chapchap.shared.enums.Canal;
import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor
@ToString(exclude = "preuve")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Devise devise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus statut;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "personne_id")
    private Personne personne;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private Canal canal;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private TransactionPreuve preuve;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
