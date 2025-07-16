package com.sun.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * SpringUtil 工具类，用于在任何地方通过静态方法获取 Spring 容器中的 Bean。
 * 需要 Spring 扫描此类并自动注入 ApplicationContext。
 */
@Component
public class SpringUtil implements ApplicationContextAware {

    /**
     * Spring 应用上下文，静态保存
     */
    private static ApplicationContext applicationContext;

    /**
     * 实现 ApplicationContextAware 接口，Spring 启动时会自动调用此方法注入上下文
     *
     * @param applicationContext Spring 应用上下文
     * @throws BeansException 可能抛出的异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.applicationContext = applicationContext;
    }

    /**
     * 获取当前静态保存的 ApplicationContext
     *
     * @return ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过 Bean 名称获取 Bean 实例
     *
     * @param name Bean 名称
     * @return Bean 实例
     * @throws BeansException Bean 不存在时抛出异常
     */
    public static Object getBean(String name) throws BeansException {
        assertContextInjected();
        try {
            return applicationContext.getBean(name);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 通过类型获取 Bean 实例
     *
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例
     * @throws BeansException Bean 不存在时抛出异常
     */
    public static <T> T getBean(Class<T> clazz) throws BeansException {
        assertContextInjected();
        try {
            return applicationContext.getBean(clazz);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 通过名称和类型获取 Bean 实例
     *
     * @param name  Bean 名称
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例
     * @throws BeansException Bean 不存在时抛出异常
     */
    public static <T> T getBean(String name, Class<T> clazz) throws BeansException {
        assertContextInjected();
        try {
            return applicationContext.getBean(name, clazz);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 检查 ApplicationContext 是否注入，未注入时抛异常提醒
     */
    private static void assertContextInjected() {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext 未注入，请确保 SpringUtil 被 Spring 扫描到！");
        }
    }
}

