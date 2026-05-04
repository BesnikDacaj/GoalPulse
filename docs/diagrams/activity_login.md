# GoalPulse — UML Activity Diagram: "User logs in"

```mermaid
flowchart TD
    A([Start: user submits login form]) --> B[POST /api/v1/auth/login<br/>{email, password}]
    B --> C[SELECT user_id, role_id, password_hash<br/>FROM app_user WHERE email = ?]
    C --> D{Row found and is_active = true?}
    D -- No --> X1[401 Unauthorized<br/>"Invalid credentials"] --> Z([End])
    D -- Yes --> E[BCrypt.checkpw(plain, password_hash)<br/>or PBKDF2 fallback]
    E --> F{Hash matches?}
    F -- No --> X1
    F -- Yes --> G[Build token payload:<br/>sub=user_id, role, exp=now+1h]
    G --> H[Sign with HMAC-SHA256<br/>using GOALPULSE_TOKEN_SECRET]
    H --> I[Generate refresh token]
    I --> J[Return 200 OK<br/>{token, refreshToken, user}]
    J --> K[Browser stores token in localStorage<br/>and sets Authorization header for next calls]
    K --> Z
```

## Notes

- BCrypt is the primary verification path; PBKDF2 is a documented fallback used only when jBCrypt is unavailable on the classpath.
- The token's `exp` claim is enforced server-side on every authenticated request — there is no clock-trust on the client.
- Failed login does **not** record an audit row (audit is for admin actions, not for failed auth, to avoid log poisoning by anonymous attackers).
