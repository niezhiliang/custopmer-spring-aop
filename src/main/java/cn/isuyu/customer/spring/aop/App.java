package cn.isuyu.customer.spring.aop;

import cn.isuyu.customer.spring.aop.annotations.EnableCustomerAop;
import cn.isuyu.customer.spring.aop.service.IndexService;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 上午10:49
 */
@Configurable
@ComponentScan(value = "cn.isuyu.customer.spring.aop")
@EnableCustomerAop
public class App {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(App.class);
        IndexService indexService = applicationContext.getBean(IndexService.class);
        indexService.hello();
    }

}
