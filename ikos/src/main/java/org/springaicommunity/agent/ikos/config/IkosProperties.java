/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.config;

import java.util.Map;

/**
 * Externalized configuration for IKOS risk policies and operational parameters.
 *
 * <p>Replaces hardcoded values in risk engines with tunable thresholds.
 * In Spring Boot, this is populated from {@code application.yml} via
 * {@code @ConfigurationProperties("ikos")}.
 *
 * @author Antigravity
 */
public class IkosProperties {

    private String storagePath;
    private PolicyConfig policies = new PolicyConfig();
    private ScanConfig scan = new ScanConfig();
    private AdvisorConfig advisor = new AdvisorConfig();
    private AuditConfig audit = new AuditConfig();
    private ApiConfig api = new ApiConfig();

    // ── Getters / Setters ────────────────────────────────────────────────

    public String getStoragePath() {
        return storagePath != null ? storagePath
                : System.getProperty("user.home") + "/.ikos";
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public PolicyConfig getPolicies() { return policies; }
    public void setPolicies(PolicyConfig policies) { this.policies = policies; }

    public ScanConfig getScan() { return scan; }
    public void setScan(ScanConfig scan) { this.scan = scan; }

    public AdvisorConfig getAdvisor() { return advisor; }
    public void setAdvisor(AdvisorConfig advisor) { this.advisor = advisor; }

    public AuditConfig getAudit() { return audit; }
    public void setAudit(AuditConfig audit) { this.audit = audit; }

    public ApiConfig getApi() { return api; }
    public void setApi(ApiConfig api) { this.api = api; }

    // ── Policy Configuration ─────────────────────────────────────────────

    /**
     * Configurable risk detection thresholds.
     *
     * <pre>
     * ikos:
     *   policies:
     *     dormant-admin-threshold-days: 90
     *     cross-platform-admin-min-platforms: 3
     *     privilege-creep-max-permissions: 30
     *     privilege-creep-max-sensitive: 5
     *     stale-service-account-days: 180
     *     offboarding-max-hours: 24
     *     credential-rotation-api-token-days: 365
     *     credential-rotation-service-account-days: 90
     *     severity-overrides:
     *       OFFBOARDING_GAP: CRITICAL
     *       DORMANT_ADMIN: HIGH
     * </pre>
     */
    public static class PolicyConfig {
        private int dormantAdminThresholdDays = 90;
        private int crossPlatformAdminMinPlatforms = 3;
        private int privilegeCreepMaxPermissions = 30;
        private int privilegeCreepMaxSensitive = 5;
        private int staleServiceAccountDays = 180;
        private int offboardingMaxHours = 24;
        private int credentialRotationApiTokenDays = 365;
        private int credentialRotationServiceAccountDays = 90;
        private Map<String, String> severityOverrides = Map.of();

        public int getDormantAdminThresholdDays() { return dormantAdminThresholdDays; }
        public void setDormantAdminThresholdDays(int v) { this.dormantAdminThresholdDays = v; }

        public int getCrossPlatformAdminMinPlatforms() { return crossPlatformAdminMinPlatforms; }
        public void setCrossPlatformAdminMinPlatforms(int v) { this.crossPlatformAdminMinPlatforms = v; }

        public int getPrivilegeCreepMaxPermissions() { return privilegeCreepMaxPermissions; }
        public void setPrivilegeCreepMaxPermissions(int v) { this.privilegeCreepMaxPermissions = v; }

        public int getPrivilegeCreepMaxSensitive() { return privilegeCreepMaxSensitive; }
        public void setPrivilegeCreepMaxSensitive(int v) { this.privilegeCreepMaxSensitive = v; }

        public int getStaleServiceAccountDays() { return staleServiceAccountDays; }
        public void setStaleServiceAccountDays(int v) { this.staleServiceAccountDays = v; }

        public int getOffboardingMaxHours() { return offboardingMaxHours; }
        public void setOffboardingMaxHours(int v) { this.offboardingMaxHours = v; }

        public int getCredentialRotationApiTokenDays() { return credentialRotationApiTokenDays; }
        public void setCredentialRotationApiTokenDays(int v) { this.credentialRotationApiTokenDays = v; }

        public int getCredentialRotationServiceAccountDays() { return credentialRotationServiceAccountDays; }
        public void setCredentialRotationServiceAccountDays(int v) { this.credentialRotationServiceAccountDays = v; }

        public Map<String, String> getSeverityOverrides() { return severityOverrides; }
        public void setSeverityOverrides(Map<String, String> v) { this.severityOverrides = v; }
    }

    // ── Scan Configuration ───────────────────────────────────────────────

    /**
     * <pre>
     * ikos:
     *   scan:
     *     enabled: true
     *     cron: "0 0 2 * * *"
     *     identity-count: 500
     * </pre>
     */
    public static class ScanConfig {
        private boolean enabled = false;
        private String cron = "0 0 2 * * *";
        private int identityCount = 500;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }

        public String getCron() { return cron; }
        public void setCron(String v) { this.cron = v; }

        public int getIdentityCount() { return identityCount; }
        public void setIdentityCount(int v) { this.identityCount = v; }
    }

    // ── Advisor Configuration ────────────────────────────────────────────

    /**
     * <pre>
     * ikos:
     *   advisor:
     *     max-units: 10
     *     order: 100
     * </pre>
     */
    public static class AdvisorConfig {
        private int maxUnits = 10;
        private int order = 100;

        public int getMaxUnits() { return maxUnits; }
        public void setMaxUnits(int v) { this.maxUnits = v; }

        public int getOrder() { return order; }
        public void setOrder(int v) { this.order = v; }
    }

    // ── Audit Configuration ──────────────────────────────────────────────

    /**
     * <pre>
     * ikos:
     *   audit:
     *     enabled: true
     *     retention-days: 365
     * </pre>
     */
    public static class AuditConfig {
        private boolean enabled = true;
        private int retentionDays = 365;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int v) { this.retentionDays = v; }
    }

    // ── API Configuration ────────────────────────────────────────────────

    /**
     * <pre>
     * ikos:
     *   api:
     *     enabled: true
     *     base-path: /api/ikos
     * </pre>
     */
    public static class ApiConfig {
        private boolean enabled = true;
        private String basePath = "/api/ikos";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }

        public String getBasePath() { return basePath; }
        public void setBasePath(String v) { this.basePath = v; }
    }

}
