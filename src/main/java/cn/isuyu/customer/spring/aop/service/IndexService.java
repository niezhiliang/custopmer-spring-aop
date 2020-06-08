package cn.isuyu.customer.spring.aop.service;

import org.springframework.stereotype.Component;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 上午10:54
 */
@Component
public class IndexService {

    public void hello() {
        System.out.println("hello aop");
    }


    public void hello2() {
        System.out.println("hello aop2");
    }

}
