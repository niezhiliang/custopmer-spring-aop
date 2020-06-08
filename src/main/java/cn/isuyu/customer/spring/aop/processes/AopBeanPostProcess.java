package cn.isuyu.customer.spring.aop.processes;

import cn.isuyu.customer.spring.aop.proxy.MyMethodInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;

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
