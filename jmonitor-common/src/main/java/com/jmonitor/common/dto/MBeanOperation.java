package com.jmonitor.common.dto;

import java.util.List;

/**
 * An MBean operation signature (Phase 4). No-argument operations can be invoked
 * from the GUI; operations with parameters are shown for reference.
 *
 * @param name           operation name
 * @param returnType     declared return type
 * @param parameterTypes declared parameter types, in order
 * @param description    operation description, may be empty
 */
public record MBeanOperation(
        String name,
        String returnType,
        List<String> parameterTypes,
        String description
) {
}
