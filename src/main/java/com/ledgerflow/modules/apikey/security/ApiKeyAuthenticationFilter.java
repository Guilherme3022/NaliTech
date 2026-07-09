package com.ledgerflow.modules.apikey.security;

import com.ledgerflow.modules.apikey.entity.ApiKey;
import com.ledgerflow.modules.apikey.repository.ApiKeyRepository;
import com.ledgerflow.modules.apikey.service.ApiKeyService;
import com.ledgerflow.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        if (StringUtils.hasText(rawKey) && SecurityContextHolder.getContext().getAuthentication() == null) {
            apiKeyRepository.findByChaveHashAndAtivoTrue(ApiKeyService.hash(rawKey))
                    .ifPresent(apiKey -> authenticate(apiKey, request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(ApiKey apiKey, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = scopes(apiKey).stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());

        AuthenticatedUser principal = new AuthenticatedUser(
                apiKey.getId(), "apikey:" + apiKey.getNome(), apiKey.getEmpresaId(), List.of(), null);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        apiKey.setUltimoUso(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);
    }

    private List<String> scopes(ApiKey apiKey) {
        if (!StringUtils.hasText(apiKey.getEscopos())) {
            return List.of();
        }
        return Arrays.stream(apiKey.getEscopos().split(",")).map(String::trim).toList();
    }
}
