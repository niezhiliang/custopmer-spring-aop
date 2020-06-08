package cn.isuyu.customer.spring.aop.processes;

import cn.isuyu.customer.spring.aop.holders.ProxyBeanHolder;
import cn.isuyu.customer.spring.aop.annotations.After;
import cn.isuyu.customer.spring.aop.annotations.Around;
import cn.isuyu.customer.spring.aop.annotations.Before;
import cn.isuyu.customer.spring.aop.annotations.CustomerAop;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 上午10:55
 */
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
}
