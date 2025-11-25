package exhibitflow.reservation_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration
 * Enables JPA auditing, repositories, and transaction management
 */
@Configuration
@EnableJpaRepositories(basePackages = "exhibitflow.reservation_service.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
   
}
