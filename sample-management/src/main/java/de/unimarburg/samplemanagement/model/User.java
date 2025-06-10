package de.unimarburg.samplemanagement.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Entity
@Data
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;

    /**
     * One-time token for magic link login
     */
    @Column(name = "otp_token")
    private String oneTimeToken;

    /**
     * When the OTP token will expire (epoch timestamp)
     */
    @Column(name = "otp_token_expiry")
    private Instant oneTimeTokenExpiry;

    // Optional: store last login time
    @Column(name = "last_login")
    private Instant lastLogin;

    public boolean isOtpTokenValid(String token) {
        return this.oneTimeToken != null &&
                this.oneTimeToken.equals(token) &&
                this.oneTimeTokenExpiry != null &&
                Instant.now().isBefore(this.oneTimeTokenExpiry);
    }

    public void clearOtpToken() {
        this.oneTimeToken = null;
        this.oneTimeTokenExpiry = null;
    }
}
