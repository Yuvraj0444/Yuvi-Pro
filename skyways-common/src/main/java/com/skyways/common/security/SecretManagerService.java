package com.skyways.common.security;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Resolves secrets using a four-tier cascade designed for both local development
 * and production cloud deployment:
 *
 *   1. OS / shell environment variable  (export KEY=value  or  load-secrets.ps1)
 *   2. JVM system property              (mvn spring-boot:run -DKEY=value)
 *   3. scripts/secrets.env file         (local dev — searched relative to CWD)
 *   4. GCP Secret Manager               (production only — set skyways.secrets.gcp.enabled=true)
 *
 * GCP is NEVER contacted in local development (enabled defaults to false).
 * Services need no ADC credentials when running locally.
 */
@Service
@ConditionalOnClass(name = "com.google.cloud.secretmanager.v1.SecretManagerServiceClient")
public class SecretManagerService {

    private static final Logger log = LogManager.getLogger(SecretManagerService.class);
    private static final long CACHE_TTL_MINUTES = 60;

    @Value("${gcp.project-id:skyways-airlines-prod}")
    private String projectId;

    /**
     * Set to true ONLY in production / GKE environments where ADC / Workload Identity is configured.
     * Defaults to false — local development never touches GCP.
     */
    @Value("${skyways.secrets.gcp.enabled:false}")
    private boolean gcpEnabled;

    private final Map<String, CachedSecret> cache = new ConcurrentHashMap<>();

    // Parsed secrets.env file contents — loaded once on first access (double-checked locking)
    private volatile Map<String, String> fileSecrets = null;
    private final Object fileLock = new Object();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public String getSecret(String secretName) {
        CachedSecret cached = cache.get(secretName);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }
        String value = resolveSecret(secretName);
        cache.put(secretName, new CachedSecret(value));
        return value;
    }

    public void invalidateCache(String secretName) {
        cache.remove(secretName);
    }

    // -------------------------------------------------------------------------
    // Resolution cascade
    // -------------------------------------------------------------------------

    private String resolveSecret(String secretName) {

        // Tier 1 — OS environment variable
        String value = System.getenv(secretName);
        if (isUsable(value)) {
            log.debug("Secret '{}' resolved from environment variable", secretName);
            return value;
        }

        // Tier 2 — JVM system property  (-DKEY=value on mvn / java command line)
        value = System.getProperty(secretName);
        if (isUsable(value)) {
            log.debug("Secret '{}' resolved from system property (-D flag)", secretName);
            return value;
        }

        // Tier 3 — secrets.env file (local development without cloud credentials)
        value = loadFromFile(secretName);
        if (isUsable(value)) {
            log.debug("Secret '{}' resolved from secrets.env file", secretName);
            return value;
        }

        // Tier 4 — GCP Secret Manager (production only)
        if (gcpEnabled) {
            log.info("Fetching secret '{}' from GCP Secret Manager", secretName);
            return fetchFromGCP(secretName);
        }

        // No source found in local-dev mode
        log.error(
            "Secret '{}' not found. Searched: environment variable, system property (-D), " +
            "scripts/secrets.env file. " +
            "For local dev: run '. .\\scripts\\load-secrets.ps1' in the same terminal, " +
            "or ensure scripts/secrets.env contains {}=<value>. " +
            "For production: set skyways.secrets.gcp.enabled=true and configure ADC.",
            secretName, secretName
        );
        throw new IllegalStateException(
            "Secret '" + secretName + "' not found. " +
            "Add it to scripts/secrets.env for local development, " +
            "or enable GCP Secret Manager with skyways.secrets.gcp.enabled=true."
        );
    }

    // -------------------------------------------------------------------------
    // File-based loading
    // -------------------------------------------------------------------------

    private String loadFromFile(String secretName) {
        return getFileSecrets().getOrDefault(secretName, null);
    }

    private Map<String, String> getFileSecrets() {
        if (fileSecrets == null) {
            synchronized (fileLock) {
                if (fileSecrets == null) {
                    fileSecrets = parseSecretsFiles();
                }
            }
        }
        return fileSecrets;
    }

    private Map<String, String> parseSecretsFiles() {
        // Candidate paths in priority order.
        // Maven runs spring-boot:run with CWD = the service module directory,
        // so "../scripts/secrets.env" resolves to the project-level scripts/ folder.
        List<Path> candidates = List.of(
            Paths.get("scripts", "secrets.env"),                          // running from project root
            Paths.get("..", "scripts", "secrets.env"),                    // running from any service dir
            Paths.get("..", "..", "scripts", "secrets.env"),              // running from a nested module
            Paths.get(System.getProperty("user.home"), ".skyways", "secrets.env") // user-level override
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                log.info("Loading local secrets from: {}", candidate.toAbsolutePath());
                return parseFile(candidate);
            }
        }

        log.debug("No secrets.env file found in standard locations — relying on env vars");
        return Map.of();
    }

    private Map<String, String> parseFile(Path path) {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq < 1) continue;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                if (!value.startsWith("REPLACE_WITH")) {
                    result.put(key, value);
                }
            }
        } catch (IOException e) {
            log.warn("Could not read secrets file {}: {}", path, e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // GCP Secret Manager (production path)
    // -------------------------------------------------------------------------

    private String fetchFromGCP(String secretName) {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName versionName =
                SecretVersionName.of(projectId, secretName, "latest");
            AccessSecretVersionResponse response = client.accessSecretVersion(versionName);
            log.info("Secret '{}' fetched from GCP Secret Manager [project={}]",
                secretName, projectId);
            return response.getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            log.error("GCP Secret Manager fetch failed for '{}': {}", secretName, e.getMessage());
            throw new IllegalStateException(
                "GCP Secret Manager fetch failed for: " + secretName +
                ". Verify ADC credentials (gcloud auth application-default login).", e
            );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }

    // -------------------------------------------------------------------------
    // Cache entry
    // -------------------------------------------------------------------------

    private static final class CachedSecret {
        private final String value;
        private final long expiresAt;

        CachedSecret(String value) {
            this.value = value;
            this.expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES);
        }

        String value()      { return value; }
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}