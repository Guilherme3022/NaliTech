package com.nalitech.modules.audit.service;

import com.nalitech.modules.audit.Audited;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void afterAudited(JoinPoint joinPoint, Audited audited, Object result) {
        String entidadeId = extractId(joinPoint);
        auditLogService.record(audited.action(), audited.entity(), entidadeId);
    }

    private String extractId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] != null && isSimple(args[0])) {
            return args[0].toString();
        }
        return null;
    }

    private boolean isSimple(Object value) {
        return value instanceof java.util.UUID || value instanceof String || value instanceof Number;
    }

    @SuppressWarnings("unused")
    private String methodName(JoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
    }
}
