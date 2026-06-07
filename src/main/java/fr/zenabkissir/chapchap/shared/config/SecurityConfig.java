package fr.zenabkissir.chapchap.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/simulateur/chat"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/fonts/**", "/uploads/**").permitAll()
                        .requestMatchers("/login", "/error", "/simulateur", "/api/simulateur/chat").permitAll()

                        // Administration : ADMIN uniquement
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Rejeter : ADMIN uniquement
                        .requestMatchers(HttpMethod.POST, "/transactions/*/rejeter").hasRole("ADMIN")
                        // Valider : ADMIN + USER
                        .requestMatchers(HttpMethod.POST, "/transactions/*/valider").hasAnyRole("ADMIN", "USER")

                        // Formulaires de création / modification : ADMIN + USER
                        .requestMatchers(HttpMethod.GET,
                                "/transactions/nouveau",
                                "/transactions/*/modifier").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST,
                                "/transactions/nouveau",
                                "/transactions/*/modifier",
                                "/transactions/*/supprimer").hasAnyRole("ADMIN", "USER")

                        // Dashboard + liste : tous les authentifiés
                        .requestMatchers("/transactions/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/transactions/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
