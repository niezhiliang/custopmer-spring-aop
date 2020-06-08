### 手写AOP

#### 实现思路

我们先看下我们平常是如何使用spring的Aop,下面代码是一个比较简单的aop的前置通知的使用。

```java
@Component
@Aspect
public class BeforeAspect {

    @Before("execution(public * com.test.controller..*.*(..))")
    public void before(JoinPoint joinPoint) {
    }
}
```
首先我们看类注解，`@Component`、`@Aspect` 第一个是将该类作为一个bean，交给容器去管理，第二个注解的意思就是告诉容器，我这个类是
一个Aop的类，在加载bean的时候需要做特定的操作。我们再来看方法上的注解`@Before`，这个是Aop的前置通知方法，表示我这个方法是所有符
合我定义的切面条件，在它们本身代码执行之前要先执行。然后注解里面就是定义的条件(具体意思就是所有`com.test.controller`的方法都需
符合我的条件，都需要执行下面这个方法)，然后是方法的参数`JoinPoint`，这个类能够帮我们获取很多请求的信息，比如参数、请求的方法等
具体的自行去了解一下。

我们根据上面使用方法，来解析Aop是如何实现的，

首先定义一个类，使用特定的注解告诉Spring容器这个类是一个Aop类，然后通过注解告诉容器，在加载类的时候，符合我条件的我需要对它进行
方法增强，在执行原本方法之前或之后动态织入一些逻辑代码，然后再交给容器去管理。

#### 思路上遇到的问题

- 如何定义切面？
我们就模仿Spring来写，首先使用一个注解标记该类是一个Aop类，它是使用`@Before`、`@Around`、`@After`来告诉容器具体织入的逻辑是在
原方法的之前，还是之后，还是前后都需要执行。我们这里就做的简单一些，我们也定义四个注解`@CustomerAop`、`@Before`、`@Around`、
`@After`，通过`@CustomerAop`来告诉容器，这个类是Aop的类，然后`@Before`、`@Around`、`@After`分别表示方法执行之前、方法执行
前后、方法执行后执行。Spring的Aop中条件匹配功能太多，有注解的，有匹配粒度到类的、甚至匹配的粒度还能到方法和参数、我们这里就做的简单
点，匹配某个包下面的所有类注解如下：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Before {

    String value() default "";

}
```

我们先把我们的Aop的类写出来，然后再围绕这个类展开编写。具体代码如下：

```java
@CustomerAop
@Component
public class AspectLogic {

    @Before(value = "cn.isuyu.customer.spring.aop.service")
    public void before() {
        System.out.println("before----------------------");
    }

    @After(value = "cn.isuyu.customer.spring.aop.service")
    public void after() {
        System.out.println("after----------------------");
    }

    @Around(value = "cn.isuyu.customer.spring.aop.service")
    public void around() {
        System.out.println("around----------------------");
    }

}
```

- 在什么时候去匹配符合切面的bean

这个肯定是在容器扫描完所有类的时候，然后我们去判断哪些类加了我们的自定义注解`@CustomerAop`,然后我们去解析这个类，看下这个类有哪些
注解(`@Before`、`@Around`、`@After`)，并看他们的匹配条件是啥。然后将他们都记录下来，问题又来了，我从哪能知道Spring容器已经将所
有的Bean加载完了，Spring有没有提供这种口子，我去实现预留的接口，然后把加载到的类全部返回回来，Spring还真有这种这种接口，一个叫
`BeanFactoryPostProcessor`的后置处理器，实现这个接口，会直接把Bean工厂都返回过来，我们拿到所有扫描到的beanName,然后去获取对应
的`BeanDefinition`(spring用来描述Bean的数据接口，这个类可以获取Bean的作用域，是否懒加载，具体可以点开这个类去看)，不过我们要的
不是这个类，而是这个类的子类`AnnotatedBeanDefinition`，因为我们是基于注解将类给容器管理的。

```java
public class AopBeanFactoryPostProcess implements BeanFactoryPostProcessor {

