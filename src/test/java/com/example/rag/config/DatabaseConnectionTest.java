package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库连接测试
 * 验证 PostgreSQL 数据库连接和 MyBatis-Plus 配置是否正确
 */
@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试数据库连接是否成功
     */
    @Test
    void testDatabaseConnection() throws SQLException {
        assertNotNull(dataSource, "DataSource should not be null");
        
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");
            
            // 验证连接的数据库
            String catalog = connection.getCatalog();
            assertEquals("rag_system", catalog, "Should connect to rag_system database");
            
            System.out.println("✓ Database connection successful!");
            System.out.println("  Database: " + catalog);
            System.out.println("  URL: " + connection.getMetaData().getURL());
        }
    }

    /**
     * 测试 JdbcTemplate 是否可用
     */
    @Test
    void testJdbcTemplate() {
        assertNotNull(jdbcTemplate, "JdbcTemplate should not be null");
        
        // 执行简单查询验证连接
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result, "Query should return 1");
        
        System.out.println("✓ JdbcTemplate is working!");
    }

    /**
     * 测试数据库版本
     */
    @Test
    void testDatabaseVersion() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertNotNull(version, "Database version should not be null");
        assertTrue(version.contains("PostgreSQL"), "Should be PostgreSQL database");
        
        System.out.println("✓ Database version: " + version);
    }

    /**
     * 测试表是否存在
     */
    @Test
    void testTablesExist() {
        // 检查 document 表是否存在
        Integer documentTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document'",
            Integer.class
        );
        assertTrue(documentTableCount > 0, "Document table should exist");
        
        // 检查 document_chunk 表是否存在
        Integer chunkTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_chunk'",
            Integer.class
        );
        assertTrue(chunkTableCount > 0, "DocumentChunk table should exist");
        
        System.out.println("✓ Required tables exist!");
    }
}
