package com.autopecas.autopecas.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converte o token JWT emitido pelo Keycloak em um Authentication do Spring Security.
 * <p>
 * O Keycloak publica os roles no claim "realm_access.roles".
 * Este converter extrai esses roles e os transforma em GrantedAuthority com prefixo "ROLE_",
 * padrão esperado pelo Spring Security para funcionar com hasRole() e @PreAuthorize.
 * <p>
 * Exemplo de payload JWT do Keycloak:
 * {
 * "sub": "uuid-do-usuario",
 * "preferred_username": "joao.mecanico",
 * "realm_access": {
 * "roles": ["MECANICO", "offline_access", "uma_authorization"]
 * },
 * "resource_access": {
 * "autopecas-api": {
 * "roles": ["ROLE_ESPECIFICA_DO_CLIENT"]
 * }
 * }
 * }
 */
@Component
public class KeycloakJwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Converter padrão do Spring — extrai "scope" e "scp" do JWT
     */
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    /**
     * Nome do client do Keycloak (resource_access).
     * Configurado em application.yml: keycloak.client-id
     */
    @Value("${keycloak.client-id:autopecas-api}")
    private String clientId;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultConverter.convert(jwt).stream(),
                extractRealmRoles(jwt).stream()
        ).collect(Collectors.toSet());

        // "sub" do JWT é o identificador único do usuário no Keycloak
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /**
     * Extrai roles de realm_access e resource_access do JWT do Keycloak.
     * Converte para o padrão ROLE_NOME esperado pelo Spring Security.
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        // 1. Roles do realm (valem para toda a aplicação)
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> realmRoles = realmAccess != null
                ? (List<String>) realmAccess.getOrDefault("roles", Collections.emptyList())
                : Collections.emptyList();

        // 2. Roles específicas do client (resource_access.<clientId>.roles)
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        List<String> clientRoles = Collections.emptyList();
        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
            clientRoles = (List<String>) clientAccess.getOrDefault("roles", Collections.emptyList());
        }

        // Combina realm + client roles, filtra roles internas do Keycloak e adiciona prefixo ROLE_
        return Stream.concat(realmRoles.stream(), clientRoles.stream())
                .filter(role -> !isKeycloakInternalRole(role))
                .map(String::toUpperCase)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Ignora roles internas do Keycloak que não têm semântica na aplicação
     */
    private boolean isKeycloakInternalRole(String role) {
        return role.startsWith("default-roles-")
                || role.equals("offline_access")
                || role.equals("uma_authorization");
    }
}