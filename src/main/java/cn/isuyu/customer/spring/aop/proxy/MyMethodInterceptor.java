package cn.isuyu.customer.spring.aop.proxy;

import cn.isuyu.customer.spring.aop.holders.ProxyBeanHolder;
import cn.isuyu.customer.spring.aop.annotations.After;
import cn.isuyu.customer.spring.aop.annotations.Around;
import cn.isuyu.customer.spring.aop.annotations.Before;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 下午12:26
 */
public class MyMethodInterceptor implements MethodInterceptor {

    private List<ProxyBeanHolder> proxyBeanHolders;

    public MyMethodInterceptor(List<ProxyBeanHolder> proxyBeanHolders) {
        this.proxyBeanHolders = proxyBeanHolders;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        //这里是在原方法之前织入前置通知和环绕通知
        for (ProxyBeanHolder proxyBeanHolder : proxyBeanHolders) {
            String annotationName = proxyBeanHolder.getAnnotationName();
            if (annotationName.equals(Before.class.getName()) || annotationName.equals(Around.class.getName())) {
                Class  clazz  = Class.forName(proxyBeanHolder.getClassName());
                Method method1 = clazz.getMethod(proxyBeanHolder.getMethod());
                method1.invoke(clazz.newInstance(),null);
            }
        }
        //执行原方法
        Object result = methodProxy.invokeSuper(o,objects);

        //这里是在原方法之前织入后置通知和环绕通知
        for (ProxyBeanHolder proxyBeanHolder : proxyBeanHolders) {
            String annotationName = proxyBeanHolder.getAnnotationName();
            if (annotationName.equals(After.class.getName()) || annotationName.equals(Around.class.getName())) {
                Class  clazz  = Class.forName(proxyBeanHolder.getClassName());
                Method method1 = clazz.getMethod(proxyBeanHolder.getMethod());
                method1.invoke(clazz.newInstance(),null);
            }
        }
        return result;
    }
}
