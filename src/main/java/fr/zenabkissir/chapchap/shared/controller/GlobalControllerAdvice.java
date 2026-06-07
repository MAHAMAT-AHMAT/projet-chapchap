package fr.zenabkissir.chapchap.shared.controller;

import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.Role;
import fr.zenabkissir.chapchap.user.service.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("userPays")
    public Pays userPays(@AuthenticationPrincipal CustomUserDetails user) {
        return user != null ? user.getPays() : null;
    }

    @ModelAttribute("userRole")
    public Role userRole(@AuthenticationPrincipal CustomUserDetails user) {
        return user != null ? user.getRole() : null;
    }
}
