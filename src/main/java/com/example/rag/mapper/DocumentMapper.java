package com.example.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档Mapper接口
 * 提供文档数据的CRUD操作
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
    // BaseMapper已提供基础的CRUD方法
    // 如需自定义SQL，可在此添加方法并在对应的XML文件中实现
}
