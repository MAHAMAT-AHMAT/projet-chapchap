package fr.zenabkissir.chapchap.user.service;

import fr.zenabkissir.chapchap.user.dto.UserDTO;
import fr.zenabkissir.chapchap.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAll();
    Optional<User> findById(Long id);
    User creer(UserDTO dto);
    User modifier(Long id, UserDTO dto);
    void supprimer(Long id);
}
