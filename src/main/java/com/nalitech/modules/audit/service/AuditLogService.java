package com.nalitech.modules.audit.service;

import com.nalitech.modules.audit.entity.AuditLog;
import com.nalitech.modules.audit.repository.AuditLogRepository;
import com.nalitech.security.AuthenticatedUser;
import com.nalitech.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String acao, String entidade, String entidadeId) {
        AuditLog log = new AuditLog();
        log.setAcao(acao);
        log.setEntidade(entidade == null || entidade.isBlank() ? null : entidade);
        log.setEntidadeId(entidadeId);

        Optional<AuthenticatedUser> user = SecurityUtils.currentUser();
        user.ifPresent(u -> {
            log.setUsuarioId(u.id());
            log.setEmpresaId(u.empresaId());
        });

        currentRequest().ifPresent(request -> {
            log.setIp(clientIp(request));
            log.setUserAgent(truncate(request.getHeader("User-Agent")));
        });

        repository.save(log);
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 300 ? value.substring(0, 300) : value;
    }
}
