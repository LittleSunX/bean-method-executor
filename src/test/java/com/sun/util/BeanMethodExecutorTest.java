package com.sun.util;

import com.sun.tools.exception.BeanMethodExecutionException;
import com.sun.tools.util.BeanMethodExecutor;
import com.sun.tools.util.SpringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/**
 * BeanMethodExecutor 测试类
 * 测试各种调用场景、异常情况和边界条件
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = BeanMethodExecutor.class)
class BeanMethodExecutorTest {

    // 测试用的 Bean 类
    @Service("testService")
    public static class TestService {

        public String simpleMethod() {
            return "simple result";
        }

        public String methodWithParam(String param) {
            return "param: " + param;
        }

        public String methodWithMultipleParams(String str, Integer num, Boolean flag) {
            return String.format("str=%s, num=%d, flag=%s", str, num, flag);
        }

        public String methodWithNullParam(String param) {
            return "param is " + (param == null ? "null" : param);
        }

        public List<String> getStringList() {
            return Arrays.asList("item1", "item2", "item3");
        }

        public List<User> getUserList() {
            return Arrays.asList(
                    new User(1L, "张三"),
                    new User(2L, "李四")
            );
        }

        public User getUserById(Long id) {
            return new User(id, "用户" + id);
        }

        public User getUserByIdAndName(Long id, String name) {
            return new User(id, name);
        }

        // 重载方法测试
        public String overloadedMethod(String param) {
            return "String: " + param;
        }

        public String overloadedMethod(Integer param) {
            return "Integer: " + param;
        }

        public String overloadedMethod(Long param) {
            return "Long: " + param;
        }

        // 基本类型和包装类型测试
        public String primitiveMethod(int value) {
            return "primitive int: " + value;
        }

        public String wrapperMethod(Integer value) {
            return "wrapper Integer: " + value;
        }

        // 异常方法
        public void throwException() {
            throw new RuntimeException("测试异常");
        }

        // 返回 null 的方法
        public String returnNull() {
            return null;
        }

        // 私有方法（测试访问性）
        private String privateMethod() {
            return "private method result";
        }
    }

    // 测试用的数据类
    public static class User {
        private Long id;
        private String name;

        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        // getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "'}";
        }
    }

    private TestService testService;

    @BeforeEach
    void setUp() {
        testService = new TestService();
        // 清理缓存，确保测试独立性
        BeanMethodExecutor.clearMethodCache();
    }

    /**
     * 测试基本的方法调用（通过 Bean 名称）
     */
    @Test
    void testInvokeByBeanName() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            Object result = BeanMethodExecutor.invoke("testService", "simpleMethod");

            assertEquals("simple result", result);
        }
    }

    /**
     * 测试基本的方法调用（通过 Bean 类型）
     */
    @Test
    void testInvokeByBeanClass() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean(TestService.class))
                    .thenReturn(testService);

            Object result = BeanMethodExecutor.invoke(TestService.class, "simpleMethod");

            assertEquals("simple result", result);
        }
    }

    /**
     * 测试带参数的方法调用
     */
    @Test
    void testInvokeWithParameters() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            Object result = BeanMethodExecutor.invoke("testService", "methodWithParam", "hello");

            assertEquals("param: hello", result);
        }
    }

    /**
     * 测试多个参数的方法调用
     */
    @Test
    void testInvokeWithMultipleParameters() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            Object result = BeanMethodExecutor.invoke("testService", "methodWithMultipleParams",
                    "test", 123, true);

            assertEquals("str=test, num=123, flag=true", result);
        }
    }

    /**
     * 测试 null 参数的处理
     */
    @Test
    void testInvokeWithNullParameter() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            Object result = BeanMethodExecutor.invoke("testService", "methodWithNullParam", (String) null);

            assertEquals("param is null", result);
        }
    }

    /**
     * 测试强类型返回值调用
     */
    @Test
    void testInvokeWithReturn() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            String result = BeanMethodExecutor.invokeWithReturn("testService", "methodWithParam",
                    String.class, "typed");

            assertEquals("param: typed", result);
        }
    }

    /**
     * 测试用户对象返回
     */
    @Test
    void testInvokeUserObject() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            User user = BeanMethodExecutor.invokeWithReturn("testService", "getUserById",
                    User.class, 123L);

            assertNotNull(user);
            assertEquals(123L, user.getId());
            assertEquals("用户123", user.getName());
        }
    }

    /**
     * 测试 List 返回值调用
     */
    @Test
    void testInvokeListWithReturn() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            List<String> result = BeanMethodExecutor.invokeListWithReturn("testService", "getStringList",
                    String.class);

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("item1", result.get(0));
            assertEquals("item2", result.get(1));
            assertEquals("item3", result.get(2));
        }
    }

    /**
     * 测试复杂对象 List 返回值
     */
    @Test
    void testInvokeUserListWithReturn() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            List<User> result = BeanMethodExecutor.invokeListWithReturn("testService", "getUserList",
                    User.class);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("张三", result.get(0).getName());
            assertEquals("李四", result.get(1).getName());
        }
    }

    /**
     * 测试方法重载
     */
    @Test
    void testMethodOverloading() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 测试 String 参数重载
            Object result1 = BeanMethodExecutor.invoke("testService", "overloadedMethod", "test");
            assertEquals("String: test", result1);

            // 测试 Integer 参数重载
            Object result2 = BeanMethodExecutor.invoke("testService", "overloadedMethod", 123);
            assertEquals("Integer: 123", result2);

            // 测试 Long 参数重载
            Object result3 = BeanMethodExecutor.invoke("testService", "overloadedMethod", 123L);
            assertEquals("Long: 123", result3);
        }
    }

    /**
     * 测试基本类型和包装类型的兼容性
     */
    @Test
    void testPrimitiveAndWrapperTypes() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 测试基本类型方法
            Object result1 = BeanMethodExecutor.invoke("testService", "primitiveMethod", 42);
            assertEquals("primitive int: 42", result1);

            // 测试包装类型方法
            Object result2 = BeanMethodExecutor.invoke("testService", "wrapperMethod", Integer.valueOf(42));
            assertEquals("wrapper Integer: 42", result2);
        }
    }

    /**
     * 测试私有方法调用
     */
    @Test
    void testPrivateMethodAccess() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            Object result = BeanMethodExecutor.invoke("testService", "privateMethod");
            assertEquals("private method result", result);
        }
    }

    /**
     * 测试返回 null 的方法
     */
    @Test
    void testReturnNullMethod() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            String result = BeanMethodExecutor.invokeWithReturn("testService", "returnNull", String.class);
            assertNull(result);
        }
    }

    /**
     * 测试方法缓存功能
     */
    @Test
    void testMethodCaching() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 第一次调用
            int initialCacheSize = BeanMethodExecutor.getCacheSize();
            BeanMethodExecutor.invoke("testService", "simpleMethod");
            int afterFirstCall = BeanMethodExecutor.getCacheSize();

            // 第二次调用相同方法
            BeanMethodExecutor.invoke("testService", "simpleMethod");
            int afterSecondCall = BeanMethodExecutor.getCacheSize();

            // 验证缓存生效
            assertEquals(initialCacheSize + 1, afterFirstCall);
            assertEquals(afterFirstCall, afterSecondCall);
        }
    }

    /**
     * 测试缓存清理功能
     */
    @Test
    void testCacheClear() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 添加一些缓存
            BeanMethodExecutor.invoke("testService", "simpleMethod");
            BeanMethodExecutor.invoke("testService", "methodWithParam", "test");

            assertTrue(BeanMethodExecutor.getCacheSize() > 0);

            // 清理缓存
            BeanMethodExecutor.clearMethodCache();
            assertEquals(0, BeanMethodExecutor.getCacheSize());
        }
    }

    /**
     * 测试 Bean 不存在的异常情况
     */
    @Test
    void testBeanNotFound() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("nonExistentService"))
                    .thenReturn(null);

            BeanMethodExecutionException exception =
                    assertThrows(BeanMethodExecutionException.class,
                            () -> BeanMethodExecutor.invoke("nonExistentService", "someMethod"));
            System.out.println(exception.getMessage());
            assertTrue(exception.getMessage().contains("未找到 Bean"));
        }
    }

    /**
     * 测试方法不存在的异常情况
     */
    @Test
    void testMethodNotFound() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            BeanMethodExecutionException exception =
                    assertThrows(BeanMethodExecutionException.class,
                            () -> BeanMethodExecutor.invoke("testService", "nonExistentMethod"));
            assertTrue(exception.getMessage().contains("找不到匹配的方法"));
        }
    }

    /**
     * 测试方法执行时抛出异常
     */
    @Test
    void testMethodExecutionException() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            BeanMethodExecutionException exception =
                    assertThrows(BeanMethodExecutionException.class,
                            () -> BeanMethodExecutor.invoke("testService", "throwException"));

            assertTrue(exception.getMessage().contains("调用 Bean 方法失败"));
            assertInstanceOf(InvocationTargetException.class, exception.getCause());
        }
    }

    /**
     * 测试类型转换异常
     */
    @Test
    void testTypeCastException() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 尝试将 String 结果转换为 Integer
            ClassCastException exception =
                    assertThrows(ClassCastException.class,
                            () -> BeanMethodExecutor.invokeWithReturn("testService", "simpleMethod", Integer.class));

            assertTrue(exception.getMessage().contains("返回值类型不匹配"));
        }
    }

    /**
     * 测试 List 类型转换异常
     */
    @Test
    void testListTypeCastException() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 尝试将非 List 结果转换为 List
            ClassCastException exception =
                    assertThrows(ClassCastException.class,
                            () -> BeanMethodExecutor.invokeListWithReturn("testService", "simpleMethod", String.class));

            assertTrue(exception.getMessage().contains("返回类型不是 List"));
        }
    }

    /**
     * 测试 List 元素类型不匹配异常
     */
    @Test
    void testListElementTypeMismatch() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 尝试将 String List 转换为 Integer List
            ClassCastException exception =
                    assertThrows(ClassCastException.class,
                            () -> BeanMethodExecutor.invokeListWithReturn("testService", "getStringList", Integer.class));

            assertTrue(exception.getMessage().contains("元素类型不匹配"));
        }
    }

    /**
     * 测试参数校验
     */
    @Test
    void testParameterValidation() {
        // 测试 beanName 为空
        IllegalArgumentException exception1 =
                assertThrows(IllegalArgumentException.class,
                        () -> BeanMethodExecutor.invoke("", "method"));

        // 测试 methodName 为空
        IllegalArgumentException exception2 =
                assertThrows(IllegalArgumentException.class,
                        () -> BeanMethodExecutor.invoke("bean", ""));

        // 测试 beanClass 为 null
        IllegalArgumentException exception3 =
                assertThrows(IllegalArgumentException.class,
                        () -> BeanMethodExecutor.invoke((Class<?>) null, "method"));
    }

    /**
     * 测试复杂参数匹配场景
     */
    @Test
    void testComplexParameterMatching() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 测试多参数方法调用
            User result = BeanMethodExecutor.invokeWithReturn("testService", "getUserByIdAndName",
                    User.class, 999L, "复杂测试");

            assertNotNull(result);
            assertEquals(999L, result.getId());
            assertEquals("复杂测试", result.getName());
        }
    }

    /**
     * 性能测试（可选）
     */
    @Test
    void testPerformance() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);

            // 预热缓存
            BeanMethodExecutor.invoke("testService", "simpleMethod");

            long startTime = System.currentTimeMillis();

            // 多次调用，测试缓存效果
            for (int i = 0; i < 1000; i++) {
                BeanMethodExecutor.invoke("testService", "simpleMethod");
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("1000 次缓存调用耗时: " + duration + "ms");

            // 验证缓存只有一个条目
            assertEquals(1, BeanMethodExecutor.getCacheSize());
        }
    }

    @Test
    void test1() {
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean("testService"))
                    .thenReturn(testService);
            Object invoke = BeanMethodExecutor.invoke("testService", "simpleMethod");
            System.out.println(invoke);
        }
    }
}