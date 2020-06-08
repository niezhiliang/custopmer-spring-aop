package cn.isuyu.customer.spring.aop.aop;

import cn.isuyu.customer.spring.aop.annotations.After;
import cn.isuyu.customer.spring.aop.annotations.Around;
import cn.isuyu.customer.spring.aop.annotations.Before;
import cn.isuyu.customer.spring.aop.annotations.CustomerAop;
import org.springframework.stereotype.Component;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 上午11:03
 */
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
