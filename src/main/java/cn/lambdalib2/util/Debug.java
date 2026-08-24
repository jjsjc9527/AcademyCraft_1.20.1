/*
 * 源自 LambdaLib2 (https://github.com/LambdaInnovation/LambdaLib2),MIT 协议,作者 WeAthFolD。
 */
package cn.lambdalib2.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.function.Supplier;

public class Debug {

    private static final Logger logger = LogManager.getLogger("AcademyCraft-LL2");

    public static RuntimeException TODO() {
        throw new RuntimeException("TODO: Not implemented!");
    }

    public static void assert2(boolean expr) {
        assert2(expr, "Assertion failed");
    }

    public static void assert2(boolean expr, Supplier<String> lazyMessage) {
        if (!expr) {
            throw new RuntimeException("Assertion failed: " + lazyMessage.get());
        }
    }

    public static void assert2(boolean expr, String message) {
        if (!expr) {
            throw new RuntimeException("Assertion failed: " + message);
        }
    }

    public static void require(boolean expr) {
        require(expr, "Requirement failed");
    }

    public static void require(boolean expr, String message) {
        if (!expr) {
            throw new RuntimeException("Requirement failed: " + message);
        }
    }

    public static void require(boolean expr, Supplier<String> lazyMessage) {
        if (!expr) {
            throw new RuntimeException("Requirement failed: " + lazyMessage.get());
        }
    }

    public static <T> T assertNotNull(T obj) {
        return assertNotNull(obj, "Object is null");
    }

    public static <T> T assertNotNull(T obj, String message) {
        return Objects.requireNonNull(obj, message);
    }

    public static <T> T assertNotNull(T obj, Supplier<String> lazyMessage) {
        if (obj == null) {
            throw new RuntimeException(lazyMessage.get());
        }
        return obj;
    }

    public static void debug(String msg) {
        logger.debug(msg);
    }

    public static void debugFormat(String format, Object... pars) {
        debug(String.format(format, pars));
    }

    public static void log(String msg) {
        logger.info(msg);
    }

    public static void logFormat(String format, Object... params) {
        log(String.format(format, params));
    }

    public static void error(Throwable ex) {
        logger.error(ex);
    }

    public static void error(String msg, Throwable ex) {
        logger.error(msg, ex);
    }

    public static void error(String msg) {
        logger.error(msg);
    }

    public static void warn(String msg) {
        logger.warn(msg);
    }

    public static void warnFormat(String msg, Object... pars) {
        warn(String.format(msg, pars));
    }

    private Debug() {}
}
