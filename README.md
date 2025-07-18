# BeanMethodExecutor

> 🚀 一个基于 Spring Boot 的通用反射调用工具类，支持从 Spring 容器中动态获取 Bean 并调用其方法，适用于插件机制、运行时扩展点、多模块解耦等场景。

---

## ✨ 特性

- ✅ 动态调用 Bean 中的方法（支持 Bean 名称或类型）
- ✅ 方法缓存（基于类名 + 方法名 + 参数签名）
- ✅ 参数自动推断，兼容 null 与基本类型
- ✅ 强类型返回支持（包括 `List<T>`）
- ✅ 封装统一异常，方便调用方处理
- ✅ 支持私有方法调用
- ✅ 无侵入、零依赖、易集成

---

## 🔧 环境要求

- Java 8+
- Spring Boot 2.3.x（底层 Spring Framework 5.3.x）

---

## 📦 快速集成
### 方式一：直接在项目中添加以下依赖
 ``` xml
    <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
	
	<dependency>
            <groupId>com.github.LittleSunX</groupId>
            <artifactId>bean-method-executor</artifactId>
            <version>v1.0.1</version>
    </dependency>
  ``` 
### 方式二：安装到本地 Maven 仓库

1. 克隆代码并打包：

   ```bash
   mvn clean install
2. 在你的项目中添加依赖：
   ``` xml
   <dependency>
     <groupId>com.sun.tools</groupId>
     <artifactId>bean-method-executor</artifactId>
     <version>1.0.0</version>
   </dependency>
   ```
3. 🚀  使用示例

 调用无参数方法
``` java
 Object result = BeanMethodExecutor.invoke("myService", "noParamMethod");
```
 调用带参数并返回类型的方法
``` java
 String res = BeanMethodExecutor.invokeWithReturn(MyService.class, "process", String.class, "abc", 123);
```
 调用返回 List<T> 的方法：
``` java
 List<User> users = BeanMethodExecutor.invokeListWithReturn("userService", "listUsers", User.class);
```
 捕获异常统一处理
``` java
  try {
      Object value = BeanMethodExecutor.invoke("myBean", "methodName", arg1, arg2);
   } catch (BeanMethodExecutionException e) {
       log.error("调用失败", e);
   }
```
🧩 核心方法一览
   | 方法名                                                        | 描述                   |
   | ---------------------------------------------------------- | -------------------- |
   | `invoke(String beanName, String methodName, Object...)`    | 通过 Bean 名称调用方法       |
   | `invoke(Class<?> beanClass, String methodName, Object...)` | 通过 Bean 类型调用方法       |
   | `invokeWithReturn(...)`                                    | 带返回值类型的调用            |
   | `invokeListWithReturn(...)`                                | 返回 `List<T>`，并校验元素类型 |
   | `clearMethodCache()`                                       | 清理方法缓存（测试或热更新用）      |
   | `getCacheSize()`                                           | 获取当前缓存的方法数量          |

📌 注意事项
   1. 方法调用时，内部支持缓存避免多次反射。
   2. 若方法不存在、参数不匹配或反射失败，统一抛出 BeanMethodExecutionException。
   3. 支持私有方法、方法重载、参数为 null 情况。 
   4. 方法参数支持自动类型匹配，包括基本类型与包装类型的兼容。

 
