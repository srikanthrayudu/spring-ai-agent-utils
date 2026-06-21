/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.agent.ikos.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Remediation Executor Tool — AI-agent-callable remediation actions.
 *
 * <p>Each method is a Spring AI {@code @Tool} that can be invoked by the
 * AI agent to execute identity governance remediation actions. In this
 * hackathon implementation, actions are <b>simulated</b> with realistic
 * cloud API response formats.
 *
 * <h3>Production MCP Integration Path</h3>
 * <pre>
 * ┌──────────────┐    ┌──────────────┐    ┌────────────────────┐
 * │  IKOS Agent   │───▶│  Spring AI   │───▶│  MCP Server        │
 * │  (decides)    │    │  Tool Call   │    │  (aws-iam, okta,   │
 * │               │    │              │    │   azure-ad, snow)  │
 * └──────────────┘    └──────────────┘    └────────┬───────────┘
 *                                                   │
 *                          ┌────────────────────────┼────────────────┐
 *                          ▼                        ▼                ▼
 *                   ┌──────────┐           ┌──────────┐      ┌──────────┐
 *                   │ AWS IAM  │           │  Okta    │      │ Azure AD │
 *                   │ API      │           │  API     │      │ Graph API│
 *                   └──────────┘           └──────────┘      └──────────┘
 * </pre>
 *
 * <p>To connect to real platforms, replace the simulated responses with:
 * <ul>
 *   <li>AWS: {@code software.amazon.awssdk:iam} SDK calls</li>
 *   <li>Okta: {@code com.okta.sdk:okta-sdk-api} calls</li>
 *   <li>Azure AD: Microsoft Graph API via REST client</li>
 *   <li>ServiceNow: REST API table operations</li>
 * </ul>
 *
 * @author Antigravity
 */
public class RemediationExecutorTool {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /** Audit log of all executed actions. Each entry: [txId, actionType, target, platform, details, timestamp]. */
    private final List<String[]> auditLog = new ArrayList<>();

    // ── Account Lifecycle ───────────────────────────────────────────────────

    @Tool(name = "DisableAccount",
          description = "Disable a user account on a specific platform. " +
                        "Revokes login access while preserving audit data. " +
                        "Applicable to: ActiveDirectory, AWS_IAM, Okta, Salesforce, ServiceNow.")
    public String disableAccount(
            @ToolParam(description = "Username or account ID to disable") String accountId,
            @ToolParam(description = "Target platform (ActiveDirectory, AWS_IAM, Okta, Salesforce, ServiceNow)") String platform,
            @ToolParam(description = "Reason for disabling (e.g. offboarding, dormant, policy violation)") String reason) {

        String txId = generateTxId("DISABLE");
        String ts = LocalDateTime.now().format(TS);

        // Simulate platform-specific API call
        String apiEndpoint;
        String responseCode;
        if ("AWS_IAM".equals(platform)) {
            apiEndpoint = "iam.amazonaws.com/UpdateLoginProfile";
            responseCode = "200 OK — LoginProfile disabled";
        } else if ("Okta".equals(platform)) {
            apiEndpoint = "dev-org.okta.com/api/v1/users/" + accountId + "/lifecycle/suspend";
            responseCode = "200 OK — User suspended";
        } else if ("ActiveDirectory".equals(platform)) {
            apiEndpoint = "graph.microsoft.com/v1.0/users/" + accountId;
            responseCode = "204 No Content — accountEnabled=false";
        } else if ("Salesforce".equals(platform)) {
            apiEndpoint = "org.salesforce.com/services/data/v58.0/sobjects/User/" + accountId;
            responseCode = "200 OK — IsActive=false";
        } else {
            apiEndpoint = platform.toLowerCase() + ".api/users/" + accountId + "/disable";
            responseCode = "200 OK — Account disabled";
        }

        logAction(txId, "DISABLE_ACCOUNT", accountId, platform, reason);

        return String.format(
                "{ \"status\": \"SUCCESS\", \"txId\": \"%s\", \"action\": \"DISABLE_ACCOUNT\", " +
                "\"target\": \"%s\", \"platform\": \"%s\", \"api\": \"%s\", " +
                "\"response\": \"%s\", \"timestamp\": \"%s\", " +
                "\"auditNote\": \"Account disabled per SOC directive. Reason: %s\" }",
                txId, accountId, platform, apiEndpoint, responseCode, ts, reason);
    }

    // ── API Key / Token Revocation ──────────────────────────────────────────

