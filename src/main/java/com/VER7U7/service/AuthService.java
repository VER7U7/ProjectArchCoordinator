package com.VER7U7.service;

import com.VER7U7.auth.JwtService;
import com.VER7U7.dto.Result;
import com.VER7U7.dto.ResultStatus;
import com.VER7U7.dto.TokensData;
import com.VER7U7.exceptions.*;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.models.RefreshToken;
import com.VER7U7.repo.PlayerAccountRepository;
import com.VER7U7.repo.RefreshTokenRepository;
import com.VER7U7.websock.sessions.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private PlayerAccountRepository playerAccountRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final SessionManager sessionManager;


    public AuthService(JwtService jwtService, SessionManager sessionManager) {
        this.jwtService = jwtService;
        this.sessionManager = sessionManager;

    }

    public PlayerAccount CreateAccount(String name, String password) throws BadCredentialException {
        Optional<PlayerAccount> account = playerAccountRepository.findByPlayerNickName(name);
        if (account.isPresent())
            throw new BadCredentialException(BadCredentialType.LoginIsExists);

        if (name == null || name.length() < 3)
            throw new BadCredentialException(BadCredentialType.BadLogin);

        if (password == null || password.length() < 8)
            throw new BadCredentialException(BadCredentialType.BadPassword);

        String encodedPassword = passwordEncoder.encode(password);

        PlayerAccount newAccount = new PlayerAccount(
                name, encodedPassword
        );

        playerAccountRepository.save(newAccount);

        return newAccount;
    }

    public PlayerAccount loginAccount(String name, String password) {
        Optional<PlayerAccount> account = playerAccountRepository.findByPlayerNickName(name);
        if (account.isEmpty())
            return null;

        if (!passwordEncoder.matches(password, account.get().getPassword()))
            return null;

        WebSocketSession session = sessionManager.getPlayerSession(account.get().getId());
        sessionManager.kick(session, new ResultStatus("auth_another_login"));

        return account.get();
    }

    public PlayerAccount validateRefreshToken(String token) throws IOException, OldTokenException, BadTokenException, AccountNotExists {
        String playerId = jwtService.validateRefreshTokenAndGetPlayerId(token);
        if (playerId == null || playerId.isEmpty())
            throw new BadTokenException();

        Optional<PlayerAccount> account = playerAccountRepository.findById(Long.parseLong(playerId));

        if (account.isEmpty())
            throw new AccountNotExists();

        List<RefreshToken> allTokens = refreshTokenRepository.findByOwnerAccount(account.get());
        Optional<RefreshToken> refreshToken = allTokens.stream()
                .filter(n -> n.getRefreshToken().equals(token))
                .findFirst();

        if (refreshToken.isEmpty())
            throw new BadTokenException();

        if (refreshToken.get().isUsed()) {
            WebSocketSession session = sessionManager.getPlayerSession(account.get().getId());
            sessionManager.kick(session, new ResultStatus("auth_failed_old_token"));

            throw new OldTokenException();
        }

        refreshToken.get().setUsed(true);
        refreshTokenRepository.save(refreshToken.get());

        return account.get();
    }

    public TokensData createSession(WebSocketSession session, PlayerAccount account) {

        TokensData tokens = createTokens(account);

        session.getAttributes().put("playerId", account.getId());
        session.getAttributes().put("accessToken", tokens.accessToken());

        if (!sessionManager.validateSession(session))
            sessionManager.registerPlayer(account.getId(), session);

        return tokens;
    }

    public TokensData createTokens(PlayerAccount playerAccount) {
        String accessToken = jwtService.generateAccessToken(playerAccount.getId().toString());
        String refreshToken = jwtService.generateRefreshToken(playerAccount.getId().toString());
        TokensData tokensData = new TokensData(accessToken, refreshToken);

        RefreshToken token = new RefreshToken(refreshToken, playerAccount, false);
        refreshTokenRepository.save(token);
        return tokensData;
    }

    public boolean validateAccessToken(WebSocketSession session, String token) {
        String playerId = jwtService.validateAccessToken(token);

        if (playerId != null) {
            if (!session.getAttributes().containsKey("playerId")) {
                Long pId = Long.parseLong(playerId);
                session.getAttributes().put("playerId", pId);
                session.getAttributes().put("accessToken", token);
                sessionManager.registerPlayer(pId, session);
            }
            return true;
        }
        return false;
    }
}
