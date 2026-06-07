package fr.zenabkissir.chapchap.user.controller;

import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.Role;
import fr.zenabkissir.chapchap.user.dto.UserDTO;
import fr.zenabkissir.chapchap.user.entity.User;
import fr.zenabkissir.chapchap.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users/liste";
    }

    @GetMapping("/nouveau")
    public String nouveauForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        model.addAttribute("roles", Role.values());
        model.addAttribute("listePays", Pays.values());
        model.addAttribute("modeEdition", false);
        return "admin/users/formulaire";
    }

    @PostMapping("/nouveau")
    public String creer(@Valid @ModelAttribute("userDTO") UserDTO dto,
                        BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors() || !validatePays(dto, result)) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("listePays", Pays.values());
            model.addAttribute("modeEdition", false);
            return "admin/users/formulaire";
        }
        try {
            userService.creer(dto);
            redirect.addFlashAttribute("successMessage", "Utilisateur créé avec succès.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        return userService.findById(id).map(user -> {
            UserDTO dto = toDTO(user);
            model.addAttribute("userDTO", dto);
            model.addAttribute("roles", Role.values());
            model.addAttribute("listePays", Pays.values());
            model.addAttribute("modeEdition", true);
            return "admin/users/formulaire";
        }).orElseGet(() -> {
            redirect.addFlashAttribute("errorMessage", "Utilisateur introuvable.");
            return "redirect:/admin/users";
        });
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id,
                           @Valid @ModelAttribute("userDTO") UserDTO dto,
                           BindingResult result, Model model,
                           RedirectAttributes redirect) {
        if (result.hasErrors() || !validatePays(dto, result)) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("listePays", Pays.values());
            model.addAttribute("modeEdition", true);
            return "admin/users/formulaire";
        }
        try {
            userService.modifier(id, dto);
            redirect.addFlashAttribute("successMessage", "Utilisateur modifié avec succès.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, Authentication auth,
                            RedirectAttributes redirect) {
        if (auth.getName().equals(userService.findById(id).map(User::getUsername).orElse(""))) {
            redirect.addFlashAttribute("errorMessage", "Vous ne pouvez pas supprimer votre propre compte.");
            return "redirect:/admin/users";
        }
        try {
            userService.supprimer(id);
            redirect.addFlashAttribute("successMessage", "Utilisateur supprimé.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setPays(user.getPays());
        dto.setEnabled(user.isEnabled());
        return dto;
    }

    private boolean validatePays(UserDTO dto, BindingResult result) {
        if (dto.getRole() == Role.USER && dto.getPays() == null) {
            result.rejectValue("pays", "required", "Le pays est obligatoire pour un utilisateur");
            return false;
        }
        return true;
    }
}
