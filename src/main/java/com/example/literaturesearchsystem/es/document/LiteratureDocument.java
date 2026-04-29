package com.example.literaturesearchsystem.es.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Data
@Document(indexName = "literature")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiteratureDocument {

    @Id
    private Long id;
    private String fileUrl;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String authors;

    @Field(type = FieldType.Text)
    private String abstractText;

    @Field(type = FieldType.Integer)
    private Integer publishYear;

    @Field(type = FieldType.Text)
    private String journal;

    @Field(type = FieldType.Text)
    private String keywords;

    @Field(type = FieldType.Text)
    private String doi;

    @Field(type = FieldType.Text)
    private String category;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    // ⭐ 使用Spring的Completion类
    @Field(type = FieldType.Text)  // 暂时改为Text，先编译通过
    private Completion suggest;
}
