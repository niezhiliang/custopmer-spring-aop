package cn.isuyu.customer.spring.aop.selector;

import cn.isuyu.customer.spring.aop.processes.AopBeanFactoryPostProcess;
import cn.isuyu.customer.spring.aop.processes.AopBeanPostProcess;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @Author NieZhiLiang
 * @Email nzlsgg@163.com
 * @GitHub https://github.com/niezhiliang
 * @Date 2020/6/8 下午12:31
 * 这个类可以动态的将某个Bean添加到
 * 利用@Import可以实现可插拔添加Bean
 */
public class MyImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{AopBeanFactoryPostProcess.class.getName(), AopBeanPostProcess.class.getName()};
    }
}
