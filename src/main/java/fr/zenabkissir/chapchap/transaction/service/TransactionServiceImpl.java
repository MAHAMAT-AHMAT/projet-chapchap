package fr.zenabkissir.chapchap.transaction.service;

import fr.zenabkissir.chapchap.personne.entity.Personne;
import fr.zenabkissir.chapchap.personne.repository.PersonneRepository;
import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import fr.zenabkissir.chapchap.transaction.dto.TransactionDTO;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import fr.zenabkissir.chapchap.transaction.entity.TransactionPreuve;
import fr.zenabkissir.chapchap.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PersonneRepository personneRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                   PersonneRepository personneRepository) {
        this.transactionRepository = transactionRepository;
        this.personneRepository = personneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findAll(Pays pays, TransactionStatus statut, Devise devise,
                                      LocalDate dateDebut, LocalDate dateFin) {
        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();

        // Pays prend la priorité sur le filtre devise libre (sécurité)
        if (pays != null) {
            Devise d = toDevise(pays);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("devise"), d));
        } else if (devise != null) {
            final Devise d = devise;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("devise"), d));
        }

        if (statut != null) {
            final TransactionStatus s = statut;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("statut"), s));
        }
        if (dateDebut != null) {
            final LocalDate dd = dateDebut;
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), dd));
        }
        if (dateFin != null) {
            final LocalDate df = dateFin;
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), df));
        }

        return transactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Override
    public Transaction creer(TransactionDTO dto) {
        Personne personne = resolvePersonne(dto);

        Transaction transaction = new Transaction();
        transaction.setMontant(dto.getMontant());
        transaction.setDevise(dto.getDevise());
        transaction.setType(dto.getType());
        transaction.setStatut(TransactionStatus.EN_ATTENTE);
        transaction.setPersonne(personne);
        transaction.setDate(dto.getDate());
        transaction.setCanal(dto.getCanal());
        transaction.setDescription(dto.getDescription());

        if (dto.getPreuveFile() != null && !dto.getPreuveFile().isEmpty()) {
            transaction.setPreuve(buildPreuve(dto.getPreuveFile(), transaction));
        }

        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction modifier(Long id, TransactionDTO dto) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));

        if (transaction.getStatut() != TransactionStatus.EN_ATTENTE) {
            throw new IllegalStateException("Seules les transactions EN_ATTENTE peuvent être modifiées");
        }

        Personne personne = resolvePersonne(dto);

        transaction.setMontant(dto.getMontant());
        transaction.setDevise(dto.getDevise());
        transaction.setType(dto.getType());
        transaction.setPersonne(personne);
        transaction.setDate(dto.getDate());
        transaction.setCanal(dto.getCanal());
        transaction.setDescription(dto.getDescription());

        if (dto.getPreuveFile() != null && !dto.getPreuveFile().isEmpty()) {
            TransactionPreuve preuve = transaction.getPreuve();
            if (preuve == null) {
                transaction.setPreuve(buildPreuve(dto.getPreuveFile(), transaction));
            } else {
                String nomFichier = saveFileToDisk(dto.getPreuveFile());
                preuve.setNomFichier(nomFichier);
                preuve.setCheminFichier(nomFichier);
            }
        }

        return transactionRepository.save(transaction);
    }

    @Override
    public void valider(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));
        transaction.setStatut(TransactionStatus.CONFIRMEE);
        transactionRepository.save(transaction);
    }

    @Override
    public void rejeter(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));
        transaction.setStatut(TransactionStatus.REJETEE);
        transactionRepository.save(transaction);
    }

    @Override
    public void supprimer(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calculerSoldes(Pays pays, LocalDate dateDebut, LocalDate dateFin) {
        Map<String, BigDecimal> soldes = new LinkedHashMap<>();

        if (pays == null || pays == Pays.MAROC) {
            BigDecimal entreesMAD = safeSum(TransactionType.ENTREE, Devise.MAD, dateDebut, dateFin);
            BigDecimal sortiesMAD = safeSum(TransactionType.SORTIE, Devise.MAD, dateDebut, dateFin);
            soldes.put("entreesMAD", entreesMAD);
            soldes.put("sortiesMAD", sortiesMAD);
            soldes.put("soldeMAD",   entreesMAD.subtract(sortiesMAD));
        } else {
            soldes.put("entreesMAD", BigDecimal.ZERO);
            soldes.put("sortiesMAD", BigDecimal.ZERO);
            soldes.put("soldeMAD",   BigDecimal.ZERO);
        }

        if (pays == null || pays == Pays.TCHAD) {
            BigDecimal entreesXAF = safeSum(TransactionType.ENTREE, Devise.XAF, dateDebut, dateFin);
            BigDecimal sortiesXAF = safeSum(TransactionType.SORTIE, Devise.XAF, dateDebut, dateFin);
            soldes.put("entreesXAF", entreesXAF);
            soldes.put("sortiesXAF", sortiesXAF);
            soldes.put("soldeXAF",   entreesXAF.subtract(sortiesXAF));
        } else {
            soldes.put("entreesXAF", BigDecimal.ZERO);
            soldes.put("sortiesXAF", BigDecimal.ZERO);
            soldes.put("soldeXAF",   BigDecimal.ZERO);
        }

        return soldes;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatut(TransactionStatus statut, Pays pays) {
        if (pays == null) return transactionRepository.countByStatut(statut);
        return transactionRepository.countByStatutAndDevise(statut, toDevise(pays));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findTop10(Pays pays) {
        if (pays == null) return transactionRepository.findTop10ByOrderByCreatedAtDesc();
        return transactionRepository.findTop10ByDeviseOrderByCreatedAtDesc(toDevise(pays));
    }

    private Devise toDevise(Pays pays) {
        return pays == Pays.MAROC ? Devise.MAD : Devise.XAF;
    }

    private BigDecimal safeSum(TransactionType type, Devise devise,
                                LocalDate dateDebut, LocalDate dateFin) {
        BigDecimal result = transactionRepository.sumMontant(
                TransactionStatus.CONFIRMEE, type, devise, dateDebut, dateFin);
        return result != null ? result : BigDecimal.ZERO;
    }

    private TransactionPreuve buildPreuve(MultipartFile file, Transaction transaction) {
        String nomFichier = saveFileToDisk(file);
        TransactionPreuve preuve = new TransactionPreuve();
        preuve.setNomFichier(nomFichier);
        preuve.setCheminFichier(nomFichier);
        preuve.setTransaction(transaction);
        return preuve;
    }

    private Personne resolvePersonne(TransactionDTO dto) {
        if (dto.getPersonneId() != null) {
            return personneRepository.findById(dto.getPersonneId())
                    .orElseThrow(() -> new IllegalArgumentException("Personne introuvable"));
        }
        if (dto.getNouveauNom() == null || dto.getNouveauNom().isBlank()) {
            throw new IllegalArgumentException("Veuillez sélectionner ou saisir une personne");
        }
        Personne p = new Personne();
        p.setNom(dto.getNouveauNom().trim());
        p.setTelephone(dto.getNouveauTelephone());
        return personneRepository.save(p);
    }

    private String saveFileToDisk(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(uploadPath);
            String nomFichier = UUID.randomUUID() + "_" + file.getOriginalFilename();
            file.transferTo(uploadPath.resolve(nomFichier));
            return nomFichier;
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'enregistrement du fichier : " + e.getMessage(), e);
        }
    }
}
