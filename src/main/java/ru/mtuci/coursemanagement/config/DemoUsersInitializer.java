package ru.mtuci.coursemanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mtuci.coursemanagement.model.AppUser;
import ru.mtuci.coursemanagement.model.Role;
import ru.mtuci.coursemanagement.repository.AppUserRepository;

/**
 * Заполняет демо-учётки в профилях без прод-данных (включается {@code app.seed-demo-users=true}).
 */
@Configuration
@RequiredArgsConstructor
public class DemoUsersInitializer {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner seedDemoAccounts(@Value("${app.seed-demo-users:true}") boolean seed) {
        return args -> {
            if (!seed || users.count() > 0) {
                return;
            }
            users.save(new AppUser("teacher", passwordEncoder.encode("password"), Role.TEACHER));
            users.save(new AppUser("student", passwordEncoder.encode("password"), Role.STUDENT));
            users.save(new AppUser("admin", passwordEncoder.encode("password"), Role.ADMIN));
        };
    }
}
