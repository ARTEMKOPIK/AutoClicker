## 2025-01-24 - Sensitive Data Leak in Logs
**Vulnerability:** User-created scripts and scheduled task details (stored as JSON) were being explicitly included in error logs when data corruption was detected. These logs could be sent to external services (like Telegram) via the `CrashHandler`.
**Learning:** Developers sometimes include too much context in error messages for debugging purposes, forgetting that this context might contain sensitive user data or secrets.
**Prevention:** Always sanitize error messages to remove any data that could contain sensitive information. Use only non-sensitive metadata (like data length or error types) for debugging in production logs.

## 2025-01-24 - Hardcoded Keystore in Repository
**Vulnerability:** A Base64 encoded JKS keystore was committed to the repository root.
**Learning:** Keystore files or their encoded versions are sometimes accidentally committed or left in the working directory during manual signing processes.
**Prevention:** Ensure `.gitignore` correctly covers all sensitive file extensions (`.jks`, `.keystore`, etc.) and verify that no sensitive files are tracked by git before pushing. Use secure environment variables or secret managers for CI/CD signing.
