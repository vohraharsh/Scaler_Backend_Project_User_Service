package com.scaler.harsh.userservice.service;

import com.scaler.harsh.userservice.dto.CreateUserRequestDTO;
import com.scaler.harsh.userservice.exceptions.ServiceException;
import com.scaler.harsh.userservice.kafka.UserEventProducer;
import com.scaler.harsh.userservice.model.Address;
import com.scaler.harsh.userservice.model.Name;
import com.scaler.harsh.userservice.model.PasswordResetToken;
import com.scaler.harsh.userservice.model.User;
import com.scaler.harsh.userservice.repository.PasswordResetTokenRepository;
import com.scaler.harsh.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserEventProducer userEventProducer;

    private SelfUserService selfUserService;

    @BeforeEach
    void setUp() {
        selfUserService = new SelfUserService(userRepository, passwordResetTokenRepository, passwordEncoder, userEventProducer);
    }

    private User buildUser(Integer id, String username, String email, String hashedPassword) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(hashedPassword);
        return user;
    }

    // ---------- getUserById ----------

    @Test
    void getUserById_found_returnsUser() {
        User user = buildUser(1, "alice", "alice@example.com", "hash");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        User result = selfUserService.getUserById(1);

        assertEquals("alice", result.getUsername());
    }

    @Test
    void getUserById_notFound_throws404() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ServiceException ex = assertThrows(ServiceException.class, () -> selfUserService.getUserById(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- getAllUsers ----------

    @Test
    void getAllUsers_delegatesToRepository() {
        List<User> users = Arrays.asList(buildUser(1, "a", "a@x.com", "h1"), buildUser(2, "b", "b@x.com", "h2"));
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = selfUserService.getAllUsers();

        assertEquals(2, result.size());
    }

    // ---------- createUser ----------

    @Test
    void createUser_hashesPasswordBeforeSaving() {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("bob");
        request.setEmail("bob@example.com");
        request.setPassword("plainPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = selfUserService.createUser(request);

        assertEquals("hashedPassword", result.getPassword());
        verify(passwordEncoder, times(1)).encode("plainPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashedPassword", captor.getValue().getPassword());
        assertNotEquals("plainPassword", captor.getValue().getPassword());
    }

    @Test
    void createUser_withNoNameOrAddress_savesWithNullsNotCrash() {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("minimal");
        request.setEmail("minimal@example.com");
        request.setPassword("pw");

        when(passwordEncoder.encode("pw")).thenReturn("hashedpw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = selfUserService.createUser(request);

        assertNull(result.getName());
        assertNull(result.getAddress());
    }

    @Test
    void createUser_withNameAndAddress_mapsBothOntoUser() {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("full");
        request.setEmail("full@example.com");
        request.setPassword("pw");
        Name name = new Name();
        name.setFirstname("Full");
        name.setLastname("User");
        request.setName(name);
        Address address = new Address();
        address.setCity("City");
        request.setAddress(address);

        when(passwordEncoder.encode("pw")).thenReturn("hashedpw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = selfUserService.createUser(request);

        assertNotNull(result.getName());
        assertEquals("Full", result.getName().getFirstname());
        assertNotNull(result.getAddress());
        assertEquals("City", result.getAddress().getCity());
    }

    @Test
    void createUser_publishesUserRegisteredEventWithSavedUserFields() {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("eventuser");
        request.setEmail("eventuser@example.com");
        request.setPassword("pw");

        when(passwordEncoder.encode("pw")).thenReturn("hashedpw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42);
            return u;
        });

        selfUserService.createUser(request);

        verify(userEventProducer, times(1))
                .publishUserRegistered(42, "eventuser", "eventuser@example.com");
    }

    @Test
    void createUser_kafkaPublishFails_doesNotPropagateException() {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("failuser");
        request.setEmail("failuser@example.com");
        request.setPassword("pw");

        when(passwordEncoder.encode("pw")).thenReturn("hashedpw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Kafka broker unreachable"))
                .when(userEventProducer).publishUserRegistered(any(), anyString(), anyString());

        User result = assertDoesNotThrow(() -> selfUserService.createUser(request));

        assertEquals("failuser", result.getUsername());
    }

    // ---------- updateUser ----------

    @Test
    void updateUser_notFound_throws404() {
        when(userRepository.findById(5)).thenReturn(Optional.empty());

        CreateUserRequestDTO request = new CreateUserRequestDTO();
        ServiceException ex = assertThrows(ServiceException.class, () -> selfUserService.updateUser(5, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateUser_blankPassword_doesNotReHashOrTouchExistingHash() {
        User existing = buildUser(1, "alice", "alice@example.com", "originalHash");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword(""); // blank — must not overwrite

        User result = selfUserService.updateUser(1, request);

        assertEquals("originalHash", result.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateUser_nullPassword_doesNotReHash() {
        User existing = buildUser(1, "alice", "alice@example.com", "originalHash");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword(null);

        User result = selfUserService.updateUser(1, request);

        assertEquals("originalHash", result.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateUser_newPasswordProvided_reHashes() {
        User existing = buildUser(1, "alice", "alice@example.com", "originalHash");
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("newPassword");

        User result = selfUserService.updateUser(1, request);

        assertEquals("newHash", result.getPassword());
    }

    @Test
    void updateUser_nullNameAndAddressInRequest_preservesExisting() {
        User existing = buildUser(1, "alice", "alice@example.com", "hash");
        Name existingName = new Name();
        existingName.setFirstname("Alice");
        existing.setName(existingName);
        Address existingAddress = new Address();
        existingAddress.setCity("OldCity");
        existing.setAddress(existingAddress);

        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("alice-updated");
        request.setEmail("alice@example.com");
        // name/address left null — must not wipe existing profile data

        User result = selfUserService.updateUser(1, request);

        assertEquals("alice-updated", result.getUsername());
        assertEquals("Alice", result.getName().getFirstname());
        assertEquals("OldCity", result.getAddress().getCity());
    }

    // ---------- deleteUser ----------

    @Test
    void deleteUser_existing_deletesSuccessfully() {
        when(userRepository.existsById(1)).thenReturn(true);

        selfUserService.deleteUser(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void deleteUser_notFound_throws404() {
        when(userRepository.existsById(99)).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class, () -> selfUserService.deleteUser(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(userRepository, never()).deleteById(any());
    }

    // ---------- forgotPassword ----------

    @Test
    void forgotPassword_userNotFound_throws404() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfUserService.forgotPassword("missing@example.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void forgotPassword_userFound_savesTokenWithFutureExpiry() {
        User user = buildUser(1, "alice", "alice@example.com", "hash");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(user);

        selfUserService.forgotPassword("alice@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertEquals(1, saved.getUserId());
        assertNotNull(saved.getToken());
        assertTrue(saved.getExpiryDate().isAfter(LocalDateTime.now()));
    }

    // ---------- resetPassword ----------

    @Test
    void resetPassword_invalidToken_throws400() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfUserService.resetPassword("bad-token", "newPw"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void resetPassword_expiredToken_throws400() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUserId(1);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(token);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfUserService.resetPassword("expired-token", "newPw"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void resetPassword_valid_hashesNewPasswordAndDeletesToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("good-token");
        token.setUserId(1);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        when(passwordResetTokenRepository.findByToken("good-token")).thenReturn(token);

        User user = buildUser(1, "alice", "alice@example.com", "oldHash");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPw")).thenReturn("newHash");

        selfUserService.resetPassword("good-token", "newPw");

        assertEquals("newHash", user.getPassword());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    // ---------- verifyCredentials ----------

    @Test
    void verifyCredentials_userNotFound_throws401WithGenericMessage() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfUserService.verifyCredentials("ghost", "anyPassword"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void verifyCredentials_wrongPassword_throws401WithGenericMessage() {
        User user = buildUser(1, "alice", "alice@example.com", "storedHash");
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrongPassword", "storedHash")).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> selfUserService.verifyCredentials("alice", "wrongPassword"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void verifyCredentials_correctPassword_returnsUser() {
        User user = buildUser(1, "alice", "alice@example.com", "storedHash");
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("correctPassword", "storedHash")).thenReturn(true);

        User result = selfUserService.verifyCredentials("alice", "correctPassword");

        assertEquals("alice", result.getUsername());
    }
}