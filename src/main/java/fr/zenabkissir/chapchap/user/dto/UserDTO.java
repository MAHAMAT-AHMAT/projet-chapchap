package fr.zenabkissir.chapchap.user.dto;

import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class UserDTO {

    private Long id;

    @NotBlank(message = "L'identifiant est obligatoire")
    private String username;

    private String email;

    private String password;

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    private Pays pays;

    private boolean enabled = true;
}
