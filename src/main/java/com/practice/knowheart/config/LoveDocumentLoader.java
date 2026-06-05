package com.practice.knowheart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LoveDocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(LoveDocumentLoader.class);

    private final int chunkSize;
    private final int chunkOverlap;

    public LoveDocumentLoader(
            @Value("${knowheart.rag.chunk-size:500}") int chunkSize,
            @Value("${knowheart.rag.chunk-overlap:100}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 读取 resources/documents 下所有 Markdown，切分后供向量库索引。
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:documents/*.md");

            if (resources.length == 0) {
                log.warn("未找到知识库文档，请在 src/main/resources/documents/ 下添加 .md 文件");
                return List.of();
            }

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                log.info("加载知识库文档: {}", filename);

                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(true)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                List<Document> documents = reader.get();

                for (Document doc : documents) {
                    doc.getMetadata().put("source", filename);
                    doc.getMetadata().put("category", extractCategory(filename));
                }

                allDocuments.addAll(documents);
                log.info("  → 解析出 {} 个段落", documents.size());
            }

            if (allDocuments.isEmpty()) {
                log.warn("文档解析结果为空");
                return List.of();
            }

            TextSplitter textSplitter = new TokenTextSplitter(
                    chunkSize, chunkOverlap, 5, 10000, true);
            List<Document> chunkedDocuments = textSplitter.split(allDocuments);

            log.info("知识库共 {} 个原始段落，切分后 {} 个片段", allDocuments.size(), chunkedDocuments.size());
            return chunkedDocuments;

        } catch (Exception e) {
            log.error("加载知识库文档失败", e);
            throw new IllegalStateException("知识库文档加载失败: " + e.getMessage(), e);
        }
    }

    private String extractCategory(String filename) {
        if (filename == null) {
            return "general";
        }
        // 恋爱常见问题-单身篇.md → 单身篇
        int dash = filename.lastIndexOf('-');
        int dot = filename.lastIndexOf('.');
        if (dash >= 0 && dot > dash) {
            return filename.substring(dash + 1, dot);
        }
        return filename.replace(".md", "");
    }
}
