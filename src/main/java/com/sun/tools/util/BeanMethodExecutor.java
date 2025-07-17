package com.sun.tools.util;

import com.sun.tools.exception.BeanMethodExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BeanMethodExecutor 是一个支持参数推断、方法缓存、泛型返回、异常包装的反射调用工具类，
 * 用于从 Spring 容器中动态获取 Bean，并调用其方法。
 *
 * <p>适用场景：
 * - 插件机制或运行时扩展点
 * - 多模块解耦调用
 * - 框架类统一反射工具封装
 */
public class BeanMethodExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BeanMethodExecutor.class);

    /**
     * 方法缓存，避免重复反射获取 Method，提高性能
     * Key 规则：类名 + 方法名 + 参数类型签名
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 根据 Bean 名称 + 方法名 + 参数动态调用方法
     *
     * @param beanName   Spring Bean 名称
     * @param methodName 要调用的方法名
     * @param args       方法参数（可为 null）
     * @return 方法执行结果
     * @throws BeanMethodExecutionException 当调用失败时抛出
     */
    public static Object invoke(String beanName, String methodName, Object... args) {
        Assert.hasText(beanName, "beanName must not be empty");
        Assert.hasText(methodName, "methodName must not be empty");

        try {
            Object bean = SpringUtil.getBean(beanName);
            if (bean == null) {
                throw new BeanMethodExecutionException("未找到 Bean: " + beanName);
            }
            return invokeInternal(bean, methodName, args);
        } catch (Exception e) {
            if (e instanceof BeanMethodExecutionException) {
                throw (BeanMethodExecutionException) e;
            }
            throw new BeanMethodExecutionException("调用 Bean 方法失败: " + beanName + "." + methodName, e);
        }
    }

    /**
     * 根据 Bean 类型 + 方法名 + 参数动态调用方法（推荐使用类型方式）
     *
     * @param beanClass  Bean 类型
     * @param methodName 方法名
     * @param args       方法参数
     * @return 方法执行结果
     * @throws BeanMethodExecutionException 当调用失败时抛出
     */
    public static Object invoke(Class<?> beanClass, String methodName, Object... args) {
        Assert.notNull(beanClass, "beanClass must not be null");
        Assert.hasText(methodName, "methodName must not be empty");

        try {
            Object bean = SpringUtil.getBean(beanClass);
            if (bean == null) {
                throw new BeanMethodExecutionException("未找到 Bean: " + beanClass.getName());
            }
            return invokeInternal(bean, methodName, args);
        } catch (Exception e) {
            if (e instanceof BeanMethodExecutionException) {
                throw (BeanMethodExecutionException) e;
            }
            throw new BeanMethodExecutionException("调用 Bean 方法失败: " + beanClass.getName() + "." + methodName, e);
        }
    }

    /**
     * 强类型调用（通过 Bean 名称），自动将返回值转换为指定类型
     *
     * @param beanName   Bean 名称
     * @param methodName 方法名
     * @param returnType 返回值类型
     * @param args       参数
     * @param <T>        泛型类型
     * @return 返回值（已强制类型转换）
     * @throws BeanMethodExecutionException 当调用失败时抛出
     */
    public static <T> T invokeWithReturn(String beanName, String methodName, Class<T> returnType, Object... args) {
        Object result = invoke(beanName, methodName, args);
        return castReturn(result, returnType);
    }

    /**
     * 强类型调用（通过 Bean 类型），自动将返回值转换为指定类型
     *
     * @param beanClass  Bean 类型
     * @param methodName 方法名
     * @param returnType 返回值类型
     * @param args       参数
     * @param <T>        泛型类型
     * @return 返回值（已强制类型转换）
     * @throws BeanMethodExecutionException 当调用失败时抛出
     */
    public static <T> T invokeWithReturn(Class<?> beanClass, String methodName, Class<T> returnType, Object... args) {
        Object result = invoke(beanClass, methodName, args);
        return castReturn(result, returnType);
    }

    /**
     * 调用方法并返回 List<T>，带元素类型校验
     *
     * @param beanName    Bean 名称
     * @param methodName  方法名
     * @param elementType 列表元素类型
     * @param args        参数
     * @return 安全类型的 List<T>
     * @throws BeanMethodExecutionException 当调用失败时抛出
     */
    public static <T> List<T> invokeListWithReturn(String beanName, String methodName,
                                                   Class<T> elementType, Object... args) {
        Object result = invoke(beanName, methodName, args);
        return castList(result, elementType);
    }

    /**
     * 调用方法并返回 List<T>（基于 Bean 类型）
     *
     * @param beanClass   Bean 类型
     * @param methodName  方法名
     * @param elementType 列表元素类型
     * @param args        参数
     * @return 类型安全的 List<T>
     * @throws BeanMethodExecutionException 当调用失败时抛出
     */
    public static <T> List<T> invokeListWithReturn(Class<?> beanClass, String methodName,
                                                   Class<T> elementType, Object... args) {
        Object result = invoke(beanClass, methodName, args);
        return castList(result, elementType);
    }

    /**
     * 核心调用逻辑：查找方法、缓存、调用
     *
     * @param bean       Bean 实例
     * @param methodName 方法名
     * @param args       参数
     * @return 方法调用结果
     * @throws Exception 当反射调用失败时抛出
     */
    private static Object invokeInternal(Object bean, String methodName, Object... args) throws Exception {
        Method method = resolveMethod(bean.getClass(), methodName, args);

        if (logger.isDebugEnabled()) {
            logger.debug("调用方法: {}.{}({})", bean.getClass().getSimpleName(), methodName,
                    Arrays.toString(args));
        }

        method.setAccessible(true);
        return method.invoke(bean, args);
    }

    /**
     * 查找匹配的方法，并进行缓存（支持参数为 null 的情况）
     * 改进的缓存策略：考虑参数类型，而不仅仅是参数数量
     *
     * @param clazz      Bean 的类
     * @param methodName 方法名
     * @param args       参数值（允许为 null）
     * @return 匹配到的方法
     */
    private static Method resolveMethod(Class<?> clazz, String methodName, Object[] args) {
        // 构建更精确的缓存键，包含参数类型信息
        String cacheKey = buildCacheKey(clazz, methodName, args);

        // 方法缓存命中
        Method cachedMethod = METHOD_CACHE.get(cacheKey);
        if (cachedMethod != null) {
            return cachedMethod;
        }

        // 查找匹配的方法
        Method bestMatch = findBestMatchMethod(clazz, methodName, args);
        if (bestMatch != null) {
            METHOD_CACHE.put(cacheKey, bestMatch);
            return bestMatch;
        }

        throw new BeanMethodExecutionException("找不到匹配的方法: " + methodName + " in class: " + clazz.getName() +
                " with args: " + Arrays.toString(args));
    }

    /**
     * 构建缓存键，包含参数类型信息
     */
    private static String buildCacheKey(Class<?> clazz, String methodName, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(clazz.getName()).append("#").append(methodName).append("#");

        if (args == null || args.length == 0) {
            keyBuilder.append("void");
        } else {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) keyBuilder.append(",");
                keyBuilder.append(args[i] != null ? args[i].getClass().getName() : "null");
            }
        }

        return keyBuilder.toString();
    }

    /**
     * 查找最佳匹配的方法，支持类型兼容性检查
     */
    private static Method findBestMatchMethod(Class<?> clazz, String methodName, Object[] args) {
        // 先用 getMethods() 查找 public 方法（大多数情况）
        Method[] publicMethods = clazz.getMethods();
        Method found = searchInMethods(publicMethods, methodName, args);

        if (found != null) {
            return found;
        }

        // 如果没找到，再遍历查找私有方法
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            found = searchInMethods(declaredMethods, methodName, args);
            if (found != null) {
                return found;
            }
            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    /**
     * 在给定的方法数组中搜索最佳匹配的方法
     */
    private static Method searchInMethods(Method[] methods, String methodName, Object[] args) {
        int argCount = args == null ? 0 : args.length;

        // 精确匹配优先
        for (Method method : methods) {
            if (isExactMatch(method, methodName, args, argCount)) {
                return method;
            }
        }

        // 兼容性匹配
        for (Method method : methods) {
            if (isCompatibleMatch(method, methodName, args, argCount)) {
                return method;
            }
        }

        return null;
    }

    /**
     * 检查是否为精确匹配
     */
    private static boolean isExactMatch(Method method, String methodName, Object[] args, int argCount) {
        if (!method.getName().equals(methodName)) {
            return false;
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != argCount) {
            return false;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            if (arg != null && !paramTypes[i].equals(arg.getClass())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否为兼容性匹配
     */
    private static boolean isCompatibleMatch(Method method, String methodName, Object[] args, int argCount) {
        if (!method.getName().equals(methodName)) {
            return false;
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != argCount) {
            return false;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                // null 可以匹配任何引用类型，但不能匹配基本类型
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
            } else if (!isAssignable(paramTypes[i], arg.getClass())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 改进的类型兼容性检查，支持基本类型和包装类型的转换
     */
    private static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }

        // 处理基本类型和包装类型的转换
        if (targetType.isPrimitive() || sourceType.isPrimitive()) {
            return isPrimitiveWrapper(targetType, sourceType) || isPrimitiveWrapper(sourceType, targetType);
        }

        return false;
    }

    /**
     * 检查是否为基本类型和包装类型的关系
     */
    private static boolean isPrimitiveWrapper(Class<?> primitive, Class<?> wrapper) {
        return (primitive == boolean.class && wrapper == Boolean.class) ||
                (primitive == byte.class && wrapper == Byte.class) ||
                (primitive == char.class && wrapper == Character.class) ||
                (primitive == double.class && wrapper == Double.class) ||
                (primitive == float.class && wrapper == Float.class) ||
                (primitive == int.class && wrapper == Integer.class) ||
                (primitive == long.class && wrapper == Long.class) ||
                (primitive == short.class && wrapper == Short.class);
    }

    /**
     * 返回值类型转换工具（带校验）
     *
     * @param result     原始结果
     * @param returnType 目标类型
     * @param <T>        泛型
     * @return 强类型返回值
     * @throws ClassCastException 当类型转换失败时抛出
     */
    private static <T> T castReturn(Object result, Class<T> returnType) {
        if (result == null) {
            return null;
        }

        if (!returnType.isInstance(result)) {
            throw new ClassCastException("返回值类型不匹配，期望: " + returnType.getName() +
                    "，但实际为: " + result.getClass().getName());
        }

        return returnType.cast(result);
    }

    /**
     * 将返回值安全转换为 List<T>，并校验每个元素的类型
     *
     * @param result      原始结果
     * @param elementType 元素类型
     * @param <T>         泛型类型
     * @return 类型安全的 List<T>
     * @throws ClassCastException 当类型转换失败时抛出
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object result, Class<T> elementType) {
        if (result == null) {
            return null;
        }

        if (!(result instanceof List)) {
            throw new ClassCastException("返回类型不是 List，实际为: " + result.getClass().getName());
        }

        List<?> rawList = (List<?>) result;
        List<T> typedList = new ArrayList<>(rawList.size());

        for (int i = 0; i < rawList.size(); i++) {
            Object item = rawList.get(i);
            if (item != null && !elementType.isInstance(item)) {
                throw new ClassCastException("List 中第 " + i + " 个元素类型不匹配，期望: " +
                        elementType.getSimpleName() + "，实际为: " + item.getClass().getName());
            }
            typedList.add((T) item);
        }

        return typedList;
    }

    /**
     * 清理方法缓存（用于测试或特殊场景）
     */
    public static void clearMethodCache() {
        METHOD_CACHE.clear();
        if (logger.isDebugEnabled()) {
            logger.debug("方法缓存已清理");
        }
    }

    /**
     * 获取缓存统计信息
     */
    public static int getCacheSize() {
        return METHOD_CACHE.size();
    }


}
