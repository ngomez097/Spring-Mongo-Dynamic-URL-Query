package com.query.util;

import com.query.exception.BadRequestException;
import org.bson.types.ObjectId;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Utils {

    public static Integer isInteger(String str) {
        if (isIntegerInternal(str)) {
            try {
                return Integer.parseInt(str);

            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean isIntegerInternal(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static Float isFloat(String str) {
        if (isFloatInternal(str)) {
            try {
                return Float.parseFloat(str);

            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean isFloatInternal(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if ((c < '0' || c > '9') && c != '.' && c != 'e') {
                return false;
            }
        }
        return true;
    }

    public static Boolean isBoolean(String str) {
        if (str.equals("true")) {
            return true;
        }
        if (str.equals("false")) {
            return false;
        }
        return null;
    }

    public static ObjectId isObjectId(String str) {
        if (str == null) {
            return null;
        }

        if (ObjectId.isValid(str)) {
            return new ObjectId(str);
        } else {
            return null;
        }
    }

    public static String isNull(String str) {
        if (str == null || str.equals("null"))
            return null;

        return str;
    }

    /**
     * Función para saber si un objeto es Float, Interger, String, Long o Boolean
     * */
    public static boolean isPrimitive(Object obj) {
        Class cls = obj.getClass();
        return cls.equals(Float.class) || cls.equals(Integer.class) ||
            cls.equals(String.class) || cls.equals(Long.class) ||
            cls.equals(Boolean.class) || cls.isPrimitive();
    }

    public static <T> Object newInstance(Class<T> cls) {
        try {
            Constructor<T> constructor = cls.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> Object copyList(List<T> src) {
        List<T> out;
        out = (List<T>) newInstance(src.getClass());
        if (out == null) {
            return null;
        }
        for (T elem : src) {
            if(isPrimitive(elem)) {
                out.add(elem);
                continue;
            }

            T aux = (T) newInstance(elem.getClass());
            Utils.copyObjectSame(elem, aux);
            out.add(aux);
        }

        return out;
    }

    public static void copyObjectSame(Object src, Object dest) {
        if (src == null || dest == null) {
            return;
        }

        Field[] fields = src.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(src);
                if (value == null) {
                    continue;
                }

                if (value instanceof List) {
                    value = copyList((List) value);
                } else if (!isPrimitive(value)) {
                    Object last = value;
                    value = newInstance(value.getClass());
                    Utils.copyObjectSame(last, value);
                }

                field.set(dest, value);
            } catch (Exception ignored) {
            }
        }
    }

    public static void copyObjectDist(Object src, Object dest) {
        if (src == null || dest == null) {
            return;
        }

        Field[] fields = src.getClass().getDeclaredFields();
        Field fieldDest;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                fieldDest = dest.getClass().getDeclaredField(field.getName());
                fieldDest.setAccessible(true);

                Object value = field.get(src);
                if (value == null) {
                    continue;
                }

                if (value instanceof List) {
                    value = copyList((List) value);
                } else if (!isPrimitive(value)) {
                    Object last = value;
                    value = newInstance(value.getClass());
                    Utils.copyObjectSame(last, value);
                }

                fieldDest.set(dest, value);

            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Copiar los datos de un objeto a otro
     */
    public static <T> void copyObjectFields(T src, T dest, String... fields) {
        if (src == null || dest == null) {
            return;
        }

        Field f;
        for (String field : fields) {
            try {
                f = src.getClass().getDeclaredField(field);
                f.setAccessible(true);
                Object value = f.get(src);

                if (value == null) {
                    continue;
                }

                if (value instanceof List) {
                    value = copyList((List) value);
                } else if (!isPrimitive(value)) {
                    Object last = value;
                    value = newInstance(value.getClass());
                    Utils.copyObjectSame(last, value);
                }
                f.set(dest, value);


            } catch (Exception ignored) {
            }
        }
    }

    public static String toUpperCaseFirst(String str) {
        return str.substring(0, 1).toUpperCase().concat(str.substring(1));
    }

    /**
     * Función para realiza un split de una variable string
     * teniendo en cuenta que puede haber una lógica de niveles
     */
    public static List<String> splitGreaterLevel(String str, char by, char subLevelCharOpen, char subLevelCharClose, boolean leaveLevelChar) {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> splitList = new LinkedList<>();
        char[] chars = str.toCharArray();
        int openCount = 0;

        for (char c : chars) {
            if (c == subLevelCharOpen) {
                openCount++;
                if (!leaveLevelChar) continue;
            } else if (c == subLevelCharClose) {
                openCount--;
                if (!leaveLevelChar) continue;
            } else if (c == by && openCount == 0) {
                if (stringBuilder.length() > 0) {
                    splitList.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }
                continue;
            }

            stringBuilder.append(c);
        }

        if (openCount != 0) {
            throw new BadRequestException("Invalid parentheses grouping");
        }

        if (stringBuilder.length() > 0) {
            splitList.add(stringBuilder.toString());
        }

        return splitList;
    }

    /**
     * Función para encontrar el indice de un objeto dentro de un arreglo
     * por un campo en particular.
     */
    public static <T> int findIndexInArray(List<T> array, String fieldStr, Object value) {
        if (array == null || array.size() == 0 || value == null || fieldStr == null || fieldStr.length() == 0) {
            return -1;
        }

        Iterator<T> iter = array.iterator();
        Field field;
        try {
            field = array.get(0).getClass().getDeclaredField(fieldStr);
            field.setAccessible(true);
        } catch (Exception e) {
            return -1;
        }

        int index = 0;
        while (iter.hasNext()) {
            T obj = iter.next();
            try {
                if (value.equals(field.get(obj))) {
                    return index;
                }
            } catch (Exception ignored) {
            }
            index++;
        }
        return -1;
    }

    /**
     * Función para encontrar un objeto dentro de un arreglo por un campo en particular.
     */
    public static <T> T findObjInArray(List<T> array, String fieldStr, Object value) {
        if (array == null || array.size() == 0 || value == null || fieldStr == null || fieldStr.length() == 0) {
            return null;
        }

        Iterator<T> iter = array.iterator();
        Field field;
        try {
            field = array.get(0).getClass().getDeclaredField(fieldStr);
            field.setAccessible(true);
        } catch (Exception e) {
            return null;
        }

        while (iter.hasNext()) {
            T obj = iter.next();
            try {
                if (field.get(obj).equals(value)) {
                    return obj;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static int generateRandomInt(int length){

        int out = 0;
        Random random = new SecureRandom();
        for(int i = 0; i < length-1; i++){
            out += random.nextInt(10)*Math.pow(10, i);
        }
        out += (1+random.nextInt(9))*Math.pow(10, length-1);


        return out;
    }

    public static String getFileString(String path){
        StringBuilder content = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String str;
            while ((str = in.readLine()) != null) {
                content.append(str);
            }
            in.close();
        } catch (IOException ignored) {
        }
        return content.toString();
    }

    public static String generateRandomString(int size){
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new SecureRandom();
        String charSet = "a0Ab1Bc2Cd3De4Ef5Fg6Gh7Hi8Ij9Jk_Kl.Lm$Mn#No!Op?Pq¡Qr¿Rs%St&TuUvVwWxXyYzZ";

        for(int i = 0; i < size; i++){
            int index = random.nextInt(charSet.length());
            stringBuilder.append(charSet.charAt(index));
        }
        return stringBuilder.toString();
    }
}
