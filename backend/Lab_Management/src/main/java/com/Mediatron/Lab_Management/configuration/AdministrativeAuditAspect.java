package com.Mediatron.Lab_Management.configuration;

import com.Mediatron.Lab_Management.authentication.UserPrincipal;
import com.Mediatron.Lab_Management.data_dto.APIResponse;
import com.Mediatron.Lab_Management.services.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Aspect
@Component
public class AdministrativeAuditAspect {

    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    public AdministrativeAuditAspect(AuditLogService auditLogService, HttpServletRequest request) {
        this.auditLogService = auditLogService;
        this.request = request;
    }

    @Around(
            "within(com.Mediatron.Lab_Management.controllers..*) && (" +
                    "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
                    "@annotation(org.springframework.web.bind.annotation.DeleteMapping))"
    )
    public Object auditAdministrativeMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String entityName = resolveEntityName(controllerName);
        if (entityName == null) {
            return result;
        }

        Integer actorId = extractActorId();
        if (actorId == null) {
            return result;
        }

        String action = resolveAction(request.getMethod(), entityName);
        Integer targetId = extractTargetId(joinPoint.getArgs(), result);
        auditLogService.logAction(actorId, action, targetId);
        return result;
    }

    private Integer extractActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }

        return null;
    }

    private String resolveAction(String httpMethod, String entityName) {
        String verb = switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> httpMethod;
        };
        return verb + "_" + entityName;
    }

    private String resolveEntityName(String controllerName) {
        return switch (controllerName) {
            case "MemberController" -> "MEMBER";
            case "EventController" -> "EVENT";
            case "ProjectController" -> "PROJECT";
            case "PhDStudentController" -> "PHD_STUDENT";
            case "UserController" -> "USER";
            default -> null;
        };
    }

    private Integer extractTargetId(Object[] args, Object result) {
        for (Object arg : args) {
            Integer id = extractId(arg);
            if (id != null) {
                return id;
            }
        }
        return extractId(result);
    }

    private Integer extractId(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue.intValue();
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof ResponseEntity<?> responseEntity) {
            return extractId(responseEntity.getBody());
        }
        if (value instanceof APIResponse<?> apiResponse) {
            return extractId(apiResponse.getData());
        }
        if (value instanceof Map<?, ?> map) {
            Integer fromId = extractId(map.get("id"));
            if (fromId != null) {
                return fromId;
            }
            Integer fromUserId = extractId(map.get("userId"));
            if (fromUserId != null) {
                return fromUserId;
            }
            return extractId(map.get("targetId"));
        }

        Integer reflectedId = invokeGetter(value, "getId");
        if (reflectedId != null) {
            return reflectedId;
        }

        Integer reflectedUserId = invokeGetter(value, "getUserId");
        if (reflectedUserId != null) {
            return reflectedUserId;
        }

        return invokeGetter(value, "getTargetId");
    }

    private Integer invokeGetter(Object value, String methodName) {
        try {
            Method method = value.getClass().getMethod(methodName);
            Object result = method.invoke(value);
            return extractId(result);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
