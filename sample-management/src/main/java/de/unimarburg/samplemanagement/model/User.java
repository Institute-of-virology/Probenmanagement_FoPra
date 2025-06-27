package de.unimarburg.samplemanagement.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Entity
@Data
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "username")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;

    @Column(name = "otp_token")
    private String oneTimeToken;

    @Column(name = "otp_token_expiry")
    private Instant oneTimeTokenExpiry;

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

    public void setUsername(String username) {
        this.username = username != null ? username.toLowerCase() : null;
    }
}
