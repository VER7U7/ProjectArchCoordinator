package com.VER7U7.service;


import com.VER7U7.auth.JwtService;
import com.VER7U7.exceptions.BadCredentialException;
import com.VER7U7.exceptions.BadCredentialType;
import com.VER7U7.models.PlayerAccount;
import com.VER7U7.repo.PlayerAccountRepository;
import com.VER7U7.repo.RefreshTokenRepository;
import com.VER7U7.websock.sessions.ClientSessionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ClientAuthServiceTest {

    private ClientAuthService clientAuthService;

    @Mock
    private PlayerAccountRepository playerAccountRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private ClientSessionManager sessionManager;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        clientAuthService = new ClientAuthService(
                playerAccountRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                sessionManager
        );
    }

    @Test
    void createAccount_NormCredentials() {
        String name = "test";
        String password = "testHash";

        Mockito.when(playerAccountRepository.findByPlayerNickName(name))
                .thenReturn(Optional.empty());

        Mockito.when(playerAccountRepository.save(Mockito.any(PlayerAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlayerAccount account = Assertions.assertDoesNotThrow(() -> clientAuthService.createAccount(name, password));

        Assertions.assertNotNull(account);
        Assertions.assertEquals(name, account.getPlayerNickName());
        Assertions.assertTrue(passwordEncoder.matches(password, account.getPassword()));

        // check argument assigned to the save method in playerAccountRepository
        ArgumentCaptor<PlayerAccount> accountCaptor = ArgumentCaptor.forClass(PlayerAccount.class);
        Mockito.verify(playerAccountRepository).save(accountCaptor.capture());

        PlayerAccount captAccount = accountCaptor.getValue();
        Assertions.assertNotNull(captAccount);
        Assertions.assertEquals(name, captAccount.getPlayerNickName());
        Assertions.assertTrue(passwordEncoder.matches(password, captAccount.getPassword()));
    }

    @Test
    void createAccount_LoginIsExists() {
        String name = "test";
        String password = "testHash";

        Mockito.when(playerAccountRepository.findByPlayerNickName(name))
                .thenReturn(Optional.of(new PlayerAccount("name", passwordEncoder.encode(password))));

        Assertions.assertThrows(BadCredentialException.class, () -> clientAuthService.createAccount(name, password));

        Mockito.verify(playerAccountRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void createAccount_TooShortLogin() {
        BadCredentialException ex = Assertions.assertThrows(
                BadCredentialException.class,
                () -> clientAuthService.createAccount("te", "validPassword123")
        );
        Assertions.assertEquals(BadCredentialType.BadLogin, ex.getType());
        Mockito.verifyNoInteractions(playerAccountRepository);
    }

    @Test
    void createAccount_TooShortPassword() {
        BadCredentialException ex = Assertions.assertThrows(
                BadCredentialException.class,
                () -> clientAuthService.createAccount("validLogin", "short")
        );
        Assertions.assertEquals(BadCredentialType.BadPassword, ex.getType());
        Mockito.verifyNoInteractions(playerAccountRepository);
    }
}