    @Tool(name = "RevokeApiKey",
          description = "Revoke and invalidate an API key or access token. " +
                        "Immediately terminates all sessions using this credential. " +
                        "Supports AWS access keys, Okta API tokens, and service account keys.")
    public String revokeApiKey(
            @ToolParam(description = "API key ID or token identifier to revoke") String keyId,
            @ToolParam(description = "Target platform (AWS_IAM, Okta, ServiceNow)") String platform,
            @ToolParam(description = "Account or service identity that owns the key") String owner) {

        String txId = generateTxId("REVOKE");
        String ts = LocalDateTime.now().format(TS);

        String apiEndpoint;
        String responseCode;
        if ("AWS_IAM".equals(platform)) {
            apiEndpoint = "iam.amazonaws.com/DeleteAccessKey?AccessKeyId=" + keyId;
            responseCode = "200 OK — AccessKey deleted";
        } else if ("Okta".equals(platform)) {
            apiEndpoint = "dev-org.okta.com/api/v1/api-tokens/" + keyId + "/revoke";
            responseCode = "204 No Content — Token revoked";
        } else {
            apiEndpoint = platform.toLowerCase() + ".api/credentials/" + keyId + "/revoke";
            responseCode = "200 OK — Credential revoked";
        }

        logAction(txId, "REVOKE_API_KEY", keyId, platform, "Owner: " + owner);

        return String.format(
                "{ \"status\": \"SUCCESS\", \"txId\": \"%s\", \"action\": \"REVOKE_API_KEY\", " +
                "\"keyId\": \"%s\", \"platform\": \"%s\", \"owner\": \"%s\", " +
                "\"api\": \"%s\", \"response\": \"%s\", \"timestamp\": \"%s\", " +
                "\"activeSessions\": 0, \"auditNote\": \"All active sessions terminated\" }",
                txId, keyId, platform, owner, apiEndpoint, responseCode, ts);
    }

    // ── Credential Rotation ─────────────────────────────────────────────────

    @Tool(name = "RotateCredentials",
          description = "Rotate credentials for a service account or user. " +
                        "Generates new credentials, invalidates old ones, and updates the secrets vault. " +
                        "Enforces rotation policy (default: 90-day cycle).")
    public String rotateCredentials(
            @ToolParam(description = "Service account or user ID") String accountId,
            @ToolParam(description = "Target platform (AWS_IAM, Okta, ActiveDirectory)") String platform,
            @ToolParam(description = "Rotation policy in days (e.g. 90)") int rotationDays) {

        String txId = generateTxId("ROTATE");
        String ts = LocalDateTime.now().format(TS);
        String newKeyId = "AKIA" + UUID.randomUUID().toString().substring(0, 16).toUpperCase().replace("-", "");
        String nextRotation = LocalDateTime.now().plusDays(rotationDays).format(TS);

        logAction(txId, "ROTATE_CREDENTIALS", accountId, platform,
                "New key: " + newKeyId + ", next rotation: " + nextRotation);

        return String.format(
                "{ \"status\": \"SUCCESS\", \"txId\": \"%s\", \"action\": \"ROTATE_CREDENTIALS\", " +
                "\"accountId\": \"%s\", \"platform\": \"%s\", " +
                "\"oldKeyStatus\": \"INVALIDATED\", \"newKeyId\": \"%s\", " +
                "\"rotationPolicy\": \"%d days\", \"nextRotation\": \"%s\", " +
                "\"vaultUpdated\": true, \"timestamp\": \"%s\" }",
                txId, accountId, platform, newKeyId, rotationDays, nextRotation, ts);
    }

    // ── Privilege Revocation ────────────────────────────────────────────────

    @Tool(name = "RevokePrivilege",
          description = "Remove a specific admin privilege or role from an identity. " +
                        "Used for SoD violations, over-privileged accounts, and privilege creep remediation.")
    public String revokePrivilege(
            @ToolParam(description = "User or identity ID") String identityId,
            @ToolParam(description = "Platform to revoke privilege on") String platform,
            @ToolParam(description = "Role or privilege to revoke (e.g. Administrator, S3FullAccess)") String privilege,
            @ToolParam(description = "Replacement role (e.g. ReadOnly, Viewer) or 'none'") String replacementRole) {

        String txId = generateTxId("REVOKE-PRIV");
        String ts = LocalDateTime.now().format(TS);

        String apiEndpoint;
        if ("AWS_IAM".equals(platform)) {
            apiEndpoint = "iam.amazonaws.com/DetachUserPolicy?PolicyArn=" + privilege;
        } else if ("ActiveDirectory".equals(platform)) {
            apiEndpoint = "graph.microsoft.com/v1.0/groups/{admin-group}/members/$ref";
        } else {
            apiEndpoint = platform.toLowerCase() + ".api/roles/" + privilege + "/revoke";
        }

        logAction(txId, "REVOKE_PRIVILEGE", identityId, platform,
                "Revoked: " + privilege + ", replacement: " + replacementRole);

        return String.format(
                "{ \"status\": \"SUCCESS\", \"txId\": \"%s\", \"action\": \"REVOKE_PRIVILEGE\", " +
                "\"identity\": \"%s\", \"platform\": \"%s\", " +
                "\"revokedRole\": \"%s\", \"newRole\": \"%s\", " +
                "\"api\": \"%s\", \"timestamp\": \"%s\", " +
                "\"breakGlassEnabled\": true, " +
                "\"auditNote\": \"Privilege revoked per NIST AC-6(1). Break-glass retained for emergency.\" }",
                txId, identityId, platform, privilege, replacementRole, apiEndpoint, ts);
    }

