package fr.zenabkissir.chapchap.personne.service;

import fr.zenabkissir.chapchap.personne.entity.Personne;

import java.util.List;
import java.util.Optional;

public interface PersonneService {
    List<Personne> findAll();
    Optional<Personne> findById(Long id);
    Personne save(Personne personne);
}
