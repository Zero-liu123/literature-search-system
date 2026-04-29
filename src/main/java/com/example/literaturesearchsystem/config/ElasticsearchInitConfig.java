package com.example.literaturesearchsystem.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchInitConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchInitConfig.class);

    private final ElasticsearchClient client;

    public ElasticsearchInitConfig(ElasticsearchClient client) {
        this.client = client;
    }

    @PostConstruct
    public void initIndex() {
        try {

            // 只在索引不存在时才创建，避免每次启动丢失数据
            if (client.indices().exists(e -> e.index("literature")).value()) {
                log.info("✅ ES索引 literature 已存在，跳过创建");
                return;
            }

            // 创建新索引（关键：completion）
            CreateIndexResponse response = client.indices().create(c -> c
                    .index("literature")
                    .mappings(m -> m
                            .properties("title", p -> p.text(t -> t))
                            .properties("authors", p -> p.text(t -> t))
                            .properties("abstractText", p -> p.text(t -> t))
                            .properties("keywords", p -> p.text(t -> t))
                            .properties("suggest", p -> p
                                    .completion(cs -> cs)
                            )
                    )
            );

            log.info("✅ ES索引创建成功: {}", response.index());

        } catch (Exception e) {
            log.error("❌ ES索引初始化失败", e);
        }
    }
}