    public static volatile Map<String, List<ProxyBeanHolder>> proxyBeanHolders = new ConcurrentHashMap<>();

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        //拿到容器中所有被加载的类名
        String[] beanDefinitionNames = configurableListableBeanFactory.getBeanDefinitionNames();
        //遍历获取到当前bean的容器中的数据结构BeanDefinition
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(beanDefinitionName);
            //因为BeanDefinition的子类有很多，因为我们是基于注解扫描的，所以我们要匹配AnnotatedBeanDefinition的Bean
            if (beanDefinition instanceof  AnnotatedBeanDefinition) {
                //拿到Bean类上修饰的注解
                AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
                //获取修饰类所有注解的名称集合
                Set<String> annotationTypes = annotationMetadata.getAnnotationTypes();
                for (String annotationType : annotationTypes) {
                    //筛选出加了标记为Aop类的Bean,然后去解析这个类
                    if (annotationType.equals(CustomerAop.class.getName())) {
                        doSacn((GenericBeanDefinition) beanDefinition);
                    }
                }
            }
        }
    }

    /**
     * 解析加了@CustomerAop的类
     * @param beanDefinition
     */
    private void doSacn(GenericBeanDefinition beanDefinition) {
        try {
            Class clazz = Class.forName(beanDefinition.getBeanClassName());
            //获取当前Bean所有的方法然后遍历判断是否加了自定义Aop的通知注解
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //获取类上所有的
                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                    if(annotation.annotationType().getName().equals(Before.class.getName())
                    || annotation.annotationType().getName().equals(Around.class.getName())
                    || annotation.annotationType().getName().equals(After.class.getName())) {
                        doScan(method,annotation);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析方法上的通知注解的值，
     * 然后将需要增强的类都找出来
     * 然后放到我们自己定义的数据
     * 结构中，存好以后方便
     * BeanPostProcess来使用
     * @param method
     * @param annotation
     */
    private void doScan(Method method,Annotation annotation){
        //获取注解所有的方法
        Method[] methods = annotation.annotationType().getMethods();
        //遍历注解的值得到需要增强的包路径
        //然后以类名为key，增强代码路径用ProxyBeanHolder来
        //修饰，然后将其添加到Map中
        for (Method annotationMethod : methods) {
            if (annotationMethod.getName().equals("value")) {
                try {
                    //得到通知注解上的包路径
                    String packageName = annotationMethod.invoke(annotation,null).toString();
                    if (!packageName.isEmpty()) {
                        //得到当前类的根目录
                        String rootPath = this.getClass().getResource("/").getPath().replaceAll("\\.","/");
                        //获取注解包下面所有的文件，然后将其类名为key存放到map中，这里会将该类需要增强的方法路径全部存到集合中
                        File file = new File(rootPath + packageName.replaceAll("\\.","/"));
                        for (File listFile : file.listFiles()) {
                            if (listFile.isFile()) {
                                String className = packageName + "." + listFile.getName().replaceAll(".class","");
                                if (!proxyBeanHolders.containsKey(className)) {
                                    List<ProxyBeanHolder> proxyBeanHolderList = new Vector<>();
                                    proxyBeanHolders.put(className,proxyBeanHolderList);
                                }
                                ProxyBeanHolder proxyBeanHolder = new ProxyBeanHolder();
                                proxyBeanHolder.setMethod(method.getName());
                                proxyBeanHolder.setAnnotationName(annotation.annotationType().getName());
                                proxyBeanHolder.setClassName(method.getDeclaringClass().getName());
                                proxyBeanHolders.get(className).add(proxyBeanHolder);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
```
- 什么时候对bean进行代理

既然有能返回BeanFactory的后置处理器，那肯定也有返回Bean的后置处理器，我们在容器返回BeanFactory的时候，已经将所有的Aop类都添加到了
我们定义的Map集合中，现在我们需要做的就是将Bean返回回来，然后插手Bean的生成，通过CGLIB代理来将通知的代码来动态织入到类中，具体代码如下
：
```java
/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 上午11:50
 * 该类可以插手Bean的生成过程，就是对该类进行代理，
 * 然后返回容器一个代理类，后面我们每次拿到的都是
 * 代理类。
 */
public class AopBeanPostProcess implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
         String targetName = bean.getClass().getName();
         //判断当前bean是否存在需要增强的类Map中，如果存在我们就用CGLIB来进行代理，然后返回一个
        //代理类给容器，容器
         if (AopBeanFactoryPostProcess.proxyBeanHolders.containsKey(targetName)) {
             Enhancer enhancer = new Enhancer();
             enhancer.setSuperclass(bean.getClass());
             enhancer.setCallback(new MyMethodInterceptor(AopBeanFactoryPostProcess.proxyBeanHolders.get(targetName)));
             bean =  enhancer.create();
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
```

到此我们的手写的Aop已经完成了99%啦，实际上Spring的Aop是可以通过注解来控制打开还是关闭，我记得以前打开Aop还需要加上注解`@EnableAspectJAutoProxy`,
,实际上就是不加这个注解，我就不把我们写的后置处理器的类交给Spring容器管理，如果有这个注解我就把我自定义的处理器放到容器中嘛。

### 动态打开关闭Aop

动态添加bean到容器中，Spring自己的Aop使用的是注解，然后注解上使用了一个`@Import`,将一个实现了`ImportBeanDefinitionRegistrar`接口的类，动态添加到
容器中，`ImportBeanDefinitionRegistrar`这也是Spirng的后置处理器之一，我们这里使用Spring的另一个后置处理器，`ImportSelector`,这个也能动态把类交给
容器去处理。

Spring代码如下：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {
    //控制是代理的实现方式 cglib和jdk动态代理
	boolean proxyTargetClass() default false;
    
	boolean exposeProxy() default false;

}
```

```java
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
        //注册了一个基于注解的自动代理创建器   AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy != null) {
            //表示强制指定了要使用CGLIB
			if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
            //强制暴露Bean的代理对象到AopContext
			if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

}
```

我们也写一个自己的注解来控制Aop的打开和关闭，`@EnableCustomerAop`,我们这里就简单的做个开关功能。具体代码如下：
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MyImportSelector.class)
public @interface EnableCustomerAop {
}
```

动态将我们自定义的处理器交给容器管理的类：
```java
public class MyImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{AopBeanFactoryPostProcess.class.getName(), AopBeanPostProcess.class.getName()};
    }
}
```






