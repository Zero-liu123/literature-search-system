package com.example.literaturesearchsystem.es.repository;

import com.example.literaturesearchsystem.es.document.LiteratureDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiteratureEsRepository extends ElasticsearchRepository<LiteratureDocument, Long> {
}