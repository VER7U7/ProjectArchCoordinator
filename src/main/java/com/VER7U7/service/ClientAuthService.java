package com.VER7U7.service;

import com.VER7U7.auth.JwtService;
import com.VER7U7.dto.client.StatusResponse;
import com.VER7U7.dto.common.TokenPair;
import com.VER7U7.exceptions.*;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.models.RefreshToken;
import com.VER7U7.repo.PlayerAccountRepository;
import com.VER7U7.repo.RefreshTokenRepository;
import com.VER7U7.websock.sessions.ClientSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ClientAuthService {

    private final PlayerAccountRepository playerAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ClientSessionManager sessionManager;

    public ClientAuthService(
            PlayerAccountRepository playerAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            ClientSessionManager sessionManager) {
        this.playerAccountRepository = playerAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionManager = sessionManager;
    }


    /**
     * Creates a new player account with specified username and password.
     *
     * @param name the username of the player account
     * @param password the password of the player account
     * @return the newly created {@link PlayerAccount} instance if all validations are successful
     * @throws BadCredentialException with {@link BadCredentialType#LoginIsExists} if the username is taken,
     *        {@link BadCredentialType#BadLogin} if the username is too short, or
     *        {@link BadCredentialType#BadPassword} if the password does not meet complexity rules
     * */
    public PlayerAccount createAccount(String name, String password) throws BadCredentialException {
        if (name == null || name.length() < 3)
            throw new BadCredentialException(BadCredentialType.BadLogin);

        if (password == null || password.length() < 8)
            throw new BadCredentialException(BadCredentialType.BadPassword);

        Optional<PlayerAccount> account = playerAccountRepository.findByPlayerNickName(name);
        if (account.isPresent())
            throw new BadCredentialException(BadCredentialType.LoginIsExists);

        String encodedPassword = passwordEncoder.encode(password);

        PlayerAccount newAccount = new PlayerAccount(
                name, encodedPassword
        );

        playerAccountRepository.save(newAccount);

        return newAccount;
    }

    /**
     * Authenticates the player account using their username and password.
     * Upon a successful login, any existing sessions for this player are terminated.
     *
     * @param name the username of the player account
     * @param password the password of player account
     * @return the {@link PlayerAccount} instance if authentication is successful
     * @throws AccountNotExists if the player account does not exist in the database
     * @throws BadCredentialException if the password does not match the password from the database
     * */
    public PlayerAccount loginAccount(String name, String password) throws AccountNotExists, BadCredentialException {
        Optional<PlayerAccount> account = playerAccountRepository.findByPlayerNickName(name);
        if (account.isEmpty())
            throw new AccountNotExists();

        if (!passwordEncoder.matches(password, account.get().getPassword()))
            throw new BadCredentialException(BadCredentialType.BadPassword);

        WebSocketSession session = sessionManager.getPlayerSession(account.get().getId());
        sessionManager.kick(session, new StatusResponse("auth_another_login"));

        return account.get();
    }


    /**
     * Validate the refresh token and returns the associated player account.
     *
     * @param token the JWT refresh token issued to the client
     * @return the {@link PlayerAccount} if validation is successful
     * @throws BadTokenException if the token is invalid, malformed, or expired
     * @throws AccountNotExists if the player associated with the token is not found in the database
     * @throws OldTokenException if the token has already been used previously
     * @throws IOException if an I/O error occurs during network or session operations
     * */
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
            sessionManager.kick(session, new StatusResponse("auth_failed_old_token"));

            throw new OldTokenException();
        }

        refreshToken.get().setUsed(true);
        refreshTokenRepository.save(refreshToken.get());

        return account.get();
    }


    /**
     * Initializes a player session with account data and registers it in the {@link ClientSessionManager}.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param account the player account instance from the database
     * @return the {@link TokenPair} containing a pair of newly generated tokens;
     *         if the {@link ClientSessionManager} already contains this session, only the tokens are updated
     * */
    public TokenPair createSession(WebSocketSession session, PlayerAccount account) {

        TokenPair tokens = createTokens(account);

        session.getAttributes().put("playerId", account.getId());
        session.getAttributes().put("accessToken", tokens.accessToken());

        if (!sessionManager.validateSession(session))
            sessionManager.registerPlayer(account.getId(), session);

        return tokens;
    }

    /**
     * Generates a pair of tokens for the player account and persists the refresh token to the database.
     *
     * @param playerAccount the player account instance from the database
     * @return the {@link TokenPair} containing a pair of newly generated tokens
     * */
    public TokenPair createTokens(PlayerAccount playerAccount) {
        String accessToken = jwtService.generateAccessToken(playerAccount.getId().toString());
        String refreshToken = jwtService.generateRefreshToken(playerAccount.getId().toString());
        TokenPair tokensData = new TokenPair(accessToken, refreshToken);

        RefreshToken token = new RefreshToken(refreshToken, playerAccount, false);
        refreshTokenRepository.save(token);
        return tokensData;
    }

    /**
     * Validates the access token. If the session does not contain the {@code playerId} attribute,
     *      binds the player data to the session and registers it in {@link ClientSessionManager}.
     *
     * @param session the {@link WebSocketSession} assigned to the client
     * @param token the JWT access token assigned to the client
     * @return {@code true} if the access token is valid; {@code false} otherwise
     * */
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
