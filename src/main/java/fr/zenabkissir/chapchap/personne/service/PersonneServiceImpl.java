package fr.zenabkissir.chapchap.personne.service;

import fr.zenabkissir.chapchap.personne.entity.Personne;
import fr.zenabkissir.chapchap.personne.repository.PersonneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PersonneServiceImpl implements PersonneService {

    private final PersonneRepository personneRepository;

    public PersonneServiceImpl(PersonneRepository personneRepository) {
        this.personneRepository = personneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personne> findAll() {
        return personneRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Personne> findById(Long id) {
        return personneRepository.findById(id);
    }

    @Override
    public Personne save(Personne personne) {
        return personneRepository.save(personne);
    }
}
