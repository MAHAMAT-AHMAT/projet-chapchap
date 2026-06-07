package fr.zenabkissir.chapchap.transaction.service;

import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.transaction.dto.TransactionDTO;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionService {
    List<Transaction> findAll(Pays pays, TransactionStatus statut, Devise devise, LocalDate dateDebut, LocalDate dateFin);
    Optional<Transaction> findById(Long id);
    Transaction creer(TransactionDTO dto);
    Transaction modifier(Long id, TransactionDTO dto);
    void valider(Long id);
    void rejeter(Long id);
    void supprimer(Long id);
    Map<String, BigDecimal> calculerSoldes(Pays pays, LocalDate dateDebut, LocalDate dateFin);
    long countByStatut(TransactionStatus statut, Pays pays);
    List<Transaction> findTop10(Pays pays);
}
