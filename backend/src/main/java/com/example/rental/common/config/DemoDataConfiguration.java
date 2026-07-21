package com.example.rental.common.config;

import com.example.rental.auth.entity.Role;
import com.example.rental.auth.entity.RoleName;
import com.example.rental.auth.repository.RoleRepository;
import com.example.rental.auth.service.RefreshSessionService;
import com.example.rental.user.entity.UserStatus;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({DemoDataProperties.class, BootstrapAdminProperties.class})
public class DemoDataConfiguration {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Bean
    @Profile("!prod")
    @ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
    ApplicationRunner enableDemoAccounts(UserAccountRepository repository) {
        return arguments -> {
            List<UserAccount> demoAccounts = new ArrayList<>();
            for (String email : DemoDataProperties.ACCOUNT_EMAILS) {
                repository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(account -> {
                    account.setStatus(UserStatus.ACTIVE);
                    demoAccounts.add(account);
                });
            }
            repository.saveAll(demoAccounts);
        };
    }

    @Bean
    @Profile("prod")
    ApplicationRunner secureProductionAccounts(
        UserAccountRepository repository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        BootstrapAdminProperties bootstrapAdmin,
        RefreshSessionService refreshSessionService
    ) {
        return arguments -> {
            for (String email : DemoDataProperties.ACCOUNT_EMAILS) {
                repository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                    .ifPresent(account ->
                        refreshSessionService.setUserStatusAndRevokeAll(account.getId(), UserStatus.LOCKED)
                    );
            }

            if (repository.existsDistinctByRolesNameAndStatusAndDeletedAtIsNull(RoleName.ADMIN, UserStatus.ACTIVE)) {
                return;
            }
            validateBootstrapAdmin(bootstrapAdmin);

            String email = bootstrapAdmin.email().trim().toLowerCase(Locale.ROOT);
            if (DemoDataProperties.ACCOUNT_EMAILS.contains(email)) {
                throw new IllegalStateException("Bootstrap administrator must not use a known demo email");
            }
            if (repository.existsByEmailIgnoreCase(email)) {
                throw new IllegalStateException("Bootstrap administrator email already belongs to another account");
            }
            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Default ADMIN role is missing"));

            UserAccount administrator = new UserAccount();
            administrator.setEmail(email);
            administrator.setPasswordHash(passwordEncoder.encode(bootstrapAdmin.password()));
            administrator.setFullName(bootstrapAdmin.fullName().trim());
            administrator.markEmailVerified();
            administrator.getRoles().add(adminRole);
            repository.saveAndFlush(administrator);
        };
    }

    private void validateBootstrapAdmin(BootstrapAdminProperties properties) {
        if (!StringUtils.hasText(properties.email())
            || !StringUtils.hasText(properties.password())
            || !StringUtils.hasText(properties.fullName())) {
            throw new IllegalStateException(
                "No active administrator exists; set BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD "
                    + "and BOOTSTRAP_ADMIN_FULL_NAME for the first production startup"
            );
        }
        String email = properties.email().trim();
        String password = properties.password();
        String fullName = properties.fullName().trim();
        if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 255) {
            throw new IllegalStateException("Bootstrap administrator email is invalid");
        }
        if (fullName.length() > 150) {
            throw new IllegalStateException("Bootstrap administrator full name must not exceed 150 characters");
        }
        if (password.length() < 12 || password.length() > 72
            || password.chars().noneMatch(Character::isUpperCase)
            || password.chars().noneMatch(Character::isLowerCase)
            || password.chars().noneMatch(Character::isDigit)
            || password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new IllegalStateException(
                "Bootstrap administrator password must be 12-72 characters and include upper, lower, digit and symbol"
            );
        }
    }
}
