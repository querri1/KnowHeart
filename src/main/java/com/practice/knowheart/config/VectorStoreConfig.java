package com.practice.knowheart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    @ConfigurationProperties("knowheart.vector.datasource")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource() {
        return vectorDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }

    @Bean(name = "loveVectorStore")
    @Primary
    public VectorStore loveVectorStore(
            EmbeddingModel embeddingModel,
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            LoveDocumentLoader documentLoader,
            @Value("${knowheart.vector.table-name:love_knowledge}") String tableName,
            @Value("${knowheart.vector.dimensions:1536}") int dimensions) {

        preparePgVectorDatabase(vectorJdbcTemplate, tableName);

        PgVectorStore vectorStore = PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .dimensions(dimensions)
                .initializeSchema(true)
                .build();
        // build() 不会建表；schema 初始化在 InitializingBean.afterPropertiesSet() 中执行，
        // 而 @Bean 工厂方法内 add() 早于 Spring 生命周期回调，需手动触发一次。
        vectorStore.afterPropertiesSet();

        if (hasExistingVectors(vectorJdbcTemplate, tableName)) {
            return vectorStore;
        }

        log.info("PGVector 为空，开始初始化 RAG 知识库...");
        var documents = documentLoader.loadMarkdowns();
        if (documents.isEmpty()) {
            log.warn("未找到知识库文档，跳过向量化");
            return vectorStore;
        }

        int batchSize = 20;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            vectorStore.add(documents.subList(i, end));
            log.info("向量化进度: {}/{}", end, documents.size());
        }
        log.info("RAG 知识库已持久化到 PGVector，共 {} 个片段", documents.size());
        return vectorStore;
    }

    /**
     * 启用 pgvector 扩展，并修复因扩展未安装而创建的错误表结构。
     */
    private void preparePgVectorDatabase(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector 扩展已就绪");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "无法启用 pgvector 扩展。请确认 PostgreSQL 已安装 pgvector 插件，"
                            + "并在 knowheart_vector 库中执行: CREATE EXTENSION vector;"
                            + " 详见 sql/init_pgvector.sql", e);
        }

        try {
            String embeddingType = jdbcTemplate.queryForObject(
                    """
                    SELECT udt_name FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = 'embedding'
                    """,
                    String.class,
                    tableName);
            if (embeddingType != null && !"vector".equals(embeddingType)) {
                log.warn("表 {} 的 embedding 列类型为 {}（应为 vector），将删除后重建", tableName, embeddingType);
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
            }
        } catch (Exception e) {
            log.debug("向量表尚未创建: {}", e.getMessage());
        }
    }

    private boolean hasExistingVectors(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName, Integer.class);
            if (count != null && count > 0) {
                log.info("PGVector 已有 {} 条向量，跳过重新加载", count);
                return true;
            }
        } catch (Exception e) {
            log.debug("向量表暂不可查询: {}", e.getMessage());
        }
        return false;
    }
}
