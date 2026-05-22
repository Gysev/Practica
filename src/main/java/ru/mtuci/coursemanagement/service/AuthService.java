package ru.mtuci.coursemanagement.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.mtuci.coursemanagement.config.JwtProperties;
import ru.mtuci.coursemanagement.dto.RefreshRequest;
import ru.mtuci.coursemanagement.dto.LoginRequest;
import ru.mtuci.coursemanagement.dto.RegisterRequest;
import ru.mtuci.coursemanagement.dto.TokenResponse;
import ru.mtuci.coursemanagement.model.AppUser;
import ru.mtuci.coursemanagement.model.RefreshToken;
import ru.mtuci.coursemanagement.repository.AppUserRepository;
import ru.mtuci.coursemanagement.repository.RefreshTokenRepository;
import ru.mtuci.coursemanagement.security.JwtService;
import ru.mtuci.coursemanagement.util.TokenHasher;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshRepository;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь уже существует");
        }
        AppUser user = new AppUser(req.username(), passwordEncoder.encode(req.password()), req.role());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(req.username(), req.password()));
        AppUser user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        String hash = TokenHasher.sha256Hex(req.refreshToken());
        RefreshToken stored = refreshRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Недействительный refresh"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshRepository.delete(stored);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh истёк");
        }
        AppUser user = stored.getUser();
        Long userId = user.getId();
        refreshRepository.delete(stored);
        AppUser freshUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return issueTokens(freshUser);
    }

    private TokenResponse issueTokens(AppUser user) {
        String access = jwtService.createAccessToken(user);
        String rawRefresh = newRefreshTokenValue();
        String hash = TokenHasher.sha256Hex(rawRefresh);
        Instant exp = Instant.now().plus(jwtProperties.refreshTtl());
        refreshRepository.save(new RefreshToken(hash, exp, user));
        long seconds = jwtProperties.accessTtl().toSeconds();
        return new TokenResponse(access, rawRefresh, seconds);
    }

    private static String newRefreshTokenValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
