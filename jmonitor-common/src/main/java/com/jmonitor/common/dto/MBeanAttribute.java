package com.jmonitor.common.dto;

/**
 * A single MBean attribute with its current value rendered as text (Phase 4).
 *
 * @param name        attribute name
 * @param type        declared Java type
 * @param readable    whether the attribute can be read
 * @param writable    whether the attribute can be written
 * @param description attribute description, may be empty
 * @param value       current value as a display string, or an error marker
 */
public record MBeanAttribute(
        String name,
        String type,
        boolean readable,
        boolean writable,
        String description,
        String value
) {
}
