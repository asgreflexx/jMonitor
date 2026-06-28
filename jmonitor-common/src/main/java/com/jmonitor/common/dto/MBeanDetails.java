package com.jmonitor.common.dto;

import java.util.List;

/**
 * Full description of one MBean: its attributes (with current values) and
 * operations (Phase 4).
 *
 * @param objectName  the MBean's object name
 * @param className   the MBean's implementing class
 * @param description the MBean description, may be empty
 * @param attributes  attributes with current values
 * @param operations  operation signatures
 */
public record MBeanDetails(
        String objectName,
        String className,
        String description,
        List<MBeanAttribute> attributes,
        List<MBeanOperation> operations
) {
}
