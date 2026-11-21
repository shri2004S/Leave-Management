package LMS.LeaveManagement.config;

import LMS.LeaveManagement.LeaveManagementApplication;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${SPRING_DATASOURCE_URL:#{null}}")
    private String springDatasourceUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String defaultUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String defaultPassword;

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource() {
        // Ensure .env is loaded if running in test context
        LeaveManagementApplication.loadDotEnv();

        HikariConfig config = new HikariConfig();

        // Check System properties and environment directly if Spring @Value didn't resolve it yet
        String envDbUrl = System.getProperty("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("DATABASE_URL");
        }
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = databaseUrl;
        }

        String rawUrl = (envDbUrl != null && !envDbUrl.isBlank()) ? envDbUrl : springDatasourceUrl;
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getProperty("SPRING_DATASOURCE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("Database configuration error: neither DATABASE_URL nor SPRING_DATASOURCE_URL is specified.");
        }

        String jdbcUrl;
        String username = defaultUsername;
        String password = defaultPassword;

        // Convert standard URI format (postgresql://USER:PASSWORD@HOST/DB?sslmode=require) to JDBC format
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(rawUrl.replace("postgres://", "postgresql://"));
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    path = path.substring(1);
                }

                String query = uri.getQuery();
                StringBuilder jdbcUrlBuilder = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(path);

                // Enforce TLS / sslmode=require for Neon
                if (query == null || query.isBlank()) {
                    jdbcUrlBuilder.append("?sslmode=require");
                } else {
                    if (!query.contains("sslmode=")) {
                        jdbcUrlBuilder.append("?").append(query).append("&sslmode=require");
                    } else {
                        jdbcUrlBuilder.append("?").append(query);
                    }
                }

                // If connecting to PgBouncer pooler, avoid server-side prepared statement collision
                if (host != null && host.contains("-pooler") && !jdbcUrlBuilder.toString().contains("prepareThreshold=")) {
                    jdbcUrlBuilder.append("&prepareThreshold=0");
                }

                jdbcUrl = jdbcUrlBuilder.toString();

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    username = URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8);
                    if (userInfo.length > 1) {
                        password = URLDecoder.decode(userInfo[1], StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse Neon DATABASE_URL: " + e.getMessage(), e);
            }
        } else {
            jdbcUrl = rawUrl;
            if (username == null || username.isBlank()) {
                username = System.getProperty("SPRING_DATASOURCE_USERNAME", System.getenv("SPRING_DATASOURCE_USERNAME"));
            }
            if (password == null || password.isBlank()) {
                password = System.getProperty("SPRING_DATASOURCE_PASSWORD", System.getenv("SPRING_DATASOURCE_PASSWORD"));
            }
        }

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.postgresql.Driver");
        if (username != null && !username.isBlank()) config.setUsername(username);
        if (password != null) config.setPassword(password);

        // Bounded pool settings for Neon cloud database
        config.setPoolName("NeonHikariPool");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);   // 20s fail fast on network drop
        config.setIdleTimeout(300000);         // 5m
        config.setMaxLifetime(1800000);        // 30m
        config.setKeepaliveTime(30000);        // 30s keep-alive ping
        config.setValidationTimeout(5000);     // 5s test query timeout
        config.setConnectionTestQuery("SELECT 1");

        // Log masked URL for security
        log.info("Initialized Neon HikariCP DataSource: {}", maskUrl(jdbcUrl));

        return new HikariDataSource(config);
    }

    private static String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("://[^:@]+:[^@]+@", "://***:***@");
    }
}
