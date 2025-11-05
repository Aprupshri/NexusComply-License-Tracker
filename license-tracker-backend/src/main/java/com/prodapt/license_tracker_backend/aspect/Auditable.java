package com.prodapt.license_tracker_backend.aspect;

import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    EntityType entityType();
    AuditAction action();
}
