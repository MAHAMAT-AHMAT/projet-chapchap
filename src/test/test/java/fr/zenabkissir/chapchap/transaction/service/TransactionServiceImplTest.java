package fr.zenabkissir.chapchap.transaction.service;

import fr.zenabkissir.chapchap.personne.repository.PersonneRepository;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.transaction.dto.TransactionDTO;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import fr.zenabkissir.chapchap.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PersonneRepository personneRepository;

    @InjectMocks
    private TransactionServiceImpl service;

    @Test
    void modifier_doitLeverIllegalStateException_quandStatutNonEnAttente() {
        Transaction transaction = new Transaction();
        transaction.setStatut(TransactionStatus.EN_ATTENTE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThrows(IllegalStateException.class,
                () -> service.modifier(1L, new TransactionDTO()));
    }

    @Test
    void modifier_doitLeverIllegalArgumentException_quandTransactionIntrouvable() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.modifier(99L, new TransactionDTO()));
    }
}