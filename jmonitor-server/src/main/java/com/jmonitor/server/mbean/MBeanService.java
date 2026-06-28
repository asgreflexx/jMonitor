package com.jmonitor.server.mbean;

import com.jmonitor.common.dto.MBeanAttribute;
import com.jmonitor.common.dto.MBeanDetails;
import com.jmonitor.common.dto.MBeanOperation;
import com.jmonitor.server.process.JvmConnectionManager;
import org.springframework.stereotype.Service;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Browses and interacts with a target JVM's MBeans over JMX (Phase 4):
 * enumerate object names, read attributes, write writable attributes, and
 * invoke no-argument operations.
 */
@Service
public class MBeanService {

    private final JvmConnectionManager connections;

    public MBeanService(JvmConnectionManager connections) {
        this.connections = connections;
    }

    /** All object names, sorted, as canonical strings. */
    public List<String> listNames(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        return conn.queryNames(null, null).stream()
                .map(ObjectName::getCanonicalName)
                .sorted()
                .toList();
    }

    public MBeanDetails details(long pid, String objectName) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        ObjectName name = toObjectName(objectName);
        MBeanInfo info;
        try {
            info = conn.getMBeanInfo(name);
        } catch (Exception e) {
            throw new IOException("Cannot read MBeanInfo for " + objectName + ": " + e.getMessage(), e);
        }

        // Batch-read all readable attributes in a single JMX round trip; the
        // server silently omits any that fail, which we render as unavailable.
        String[] readable = Arrays.stream(info.getAttributes())
                .filter(MBeanAttributeInfo::isReadable)
                .map(MBeanAttributeInfo::getName)
                .toArray(String[]::new);
        Map<String, Object> values = new HashMap<>();
        if (readable.length > 0) {
            try {
                AttributeList list = conn.getAttributes(name, readable);
                for (Attribute attr : list.asList()) {
                    values.put(attr.getName(), attr.getValue());
                }
            } catch (Exception e) {
                throw new IOException("Cannot read attributes of " + objectName + ": " + e.getMessage(), e);
            }
        }

        List<MBeanAttribute> attributes = new ArrayList<>();
        for (MBeanAttributeInfo a : info.getAttributes()) {
            String value;
            if (!a.isReadable()) {
                value = "<not readable>";
            } else if (values.containsKey(a.getName())) {
                value = render(values.get(a.getName()));
            } else {
                value = "<unavailable>";
            }
            attributes.add(new MBeanAttribute(a.getName(), a.getType(),
                    a.isReadable(), a.isWritable(),
                    a.getDescription() == null ? "" : a.getDescription(), value));
        }

        List<MBeanOperation> operations = new ArrayList<>();
        for (MBeanOperationInfo op : info.getOperations()) {
            List<String> params = Arrays.stream(op.getSignature())
                    .map(MBeanParameterInfo::getType)
                    .toList();
            operations.add(new MBeanOperation(op.getName(), op.getReturnType(), params,
                    op.getDescription() == null ? "" : op.getDescription()));
        }
        operations.sort((x, y) -> x.name().compareToIgnoreCase(y.name()));

        return new MBeanDetails(name.getCanonicalName(), info.getClassName(),
                info.getDescription() == null ? "" : info.getDescription(),
                attributes, operations);
    }

    /**
     * Sets a writable attribute, coercing the string value to the declared type.
     *
     * @throws IllegalArgumentException if the attribute is unknown/not writable
     *                                  or the value can't be coerced (→ 400)
     * @throws IOException              if the target can't be reached or the set
     *                                  is rejected by the MBean (→ 502)
     */
    public void setAttribute(long pid, String objectName, String attribute, String value)
            throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        ObjectName name = toObjectName(objectName);

        MBeanInfo info;
        try {
            info = conn.getMBeanInfo(name);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot read MBeanInfo for " + objectName + ": " + e.getMessage(), e);
        }

        // Validation / coercion errors are client errors and propagate as-is.
        String type = Arrays.stream(info.getAttributes())
                .filter(a -> a.getName().equals(attribute) && a.isWritable())
                .map(MBeanAttributeInfo::getType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No writable attribute '" + attribute + "'"));
        Object coerced = coerce(value, type);

        try {
            conn.setAttribute(name, new Attribute(attribute, coerced));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot set attribute " + attribute + ": " + e.getMessage(), e);
        }
    }

    /** Invokes a no-argument operation and returns its result rendered as text. */
    public String invokeNoArg(long pid, String objectName, String operation) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        ObjectName name = toObjectName(objectName);
        try {
            Object result = conn.invoke(name, operation, new Object[0], new String[0]);
            return render(result);
        } catch (Exception e) {
            throw new IOException("Cannot invoke " + operation + ": " + e.getMessage(), e);
        }
    }

    private static ObjectName toObjectName(String objectName) throws IOException {
        try {
            return ObjectName.getInstance(objectName);
        } catch (Exception e) {
            throw new IOException("Invalid object name: " + objectName, e);
        }
    }

    /** Renders an arbitrary attribute/return value into a readable string. */
    static String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CompositeData cd) {
            Map<String, Object> map = new TreeMap<>();
            for (String key : cd.getCompositeType().keySet()) {
                map.put(key, summary(cd.get(key)));
            }
            return map.toString();
        }
        if (value instanceof TabularData td) {
            return "TabularData(" + td.size() + " rows)";
        }
        if (value instanceof Object[] arr) {
            return Arrays.stream(arr).map(MBeanService::summary)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof Collection<?> col) {
            return col.stream().map(MBeanService::summary)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        if (value.getClass().isArray()) {
            // primitive array
            int len = java.lang.reflect.Array.getLength(value);
            List<String> parts = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                parts.add(String.valueOf(java.lang.reflect.Array.get(value, i)));
            }
            return parts.toString();
        }
        return value.toString();
    }

    private static String summary(Object value) {
        if (value instanceof CompositeData) {
            return "{…}";
        }
        return String.valueOf(value);
    }

    /** Coerces a string into the given declared attribute type for setAttribute. */
    private static Object coerce(String value, String type) {
        return switch (type) {
            case "int", "java.lang.Integer" -> Integer.valueOf(value.trim());
            case "long", "java.lang.Long" -> Long.valueOf(value.trim());
            case "double", "java.lang.Double" -> Double.valueOf(value.trim());
            case "float", "java.lang.Float" -> Float.valueOf(value.trim());
            case "boolean", "java.lang.Boolean" -> Boolean.valueOf(value.trim());
            default -> value;
        };
    }
}
