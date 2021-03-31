package com.query.util;

import com.query.exception.BadRequestException;
import org.bson.types.ObjectId;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class DynamicTypes {

    public enum TYPES {
        INT("int"),
        FLOAT("float"),
        BOOL("bool"),
        OBJECT_ID("objectId"),
        TIMESTAMP("timestamp"),
        LONG("long"),
        STRING("string");

        private final String value;


        TYPES(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    private static final Map<String, Class> typeMap = new HashMap<>();

    static {
        typeMap.put(TYPES.FLOAT.value, Float.class);
        typeMap.put(TYPES.OBJECT_ID.value, ObjectId.class);
        typeMap.put(TYPES.TIMESTAMP.value, Long.class);
        typeMap.put(TYPES.INT.value, Integer.class);
        typeMap.put(TYPES.LONG.value, Long.class);
        typeMap.put(TYPES.BOOL.value, Boolean.class);
        typeMap.put(TYPES.STRING.value, String.class);
    }

    public static Object inferType(String s) {
        Object value;

        // Probar primero con null
        value = Utils.isNull(s);
        if (value == null) return null;

        value = Utils.isInteger(s);
        if (value == null) {
            value = Utils.isFloat(s);
        }
        if (value == null) {
            value = Utils.isBoolean(s);
        }
        if (value == null) {
            value = Utils.isObjectId(s);
        }
        if (value == null) {
            value = s;
        }

        return value;
    }

    public static Object castType(String type, String str) throws BadRequestException {
        Object value;

        if (str == null) {
            return null;
        }

        if (!typeMap.containsKey(type)) {
            throw new BadRequestException("Invalid cast type " + type);
        }

        Class cls = typeMap.get(type);

        if (cls.equals(String.class)) {
            return str;
        }

        Class[] classes = {String.class};

        try {
            Constructor c = cls.getConstructor(classes);
            value = c.newInstance(str);
        } catch (Exception e) {
            throw new BadRequestException(String.format("Cannot cast to %s value '%s'", type, str));
        }

        return value;
    }
}
