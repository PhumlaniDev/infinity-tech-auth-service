package com.phumlanidev.infinity_tech_auth_service.service.impl;


import com.phumlanidev.infinity_tech_auth_service.dto.LoginDto;
import com.phumlanidev.infinity_tech_auth_service.dto.UserDto;
import com.phumlanidev.infinity_tech_auth_service.enums.RoleMapping;
import com.phumlanidev.infinity_tech_auth_service.exception.auth.AuthenticationFailedException;
import com.phumlanidev.infinity_tech_auth_service.exception.auth.KeycloakCommunicationException;
import com.phumlanidev.infinity_tech_auth_service.mapper.AddressMapper;
import com.phumlanidev.infinity_tech_auth_service.mapper.UserMapper;
import com.phumlanidev.infinity_tech_auth_service.model.Address;
import com.phumlanidev.infinity_tech_auth_service.model.User;
import com.phumlanidev.infinity_tech_auth_service.repository.AddressRepository;
import com.phumlanidev.infinity_tech_auth_service.repository.UserRepository;
import com.phumlanidev.infinity_tech_auth_service.service.IAuthService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Comment: this is the placeholder for documentation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

  private static final String ENABLED_ATTRIBUTE = "enabled";
  private static final String TRUE_VALUE = "true";
  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final PasswordEncoder passwordEncoder;
  private final Keycloak keycloak;
  private final UserMapper userMapper;
  private final AddressMapper addressMapper;

  @Value("${keycloak.auth-server-url}")
  private String keycloakServerUrl;
  @Value("${keycloak.realm}")
  private String keycloakRealm;
  @Value("${keycloak.resource}")
  private String keycloakClientId;
  @Value("${keycloak.credentials.secret}")
  private String keycloakClientSecret;


  /**
   * Comment: this is the placeholder for documentation.
   */
  @Override
  public void registerUser(UserDto userDto) {
    String rawPassword = userDto.getPassword();
    userDto.setPassword(passwordEncoder.encode(rawPassword));

    User user = userMapper.toEntity(userDto, new User());
    Address address = addressMapper.toEntity(userDto.getAddress(), new Address());

    Address savedAddress = addressRepository.save(address);
    user.setAddress(savedAddress);
    userRepository.save(user);

    registerKeycloakUser(userDto, rawPassword);

  }

  private void registerKeycloakUser(UserDto userDto, String rawPassword) {
    try {
      RealmResource realmResource = keycloak.realm(keycloakRealm);
      UsersResource usersResource = realmResource.users();
      UserRepresentation keycloakUser = createUserRepresentation(userDto, rawPassword);

      createAndAssignKeycloakUser(usersResource, realmResource, keycloakUser, userDto);
    } catch (Exception e) {
      log.error("Exception occurred while creating user {} in Keycloak: {}", userDto.getUsername(),
          e.getMessage(), e);
    }
  }


  private void createAndAssignKeycloakUser(UsersResource usersResource, RealmResource realmResource,
                                           UserRepresentation keycloakUser, UserDto userDto) {
    try (Response response = usersResource.create(keycloakUser)) { // Try-with-resources
      if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
        log.info("Keycloak user created successfully for username: {}", userDto.getUsername());
        String userId = getUserIdFromLocation(response.getLocation());
        UserResource userResource = usersResource.get(userId);

        RoleMapping roleMapping = RoleMapping.from(userDto.getRole().toString())
            .orElseThrow(() -> new KeycloakCommunicationException("Invalid role"));

        assignRealmRole(userResource, realmResource, roleMapping.getRealmRole());
        assignClientRole(userResource, realmResource, roleMapping.getClientRole());
      } else {
        log.error("Failed to create Keycloak user: {}", response.getStatusInfo().toString());
        throw new KeycloakCommunicationException("Keycloak user creation failed");
      }
    } catch (NotAuthorizedException e) {
      log.error("Authorization failed during user creation: {}", e.getMessage());
    }
  }

  private String getUserIdFromLocation(URI location) {
    String path = location.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private UserRepresentation createUserRepresentation(UserDto userDto, String rawPassword) {
    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(userDto.getUsername());
    userRepresentation.setEmail(userDto.getEmail());
    userRepresentation.setFirstName(userDto.getFirstName());
    userRepresentation.setLastName(userDto.getLastName());
    userRepresentation.singleAttribute(ENABLED_ATTRIBUTE, TRUE_VALUE);
    userRepresentation.setEnabled(true);
    userRepresentation.isEmailVerified();

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setTemporary(false);
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(rawPassword);
    userRepresentation.setCredentials(Collections.singletonList(credential));
    log.info("Password set for user ID {} in Keycloak", userDto.getUsername());
    return userRepresentation;
  }

  private void assignRealmRole(UserResource userResource, RealmResource realmResource,
                               String roleName) {
    RoleRepresentation realmRole = realmResource.roles().get(roleName).toRepresentation();
    userResource.roles().realmLevel().add(Collections.singletonList(realmRole));
  }

  private void assignClientRole(UserResource userResource, RealmResource realmResource,
                                String clientRoleName) {
    String clientId = keycloakClientId; // Replace with your Keycloak client ID
    ClientResource clientResource = realmResource.clients().get(clientId);
    RoleRepresentation clientRole = clientResource.roles().get(clientRoleName).toRepresentation();
    userResource.roles().clientLevel(clientId).add(Collections.singletonList(clientRole));
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  @Override
  public String login(LoginDto loginDto) {
    try (Keycloak keycloakClient = KeycloakBuilder.builder().serverUrl(keycloakServerUrl)
        .realm(keycloakRealm).clientId(keycloakClientId).clientSecret(keycloakClientSecret)
        .grantType(OAuth2Constants.PASSWORD).username(loginDto.getUsername())
        .password(loginDto.getPassword()).build()) {

      return keycloakClient.tokenManager().getAccessToken().getToken();
    } catch (Exception e) {
      log.error("Exception occurred while logging in user {}: {}", loginDto.getUsername(),
          e.getMessage(), e);
    }
    throw new AuthenticationFailedException("Invalid username or password");
  }
}