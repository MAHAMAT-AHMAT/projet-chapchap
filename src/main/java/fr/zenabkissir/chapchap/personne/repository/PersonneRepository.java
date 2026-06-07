package fr.zenabkissir.chapchap.personne.repository;

import fr.zenabkissir.chapchap.personne.entity.Personne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonneRepository extends JpaRepository<Personne, Long> {
}