    // ── SSO Session Invalidation ────────────────────────────────────────────

    @Tool(name = "InvalidateSessions",
          description = "Terminate all active SSO sessions for an identity across all platforms. " +
                        "Forces re-authentication on next access attempt.")
    public String invalidateSessions(
            @ToolParam(description = "User or identity ID") String identityId,
            @ToolParam(description = "Comma-separated platforms or 'ALL' for all platforms") String platforms) {

        String txId = generateTxId("SESSION");
        String ts = LocalDateTime.now().format(TS);
        int sessionsKilled = 3 + (int) (Math.random() * 5);

        logAction(txId, "INVALIDATE_SESSIONS", identityId, platforms,
                sessionsKilled + " sessions terminated");

        return String.format(
                "{ \"status\": \"SUCCESS\", \"txId\": \"%s\", \"action\": \"INVALIDATE_SESSIONS\", " +
                "\"identity\": \"%s\", \"platforms\": \"%s\", " +
                "\"sessionsTerminated\": %d, \"oauthTokensRevoked\": %d, " +
                "\"samlAssertionsInvalidated\": true, \"timestamp\": \"%s\", " +
                "\"auditNote\": \"All active sessions force-terminated\" }",
                txId, identityId, platforms, sessionsKilled, sessionsKilled, ts);
    }

    // ── ITSM Ticket Creation ────────────────────────────────────────────────

    @Tool(name = "CreateRemediationTicket",
          description = "Create an ITSM ticket (ServiceNow) for remediation actions " +
                        "that require manual review or multi-step approval workflows.")
    public String createRemediationTicket(
            @ToolParam(description = "Short description of the remediation task") String summary,
            @ToolParam(description = "Priority: P1-Critical, P2-High, P3-Medium, P4-Low") String priority,
            @ToolParam(description = "Assigned SOC team or individual") String assignee) {

        String txId = generateTxId("TICKET");
        String ticketId = "INC" + String.format("%07d", (int) (Math.random() * 9999999));
        String ts = LocalDateTime.now().format(TS);

        logAction(txId, "CREATE_TICKET", ticketId, "ServiceNow",
                priority + " — " + summary);

        return String.format(
                "{ \"status\": \"SUCCESS\", \"txId\": \"%s\", \"action\": \"CREATE_TICKET\", " +
                "\"ticketId\": \"%s\", \"platform\": \"ServiceNow\", " +
                "\"summary\": \"%s\", \"priority\": \"%s\", " +
                "\"assignee\": \"%s\", \"state\": \"NEW\", " +
                "\"sla\": \"%s\", \"timestamp\": \"%s\" }",
                txId, ticketId, summary.replace("\"", "'"), priority, assignee,
                "P1".equals(priority) ? "1 hour" : "P2".equals(priority) ? "4 hours" : "24 hours",
                ts);
    }

    // ── Audit & State ───────────────────────────────────────────────────────

    /**
     * Returns a copy of the full audit log for compliance reporting.
     * Each entry is [txId, actionType, target, platform, details, timestamp].
     */
    public List<String[]> getAuditLog() {
        return List.copyOf(auditLog);
    }

    /** Returns the number of actions executed in this session. */
    public int getActionCount() {
        return auditLog.size();
    }

    // Audit log accessors for individual fields
    public static String txId(String[] rec) { return rec[0]; }
    public static String actionType(String[] rec) { return rec[1]; }
    public static String target(String[] rec) { return rec[2]; }
    public static String platform(String[] rec) { return rec[3]; }
    public static String details(String[] rec) { return rec[4]; }
    public static String timestamp(String[] rec) { return rec[5]; }
    public static String formatRecord(String[] rec) {
        return String.format("[%s] %s → %s on %s (%s)", rec[0], rec[1], rec[2], rec[3], rec[4]);
    }

    // ── Internal Helpers ────────────────────────────────────────────────────

    private String generateTxId(String prefix) {
        return "TX-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void logAction(String txId, String actionType, String target, String platform, String details) {
        auditLog.add(new String[]{txId, actionType, target, platform, details, LocalDateTime.now().format(TS)});
    }
}
