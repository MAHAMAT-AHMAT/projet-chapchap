package fr.zenabkissir.chapchap.transaction.service;

import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface FactureService {
    byte[] genererFacture(List<Transaction> transactions, Pays pays,
                           LocalDate dateDebut, LocalDate dateFin);
}
