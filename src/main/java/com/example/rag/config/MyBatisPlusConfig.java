package com.example.rag.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 配置分页插件和自动填充功能
 */
@Configuration
@MapperScan("com.example.rag.mapper")
public class MyBatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 添加分页插件，指定数据库类型为 PostgreSQL
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        
        // 设置请求的页面大于最大页后操作，true 调回到首页，false 继续请求，默认 false
        paginationInterceptor.setOverflow(false);
        
        // 设置单页分页条数限制，默认无限制
        paginationInterceptor.setMaxLimit(500L);
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }
}
