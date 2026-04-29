package com.example.literaturesearchsystem.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.literaturesearchsystem.dto.LiteratureSearchDTO;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.es.document.LiteratureDocument;
import com.example.literaturesearchsystem.es.repository.LiteratureEsRepository;
import com.example.literaturesearchsystem.mapper.LiteratureMapper;
import com.example.literaturesearchsystem.service.SearchService;
import com.example.literaturesearchsystem.vo.SearchResultVO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    // ⭐ 手动添加 log 变量
    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    // ⭐ 手动添加构造函数依赖
    private final LiteratureEsRepository literatureEsRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final LiteratureMapper literatureMapper;
    private final ElasticsearchClient elasticsearchClient;

    // ⭐ 手动添加构造函数
    public SearchServiceImpl(LiteratureEsRepository literatureEsRepository,
                             ElasticsearchOperations elasticsearchOperations,
                             LiteratureMapper literatureMapper,
                             ElasticsearchClient elasticsearchClient) {
        this.literatureEsRepository = literatureEsRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.literatureMapper = literatureMapper;
        this.elasticsearchClient = elasticsearchClient;
    }

    @PostConstruct
    public void init() {
        syncAllToEs();
    }


    // =============================
    // ⭐ 搜索（稳定版：手动高亮）
    // =============================
    @Override
    public SearchResultVO search(LiteratureSearchDTO searchDTO) {

        String keyword = searchDTO.getKeyword() == null ? "" : searchDTO.getKeyword().trim();
        int page = searchDTO.getPage() == null ? 1 : searchDTO.getPage();
        int size = searchDTO.getSize() == null ? 10 : searchDTO.getSize();

        String journal = (searchDTO.getJournal() != null && !searchDTO.getJournal().trim().isEmpty()) ? searchDTO.getJournal().trim() : null;
        String category = (searchDTO.getCategory() != null && !searchDTO.getCategory().trim().isEmpty()) ? searchDTO.getCategory().trim() : null;
        Integer startYear = searchDTO.getStartYear();
        Integer endYear = searchDTO.getEndYear();

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> {
                    boolean hasKeyword = keyword != null && !keyword.isEmpty();
                    boolean hasAuthor = searchDTO.getAuthor() != null && !searchDTO.getAuthor().trim().isEmpty();
                    boolean hasJournal = journal != null;
                    boolean hasCategory = category != null;
                    boolean hasYearRange = startYear != null || endYear != null;

                    return q.bool(b -> {

                        // keyword 搜索
                        if (hasKeyword) {
                            b.must(m -> m.bool(bb -> bb
                                    .should(s -> s.matchPhrase(mp -> mp.field("title").query(keyword)))
                                    .should(s -> s.matchPhrase(mp -> mp.field("keywords").query(keyword)))
                                    .should(s -> s.matchPhrase(mp -> mp.field("abstractText").query(keyword)))
                                    .minimumShouldMatch("1")
                            ));
                        }

                        // 作者筛选
                        if (hasAuthor) {
                            b.must(m -> m.matchPhrase(mm -> mm
                                    .field("authors")
                                    .query(searchDTO.getAuthor().trim())
                            ));
                        }

                        // 期刊筛选
                        if (hasJournal) {
                            b.must(m -> m.matchPhrase(mm -> mm
                                    .field("journal")
                                    .query(journal)
                            ));
                        }

                        // 分类筛选
                        if (hasCategory) {
                            b.must(m -> m.matchPhrase(mm -> mm
                                    .field("category")
                                    .query(category)
                            ));
                        }

                        // 年份范围筛选
                        if (hasYearRange) {
                            b.must(m -> m.range(r -> {
                                r.field("publishYear");
                                if (startYear != null) r.gte(co.elastic.clients.json.JsonData.of(startYear));
                                if (endYear != null) r.lte(co.elastic.clients.json.JsonData.of(endYear));
                                return r;
                            }));
                        }

                        // 如果什么都没有 → matchAll
                        if (!hasKeyword && !hasAuthor && !hasJournal && !hasCategory && !hasYearRange) {
                            b.must(m -> m.matchAll(ma -> ma));
                        }

                        return b;
                    });
                })

                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<LiteratureDocument> searchHits =
                elasticsearchOperations.search(query, LiteratureDocument.class);

        List<SearchResultVO.SearchHitVO> records =
                searchHits.getSearchHits().stream()
                        .map(hit -> {

                            LiteratureDocument doc = hit.getContent();
                            SearchResultVO.SearchHitVO vo = new SearchResultVO.SearchHitVO();

                            // ⭐ 手动高亮
                            String title = highlight(doc.getTitle(), keyword);
                            String abstractText = highlight(doc.getAbstractText(), keyword);

                            vo.setId(doc.getId());
                            vo.setTitle(title);
                            vo.setAuthors(doc.getAuthors());
                            vo.setAbstractText(abstractText);
                            vo.setPublishYear(doc.getPublishYear());
                            vo.setJournal(doc.getJournal());
                            vo.setKeywords(doc.getKeywords());
                            vo.setDoi(doc.getDoi());
                            vo.setCategory(doc.getCategory());
                            vo.setViewCount(doc.getViewCount());
                            vo.setScore(hit.getScore());

                            return vo;
                        })
                        .collect(Collectors.toList());

        SearchResultVO result = new SearchResultVO();
        result.setRecords(records);
        result.setTotal(searchHits.getTotalHits());
        result.setPage(page);
        result.setSize(size);

        return result;
    }


    // =============================
    // ⭐ 自动补全（支持中文模糊匹配）
    // =============================
    @Override
    public List<String> suggest(String prefix) {

        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }

        String trimmed = prefix.trim();

        try {
            // 方案：从ES中搜索标题和关键词中包含输入内容的文献，提取匹配的词语
            var response = elasticsearchClient.search(s -> s
                            .index("literature")
                            .size(20)
                            .query(q -> q.bool(b -> b
                                    .should(sh -> sh.matchPhrasePrefix(mp -> mp.field("title").query(trimmed)))
                                    .should(sh -> sh.matchPhrasePrefix(mp -> mp.field("keywords").query(trimmed)))
                                    .should(sh -> sh.match(m -> m.field("title").query(trimmed)))
                                    .should(sh -> sh.match(m -> m.field("keywords").query(trimmed)))
                                    .minimumShouldMatch("1")
                            ))
                            .source(sc -> sc.filter(f -> f.includes("title", "keywords"))),
                    LiteratureDocument.class
            );

            List<String> result = new ArrayList<>();

            response.hits().hits().forEach(hit -> {
                LiteratureDocument doc = hit.source();
                if (doc == null) return;

                // 从标题中提取：如果标题包含输入的前缀，加入建议
                if (doc.getTitle() != null && doc.getTitle().contains(trimmed)) {
                    result.add(doc.getTitle());
                }

                // 从关键词中提取：逐个关键词匹配
                if (doc.getKeywords() != null) {
                    for (String kw : doc.getKeywords().split("[,，;；、\\s]+")) {
                        String keyword = kw.trim();
                        if (!keyword.isEmpty() && keyword.contains(trimmed)) {
                            result.add(keyword);
                        }
                    }
                }
            });

            // 去重，优先展示关键词（短的排前面），限制5条
            return result.stream()
                    .distinct()
                    .sorted((a, b) -> a.length() - b.length())
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("suggest error", e);
            return List.of();
        }
    }




    // =============================
    // 同步 ES
    // =============================
    @Override
    public void syncAllToEs() {

        List<Literature> list = literatureMapper.selectList(
                new LambdaQueryWrapper<Literature>()
                        .eq(Literature::getStatus, 1)
        );

        if (list == null || list.isEmpty()) {
            log.warn("⚠ 没有数据同步到 ES");
            return;
        }

        List<LiteratureDocument> docs = list.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

        literatureEsRepository.saveAll(docs);

        log.info("✅ 已同步 {} 条数据到 ES", docs.size());
    }

    @Override
    public void syncToEs(Long literatureId) {
        Literature literature = literatureMapper.selectById(literatureId);

        if (literature != null && literature.getStatus() == 1) {
            literatureEsRepository.save(convertToDocument(literature));
        }
    }

    @Override
    public void deleteFromEs(Long literatureId) {
        if (literatureEsRepository.existsById(literatureId)) {
            literatureEsRepository.deleteById(literatureId);
        }
    }

    @Override
    public void batchDeleteFromEs(List<Long> literatureIds) {
        if (literatureIds == null || literatureIds.isEmpty()) return;
        literatureEsRepository.deleteAllById(literatureIds);
    }

    // =============================
    // 转换
    // =============================
    private LiteratureDocument convertToDocument(Literature literature) {
        LiteratureDocument doc = new LiteratureDocument();

        doc.setId(literature.getId());
        doc.setTitle(literature.getTitle());
        doc.setAuthors(literature.getAuthors());
        doc.setAbstractText(literature.getAbstractText());
        doc.setPublishYear(literature.getPublishYear());
        doc.setJournal(literature.getJournal());
        doc.setKeywords(literature.getKeywords());
        doc.setDoi(literature.getDoi());
        doc.setCategory(literature.getCategory());
        doc.setViewCount(literature.getViewCount());
        doc.setFileUrl(literature.getFileUrl());

        // ⭐ 修改：使用String[]而不是List<String>
        String[] inputs = buildSuggestInputs(literature);
        org.springframework.data.elasticsearch.core.suggest.Completion completion =
                new org.springframework.data.elasticsearch.core.suggest.Completion(inputs);

        doc.setSuggest(completion);

        return doc;
    }

    private String[] buildSuggestInputs(Literature literature) {
        List<String> list = new ArrayList<>();

        if (literature.getTitle() != null) {
            list.add(literature.getTitle());
        }

        if (literature.getKeywords() != null) {
            for (String k : literature.getKeywords().split(",")) {
                String t = k.trim();
                if (!t.isEmpty() && !list.contains(t)) {
                    list.add(t);
                }
            }
        }

        // ⭐ 返回String[]而不是List<String>
        return list.toArray(new String[0]);
    }

    // =============================
    // ⭐ 高亮核心方法
    // =============================
    private String highlight(String text, String keyword) {
        if (text == null || keyword == null || keyword.trim().isEmpty()) {
            return text;
        }

        try {
            String regex = "(?i)" + java.util.regex.Pattern.quote(keyword);
            return java.util.regex.Pattern.compile(regex)
                    .matcher(text)
                    .replaceAll(match ->
                            "<span style='color:red;font-weight:bold'>" + match.group() + "</span>"
                    );
        } catch (Exception e) {
            return text;
        }
    }

}
