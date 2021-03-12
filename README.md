# SpringBoot-Web-Demo

> 本项目基于SpringBoot 2.1.7.RELEASE版本开发

- 此项目只涉及到 springboot 开发，没用到数据库持久化，表单数据都是 Dao 里 static 手动加载的数据。
- 初始登录 用户名`随意`，密码默认 `123456`

## 前端展示：

> 本项目使用的前端模板是 Bootstrap 官网上的题材。仅供学习使用。

- 登录界面
[![前端1](https://s3.ax1x.com/2021/03/11/6NK8OJ.png)](https://imgtu.com/i/6NK8OJ)


- Dashboard

[![前端2](https://s3.ax1x.com/2021/03/11/6NKIXQ.png)](https://imgtu.com/i/6NKIXQ)

- 员工管理

[![前端3](https://s3.ax1x.com/2021/03/11/6NK70s.png)](https://imgtu.com/i/6NK70s)

- 添加页面

[![前端4](https://s3.ax1x.com/2021/03/11/6NKLt0.png)](https://imgtu.com/i/6NKLt0)

[![前端5](https://s3.ax1x.com/2021/03/11/6NKzX4.png)](https://imgtu.com/i/6NKzX4)

- 编辑

[![前端6](https://s3.ax1x.com/2021/03/11/6NMFtx.png)](https://imgtu.com/i/6NMFtx)

[![前端7](https://s3.ax1x.com/2021/03/11/6NMV1O.png)](https://imgtu.com/i/6NMV1O)

- 404

[![前端8](https://s3.ax1x.com/2021/03/11/6NMKHA.png)](https://imgtu.com/i/6NMKHA)


## SpringBoot Web开发

springboot 帮我们配置了什么？我们能不能修改，能修改哪些？能不能扩展？

- **xxxAutoConfiguration**  向容器中自动配置组件
- **xxxProperties** 自动配置类，装配配置文件中自定义的一些内容

要解决的问题

- 导入静态资源
- 首页
- SpringBoot默认不支持jsp，所以需要模板引擎 **Thymeleaf**
- 装配扩展SpringMVC
- 增删查改
- 拦截器
- 国际化



## 静态资源导入

- `package org.springframework.boot.autoconfigure.web.servlet;`

- `public class WebMvcAutoConfiguration`
- 里面有个`public static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer` 静态内部类
- 内部类内有`public void addResourceHandlers(ResourceHandlerRegistry registry)` 成员方法

```java
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    if (!this.resourceProperties.isAddMappings()) {
        logger.debug("Default resource handling disabled");
    } else {
        Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
        CacheControl cacheControl = this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl();
        if (!registry.hasMappingForPattern("/webjars/**")) {
            this.customizeResourceHandlerRegistration(registry.addResourceHandler(new String[]{"/webjars/**"}).addResourceLocations(new String[]{"classpath:/META-INF/resources/webjars/"}).setCachePeriod(this.getSeconds(cachePeriod)).setCacheControl(cacheControl));
        }
        String staticPathPattern = this.mvcProperties.getStaticPathPattern();
        if (!registry.hasMappingForPattern(staticPathPattern)) {
            this.customizeResourceHandlerRegistration(registry.addResourceHandler(new String[]{staticPathPattern}).addResourceLocations(WebMvcAutoConfiguration.getResourceLocations(this.resourceProperties.getStaticLocations())).setCachePeriod(this.getSeconds(cachePeriod)).setCacheControl(cacheControl));
        }
    }
}
```

- 如果静态资源应被自定义了，则写入"默认资源处理已禁用" 并结束

```java
if (!this.resourceProperties.isAddMappings()) {
    logger.debug("Default resource handling disabled");
}
```

在`public class WebMvcProperties` 中的成员方法有`private String staticPathPattern;` 初始化为`this.staticPathPattern = "/**";` 

可以在`application.properties` 中进行`spring.mvc.static-path-pattern`配置静态资源目录

如果自定义静态资源路径，程序会判断访问地址存不存在`/webjars/**` ，则会将地址映射到`/META-INF/resources/webjars/` 目录下找文件。这里的`/webjars/**` 是`Web Library in Jars` ，上官网可以采用maven坐标的等方式拉取需要的web Jar包。

在`Web Jars` 官网上下的jar包目录结构都是统一的。

`/META-INF/resources/webjars/` 程序都会在这个classpath下面找文件。



这里调用`getStaticPathPattern()` 取了 `public class WebMvcProperties` 下默认的`staticPathPattern` ，即`"/**"` 

```java
String staticPathPattern = this.mvcProperties.getStaticPathPattern();
```

然后判断

```java
if (!registry.hasMappingForPattern(staticPathPattern))
```

即，访问路径下的 /**，若没有处理器处理的话，都会进跳转到`this.resourceProperties.getStaticLocations()` 

继续跟踪下去会到 `package org.springframework.boot.autoconfigure.web;` 下的 `public class ResourceProperties` 中的成员变量 `private String[] staticLocations` ，在无参构造方法中会初始化为 `new String[]{"classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/"};` 

这样就存在四个目录可以直接访问到静态资源文件。即：

- `/META-INF/resources/`
- `/resources/`
- `/static/`
- `/public/`

可以创建这些目录，然后分别创建一些资源文件来验证系统默认的资源访问先后顺序，最终结论如下：

> 优先级顺序为：**META-INF/resources > resources > static > public**

一般来说 `public` 下放一些公共的资源比如 用户都会访问的 js 文件， `static` 放一些静态资源，如图片。`resources` 放一些上传upload文件。



## 首页

- templates文件夹一般会放视图模板，就是SpringBoot的动态页面，需要引入`Thymeleaf`组件
- templates目录下的所有页面**只能**通过controller来跳转！

外部浏览器默认只能访问到`static`文件夹下的静态资源，但是除`static`以外的所有文件夹下的文件都无法访问，因为它们需要 `视图解析器`访问。所以需要引入`Thymeleaf`组件来访问非static文件夹。



在 `public class WebMvcAutoConfiguration`里存在静态内部类

`public static class EnableWebMvcConfiguration`里面有如下成员方法



```java
@Bean
public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext) {
    WelcomePageHandlerMapping welcomePageHandlerMapping = new WelcomePageHandlerMapping(new TemplateAvailabilityProviders(applicationContext), applicationContext, this.getWelcomePage(), this.mvcProperties.getStaticPathPattern());
    welcomePageHandlerMapping.setInterceptors(this.getInterceptors());
    return welcomePageHandlerMapping;
}

private Optional<Resource> getWelcomePage() {
    String[] locations = WebMvcAutoConfiguration.getResourceLocations(this.resourceProperties.getStaticLocations());
    return Arrays.stream(locations).map(this::getIndexHtml).filter(this::isReadable).findFirst();
}

private Resource getIndexHtml(String location) {
    return this.resourceLoader.getResource(location + "index.html");
}

private boolean isReadable(Resource resource) {
    try {
        return resource.exists() && resource.getURL() != null;
    } catch (Exception var3) {
        return false;
    }
}
```



`welcomePageHandlerMapping()`中能找到 `this.mvcProperties.getStaticPathPattern()`自定义的静态目录，也能通过`this.getWelcomePage()`里的 `getIndexHtml()`找到当前目录下的 `index.html` 作为首页。



 ## Thymeleaf模板引擎

- Thymeleaf是用来开发Web和独立环境项目的服务器端的Java模版引擎

- Spring官方支持的服务的渲染模板中，并不包含jsp。而是Thymeleaf和Freemarker等，而Thymeleaf与SpringMVC的视图技术，及SpringBoot的自动化配置集成非常完美，几乎没有任何成本，你只用关注Thymeleaf的语法即可。

  pom.xml：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

### 常用语法：

1. 首先在html文件上方导入命名空间 `<html xmlns:th="http://www.thymeleaf.org">`
2. URL链接 如：href 前面加 `th:`标签，使用 `href="@{...}"`里面加上文件路径
3. 文本 `th:text=#{...}`
4. 变量 `${...}`
5. 选择表达式 `*{...}`



[![Thymeleaf语法](https://s3.ax1x.com/2021/03/07/6MNNKs.png)](https://imgtu.com/i/6MNNKs)



## 装配扩展 SpingMVC 

> 参考手册：https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-auto-configuration
>
> ### **30.1.1 Spring MVC Auto-configuration**



Spring Boot provides auto-configuration for Spring MVC that works well with most applications.

The auto-configuration adds the following features on top of Spring’s defaults:

- Inclusion of `ContentNegotiatingViewResolver` and `BeanNameViewResolver` beans.
- Support for serving static resources, including support for WebJars (covered [later in this document](https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-static-content))).
- Automatic registration of `Converter`, `GenericConverter`, and `Formatter` beans.
- Support for `HttpMessageConverters` (covered [later in this document](https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-message-converters)).
- Automatic registration of `MessageCodesResolver` (covered [later in this document](https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/boot-features-developing-web-applications.html#boot-features-spring-message-codes)).
- Static `index.html` support.
- Custom `Favicon` support (covered [later in this document](https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-favicon)).
- Automatic use of a `ConfigurableWebBindingInitializer` bean (covered [later in this document](https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/boot-features-developing-web-applications.html#boot-features-spring-mvc-web-binding-initializer)).

If you want to keep Spring Boot MVC features and you want to add additional [MVC configuration](https://docs.spring.io/spring/docs/5.1.19.RELEASE/spring-framework-reference/web.html#mvc) (interceptors, formatters, view controllers, and other features), you can add your own `@Configuration` class of type `WebMvcConfigurer` but **without** `@EnableWebMvc`. If you wish to provide custom instances of `RequestMappingHandlerMapping`, `RequestMappingHandlerAdapter`, or `ExceptionHandlerExceptionResolver`, you can declare a `WebMvcRegistrationsAdapter` instance to provide such components.

If you want to take complete control of Spring MVC, you can add your own `@Configuration` annotated with `@EnableWebMvc`.



### 自定义视图解析器 ViewResolver

自定义MVC配置类

```java
@Configuration
public class MyMvcConfig implements WebMvcConfigurer {
    
    //ViewResolver 实现了视图解析器接口的类，可以把它看做视图解析器
    @Bean
    public ViewResolver myViewResolver() {
        return new MyViewResolver();
    }

    //自己定义一个自己的视图解析器 MyViewResolver
    public static class MyViewResolver implements ViewResolver {
        @Override
        public View resolveViewName(String s, Locale locale) throws Exception {
            return null;
        }
    }
}
```

需要实现 `WebMvcConfigurer` 接口，同时**不能**加上注解 `@EnableWebMvc` 否则MVC被系统全面接管，自定义配置类失效。



看官方文档可以知道，SpringBoot默认实现是 `ContentNegotiatingViewResolver`和 ``BeanNameViewResolver` 这两个Bean。通过观察，它们都实现了 `ViewResolver`接口。

```java
public interface ViewResolver {
    @Nullable
    View resolveViewName(String var1, Locale var2) throws Exception;
}
```

即，实现了`resolveViewName(String var1, Locale var2)`方法。



在实现了自己的 `myViewResolver`之后，可以到`package org.springframework.web.servlet;`中的`public class DispatcherServlet extends FrameworkServlet`类中找`protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception`方法。里面会调用 `this.doDispatch(request, response);`。最终我们在`doDispatch`方法上打断点，然后程序运行卡住时，查看 `viewResolvers`这个变量，就能发现我们自定义的 `MyViewResolver`也注入进去了。



同理。重写`public void addViewControllers(ViewControllerRegistry registry)`可以完成自定义视图跳转



###  @EnableWebMvc注解原理

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({DelegatingWebMvcConfiguration.class})
public @interface EnableWebMvc {
}
```

`@Import({DelegatingWebMvcConfiguration.class})`，所以在@EnableWebMvc配置后，会自动加载`DelegatingWebMvcConfiguration.class`。而在`public class WebMvcAutoConfiguration`里面



```java
@ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
@ConditionalOnMissingBean({WebMvcConfigurationSupport.class})
@AutoConfigureOrder(-2147483638)
@AutoConfigureAfter({DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class, ValidationAutoConfiguration.class})
public class WebMvcAutoConfiguration {
    
}
```

`@ConditionalOnMissingBean({WebMvcConfigurationSupport.class})` 这里表示如果`WebMvcConfigurationSupport`注入时，则`WebMvcAutoConfiguration`不会被开启。

`public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport`注入`DelegatingWebMvcConfiguration `同时也会注入父类`WebMvcConfigurationSupport`，所以最终的结论是：

- 注入`@EnableWebMvc`会让自定义配置失效



# SpringBoot - Web项目 小Demo

- 静态html放在`templates`目录下
- css, img, js文件放在`static`目录下
- 从底层开始，创建 `pojo`
- 创建 `dao`
- 创建 `controller`
- 创建 `controller`
- 创建 `config`

## 初始化

本项目不涉及数据库的CRUD，而主要在于SpringBoot搭建简单的Web应用，所以Dao层都是用初始化的模拟数据进行，所以初始化详细查看源码。

- 登录密码的判断默认为 `123456` ，用户名随意

## 首页实现

- 在Controller里完成首页的跳转

```java
@Controller
public class IndexController {
    @RequestMapping({"/", "/index.html"})
    public String index() {
        return "index";
    }
}
```



- **一般在Config中配置完成资源跳转**

```java
@Configuration
public class MyMvcConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/index.html").setViewName("index");
    }
}
```

到这就可以完成index.html的加载，但是无法加载css的样式

- 要在html里面添加 `Thymeleaf` 命名空间，同时将 href 属性加上 `th:`完成模板引擎的控制。
- 在 `application.yml`中关掉 Thymeleaf 的缓存，让样式顺利加载

```xml
# 关闭模板引擎的缓存
spring.thymeleaf.cache=false
```

再进行下面配置，可以让地址变成 `localhost:8080/kuang`跳转首页，每一个css等文件路径前都会添加 `kuang`

```xml
server.servlet.context-path=/kuang # 修改首页的地址
```



## 国际化

- 在资源目录下创建文件夹 `i18n`，在里面创建 `login.properties`，`login_zh_CN.properties`和 `login_en_US.properties`三个配置文件
- 在左下角的菜单栏里有 `Resource Bundle`可以可视化配置信息。
- 在 `Settings`里配置 `File Encodings`里的编码都为 `UTF-8`



在  `public class MessageSourceAutoConfiguration`中负责自动配置默认的消息配置，里面引用了 ``public class MessageSourceProperties``的配置，其中包括文本名称`basename`，所以接下来要在 `application.yml`里面配置名称

```xml
# 我们的配置文件的真实位置
spring.messages.basename=i18n.login
```



- 在需要国际化的 html 文件中，在对应标签里加入 `th:text="#{...}"`，这里使用 `Thymeleaf`的文本形式，用 `#{}`的语法。



在 `public class WebMvcAutoConfiguration`里的静态内部类 `public static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer`里有成员方法`public LocaleResolver localeResolver()`，用来做 “地区解析器”

```java
public LocaleResolver localeResolver() {
    if (this.mvcProperties.getLocaleResolver() == org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties.LocaleResolver.FIXED) {
        return new FixedLocaleResolver(this.mvcProperties.getLocale());
    } else {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(this.mvcProperties.getLocale());
        return localeResolver;
    }
}
```

前半句 if 里判断用户是否自定义地区解析器，则使用用户自定义的。

else 使用官方实现的 `AcceptHeaderLocaleResolver`来做解析器。

模仿官方的 `public class AcceptHeaderLocaleResolver implements LocaleResolver`，我们可以自定义一个解析器类，实现 `LocaleResolver`接口，完成自定义。



### 自定义 地址解析器

```java
public class MyLocaleResolver implements LocaleResolver {

    //解析请求
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // 获取请求中的语言参数
        String language = request.getParameter("l");
        Locale locale = Locale.getDefault();    //如果没有就使用默认的
        //如果请求的链接携带了国际化参数
        if (!StringUtils.isEmpty(language)) { // 官方工具类，判null或空
            //zh_CN
            String[] split = language.split("_");
            //国家，地区
            locale = new Locale(split[0], split[1]);
        }
        return locale;
    }

    @Override
    public void setLocale(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Locale locale) {

    }
}
```

```html
<a class="btn btn-sm" th:href="@{/index.html(l='zh_CN')}">中文</a>
<a class="btn btn-sm" th:href="@{/index.html(l='en_US')}">English</a>
```

国际化页面上的更换语言，实际上是对服务器发送请求，所以使用 `th:href="@{/index.html(l='zh_CN')}"`， `l`是 `Parameter`，在自定义解析器中要先获取这个字符串，然后对字符串进行分割，如中文请求字符串分割成 `zh` 和 `CN`。然后 new Locale并传进去返回即可。



最后回到我们自定义的 `MyMvcConfig`中进行 地区解析器的注入。记得加上注解 `@Bean`

```java
@Bean
public LocaleResolver localeResolver() {
    return new MyLocaleResolver();
}
```



## 登录功能实现

- 在首页index.html中的 <form> 表单中，将 action 加上 Thymeleaf 标签，然后 加上请求路径，此路径与Controller的RequestMapping 匹配

```html
<form class="form-signin" th:action="@{/user/login}">
```

确保<form>里的<input>中 加上了 `name`属性，Controller可以通过此name值，获取参数。

```html
<input type="text" name="username" class="form-control" th:placeholder="#{login.username}" required="" autofocus="">
<input type="password" name="password" class="form-control" th:placeholder="#{login.password}" required="">
```

- 然后开始编写 `LoginController`

```java
@Controller
public class LoginController {
    @RequestMapping("/user/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password, Model model) {
        //具体业务
        if (!StringUtils.isEmpty(username) && "123456".equals(password)) {
            return "redirect:/main.html";
        } else {
            model.addAttribute("msg", "用户名或密码错误!");
            return "index";
        }
    }
}
```

参数加上注解 `@RequestParam`，后面跟上对应的form表单中的name值

为防止成功登录后的地址栏上有账号和密码的参数，所以采用重定向，此/main.html是自定义的，并不一定真实存在，然后在 自定义配置类中加上默认视图跳转到真实的页面中。

```
@Override
public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("index");
    registry.addViewController("/index.html").setViewName("index");
    registry.addViewController("/main.html").setViewName("dashboard");
}
```

登录失败，则在Model中添加键值对 {"msg", "用户名或密码错误!"}，然后回到 首页添加信息提示

```html
<!--如果msg的值为空，则不显示-->
<p style="color: red" th:text="${msg}" th:if="${not #strings.isEmpty(msg)}"></p>
```

`#strings.isEmpty(msg)`是Thymeleaf的高级写法，可以判断键值对是否为空，以此来判断是否将登录错误信息展示。



## 登录拦截器

- 如果用户直接输入 `localhost:8080/kuang/main.html`则可以直接进入主菜单，所以我们要添加一个拦截器，判断用户是否登录



- 在 `LoginController`添加一项，如果登录了，就添加`session`，后面根据此session的存在判断是否登录

```java
@RequestMapping("/user/login")
public String login(@RequestParam("username") String username,
                    @RequestParam("password") String password, Model model, HttpSession session) {
    //具体业务
    if (!StringUtils.isEmpty(username) && "123456".equals(password)) {
        session.setAttribute("loginUser", username);
        return "redirect:/main.html";
    } else {
        model.addAttribute("msg",  "用户名或密码错误!");
        return "index";
    }
}
```

- 自定义一个登录的拦截器

```java
public class LoginHandlerInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //登陆成功之后，应该有用户的session
        Object loginUser = request.getSession().getAttribute("loginUser");
        if (loginUser == null) { // 没有登录
            request.setAttribute("msg", "没有权限，请先登录");
            request.getRequestDispatcher("/index.html").forward(request, response);
            return false;
        } else {
            return true;
        }
    }
}
```

手动实现 `HandlerInterceptor`，并复写 `preHandle`方法，返回false则拦截，true则放行。

先获取用户session，判空，没登录则先设置回馈消息，然后`getRequestDispatcher`重定向回登录界面。

- 到 `dashboard.html`中取出session的值，放到欢迎界面，左上角的位置

```
<a class="navbar-brand col-sm-3 col-md-2 mr-0" href="http://getbootstrap.com/docs/4.0/examples/dashboard/#"> [[${session.loginUser}]] </a>
```



## 展示员工列表

- 创建员工控制类 `EmployeeController`

```java
@Controller
public class EmployeeController {
    @Autowired
    EmployeeDao employeeDao;

    @RequestMapping("/emps")
    public String list(Model model) {
        Collection<Employee> employees = employeeDao.getAll();
        model.addAttribute("emps", employees);
        return "emp/list";
    }
}
```

注入 `EmployeeDao`，添加映射 `@RequestMapping("/emps")`，添加查询结果到键值对中，然后返回到 视图 emp目录下的`list.html`

- 查看dashboard.html，发现有重复元素，如顶部导航栏和侧边栏，所以创建commons/commons.html将这个元素抽取出来

```html
<!--头部导航栏-->
<nav class="navbar navbar-dark sticky-top bg-dark flex-md-nowrap p-0" th:fragment="topbar">
```

```html
<!--侧边栏-->
<nav class="col-md-2 d-none d-md-block bg-light sidebar" th:fragment="sidebar">
```

`th:fragment` Thymeleaf模板用于抽取，后面的值自定义名称

- 回到 `list.html`里面对刚才抽取的重复元素删除，然后进行注入

```html
<!--顶部导航栏-->
<div th:replace="~{commons/commons::topbar}"></div>
```

```html
<!--传参给组件-->
<div th:replace="~{commons/commons::sidebar(active='list.html')}"></div>
```

注入的语法是：`th:replace=“~{path::name}”`后面的name可以用小括号接上参数，类似于Get方法中的问号传参。

- 在刚才抽取的 `commoms.html`中，侧边栏里，对当前所在页面进行侧边栏对应栏的高亮

```html
<a th:class="${active == 'main.html' ? 'nav-link active' : 'nav-link'}" th:href="@{/index.html}">
```

`'nav-link active'`是 bootstrap 里 class值，用于高亮，表示已选中样式，`nav-link`则是未选中的样式。然后进行路径跳转。

- 在员工管理一栏同样进行判断

```html
<a th:class="${active == 'list.html' ? 'nav-link active' : 'nav-link'}" th:href="@{/emps}">
```

这里的`${active}`就是 前面小括号传参的 key `active`，员工管理这一栏则是跳转到 `EmployeeController`进行数据查询。

- 在`list.html`在下面的资源展示中，thead补上pojo对应的属性，tbody采用Thymeleaf 的 for-each 遍历展示

```html
<tbody>
    <tr th:each="emp:${emps}">
        <td th:text="${emp.getId()}"></td>
        <!--<td>[[${emp.getLastName()}]]</td> 也可以这么写-->
        <td th:text="${emp.getLastName()}"></td>
        <td th:text="${emp.getEmail()}"></td>
        <td th:text="${emp.getGender() == 0 ? '女' : '男'}"></td>
        <td th:text="${emp.getDepartment().getDepartmentName()}"></td>
        <td th:text="${#dates.format(emp.getBirth(), 'yyyy-MM-dd HH:mm:ss')}"></td>
        <td>
            <button class="btn btn-sm btn-primary">编辑</button>
            <button class="btn btn-sm btn-danger">删除</button>
    	</td>
    </tr>html
</tbody>
```



## 添加员工实现

- 在 `list.html`里在左上角加上添加员工按钮，并加样式

```html
<h2><a class="btn btn-sm btn-success" th:href="@{/emp}">添加页面</a></h2>
```
- 提交到`/emp` 对应controller里进行数据查询
- 在 `EmployeeController`里加入对应逻辑判断

```java
@GetMapping("/emp") // get方式获取请求
public String toAddpage(Model model) {
    //查出所有部门的信息
    Collection<Department> departments = departmentDao.getDepartments();
    model.addAttribute("departments", departments);
    return "/emp/add";
}
```

- 这里采取get 方式获取请求 `@GetMapping`

- 将 `list.html`再复制一份 改为 `add.html`，然后添加表单样式，做一些修改，以下为表单样式。

```html
<form th:action="@{/emp}" method="post">
    <div class="form-group">
        <label>LastName</label>
        <input type="text" name="lastName" class="form-control" placeholder="kuangshen">
    </div>
    <div class="form-group">
        <label>Email</label>
        <input type="email" name="email" class="form-control" placeholder="1027093399@qq.com">
    </div>
    <div class="form-group">
        <label>Gender</label><br/>
        <div class="form-check form-check-inline">
            <input class="form-check-input" type="radio" name="gender" value="1">
            <label class="form-check-label">男</label>
        </div>
        <div class="form-check form-check-inline">
            <input class="form-check-input" type="radio" name="gender" value="0">
            <label class="form-check-label">女</label>
        </div>
    </div>
    <div class="form-group">
        <label>department</label>
        <select class="form-control" name="department.id"> <!--对应pojo是个对象，要传id，下面的value也是获取id-->
            <!--我们在controller接收的是一个Employee 所以我们需要提交的是其中的一个属性-->
            <option th:each="dept:${departments}" th:text="${dept.getDepartmentName()}" th:value="${dept.getId()}">1</option>
        </select>
    </div>
    <div class="form-group">
        <label>Birth</label>
        <input type="text" name="birth" class="form-control" pl aceholder="kuangstudy">
    </div>
    <button type="submit" class="btn btn-primary">添加</button>
</form>
```

- 值得注意的是，在传部门 `department`时，由于 `pojo`里对应的是一个对象，而前台传过去不能直接传对象，所以要传部门对象的其中一个属性，这里选择传 `id`，所以 `name="department.id"`。
- 然后采用 `Thymeleaf`的for-each语法进行循环，将`option`所有部门展现出来，注意：这里的 `value`也是需要取出 `dept`的id，道理跟上面一样

```html
<option th:each="dept:${departments}" th:text="${dept.getDepartmentName()}" th:value="${dept.getId()}">1</option>
```

- 然后修改 `form`表单提交的 `action`

```html
<form th:action="@{/emp}" method="post">
```

```java
@PostMapping("/emp") // post方式获取请求
public String addEmp(Employee employee) {
    //添加操作 forward
    employeeDao.save(employee); //调用底层业务方法保存员工信息
    return "redirect:/emps";
}
```

- 这里的`/emp`与上面的get方式获取请求重复了，这里采用post请求 `@PostMapping`，这里就是 `REST风格`

- 表单提交之后，用 `save()`进行保存，然后重定向到 `/emps`。重新对数据库中的所有 `employee`查找出来。然后会跳转会 `list.html`

```html
<tbody>
    <tr th:each="emp:${emps}">
        <td th:text="${emp.getId()}"></td>
        <!--<td>[[${emp.getLastName()}]]</td> 也可以这么写-->
        <td th:text="${emp.getLastName()}"></td>
        <td th:text="${emp.getEmail()}"></td>
        <td th:text="${emp.getGender() == 0 ? '女' : '男'}"></td>
        <td th:text="${emp.getDepartment().getDepartmentName()}"></td>
        <td th:text="${#dates.format(emp.getBirth(), 'yyyy-MM-dd HH:mm:ss')}"></td>
        <td>
            <button class="btn btn-sm btn-primary">编辑</button>
            <button class="btn btn-sm btn-danger">删除</button>
        </td>
    </tr>
</tbody>
```

- 然后也是 for-each 进行 emp 的遍历展示，到此添加员工功能就实现了。



- 在添加员工里的生日填写时，存在一个日期格式化的问题。

- 在 `WebMvcProperties`类中的 `dateFormat`默认是 `yyyy/MM/dd`格式，可以在 `application.yml`进行格式的配置。

```yml
#时间日期格式化
spring.mvc.date-format=yyyy-MM-dd
```



## 修改员工信息

- 创建 `update.html`作为修改界面，大致参考 `add.html`即可

- 将 `list.html`编辑按钮修改成超链接 

```html
<a class="btn btn-sm btn-primary" th:href="@{/emp/}+${emp.getId()}">编辑</a>
```

- 中间使用加号拼接 emp的id，用语 `controller`里找到部门信息。

- 编写 `Controller`

```java
//去员工修改页面
@GetMapping("/emp/{id}")
public String toUpdateEmp(@PathVariable("id") Integer id, Model model) {
    //查出原来的数据
    Employee employee = employeeDao.getEmployeeById(id);
    model.addAttribute("emp", employee);
    //查出所有部门的信息
    Collection<Department> departments = departmentDao.getDepartments();
    model.addAttribute("departments", departments);
    return "emp/update";
}
```

- get方式获取，后面带上 emp的 id，并赋值给参数 id，前面加上注解。

根据id获取原数据，再查找一遍部门信息，回到 `update.html`

- 给 `<input>`上面添加 `th:value()`信息，默认帮用户填好原来的信息。

- 注意填回日期的格式问题

```html
<input th:value="${#dates.format(emp.getBirth(), 'yyyy-MM-dd HH:mm')}" type="text" name="birth" class="form-control" pl aceholder="kuangstudy">
```



## 删除功能

- 改 `list.html`
- 添加 `Controller`对应删除功能

```html
<a class="btn btn-sm btn-danger" th:href="@{/deleteemp/}+${emp.getId()}">删除</a>
```

```java
//删除员工
@GetMapping("/deleteemp/{id}")
public String deleteEmp(@PathVariable("id")int id) {
    employeeDao.delete(id);
    return "redirect:/emps";
}
```



## 404页面处理

- `templates`文件夹下创建 `error`文件夹，里面放 `404.html`，springboot会自动识别。



## 注销功能

- `commons.html`里 注销按钮绑定 `controller`

```html
<a class="nav-link" th:href="@{/user/logout}">注销</a>
```

- `LoginController`添加注销功能

```java
//注销
@RequestMapping("/user/logout")
public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/index.html";
}
```



## 结尾

至此这个小 Demo 就 Over 了。这算是我学习 springboot 的入门Demo了。

> 鸣谢：狂神说。https://www.kuangstudy.com/