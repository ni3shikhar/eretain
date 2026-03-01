package com.eretain.ui.config;

import com.eretain.ui.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/webjars/**", "/images/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/rate-cards/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/company/**").hasAnyRole("ADMINISTRATOR", "PMO")
                .requestMatchers("/projects/bulk-upload", "/projects/bulk-upload/**").hasAnyRole("ADMINISTRATOR", "PMO")
                .requestMatchers("/projects/create", "/projects/edit/**", "/projects/delete/**").hasAnyRole("ADMINISTRATOR", "PMO")
                .requestMatchers("/allocations/create", "/allocations/edit/**", "/allocations/delete/**").hasAnyRole("ADMINISTRATOR", "PMO")
                .requestMatchers("/timesheets/review/**").hasAnyRole("ADMINISTRATOR", "PMO")
                .requestMatchers("/reports/projects", "/reports/allocations", "/reports/timesheets").hasAnyRole("ADMINISTRATOR", "PMO")
                .requestMatchers("/chat", "/chat/**").hasAnyRole("ADMINISTRATOR", "PMO")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            )
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .deleteCookies("JWT_TOKEN")
                .permitAll()
            );

        return http.build();
    }
}
