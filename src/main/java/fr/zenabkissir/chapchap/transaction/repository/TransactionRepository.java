package fr.zenabkissir.chapchap.transaction.repository;

import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findTop10ByOrderByCreatedAtDesc();
    List<Transaction> findTop10ByDeviseOrderByCreatedAtDesc(Devise devise);

    long countByStatut(TransactionStatus statut);
    long countByStatutAndDevise(TransactionStatus statut, Devise devise);

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t " +
           "WHERE t.statut = :statut AND t.type = :type AND t.devise = :devise " +
           "AND (:dateDebut IS NULL OR t.date >= :dateDebut) " +
           "AND (:dateFin IS NULL OR t.date <= :dateFin)")
    BigDecimal sumMontant(
            @Param("statut") TransactionStatus statut,
            @Param("type") TransactionType type,
            @Param("devise") Devise devise,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}
