package com.example.literaturesearchsystem.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfParserUtil {

    @Autowired
    private CategoryClassifier categoryClassifier;

    // 需要过滤的英文关键词
    private static final String[] FILTER_WORDS = {
            "Abstract", "Key words", "Keywords", "DOI", "ISSN", "CN",
            "Received", "Accepted", "Published", "网络首发", "录用定稿"
    };

    public Map<String, String> parsePdf(MultipartFile file) {
        Map<String, String> result = new HashMap<>();

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            // 提取正文（前5页）
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            int totalPages = document.getNumberOfPages();
            stripper.setEndPage(Math.min(5, totalPages));
            String text = stripper.getText(document);

            // 清理文本：合并换行，去除多余空格
            String cleanText = text.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ");

            // 提取标题（只取中文部分）
            String title = extractChineseTitle(cleanText);

            // 提取作者
            String author = extractChineseAuthor(cleanText);

            // 提取年份
            String year = extractYear(cleanText);

            // 提取摘要（只取中文）
            String abstractText = extractChineseAbstract(cleanText);

            // 提取 DOI
            String doi = extractDoi(cleanText);

            // 提取关键词（只取中文）
            String keywords = extractChineseKeywords(cleanText);

            // 提取期刊名称
            String journal = extractChineseJournal(cleanText);

            //自动分类
            String category = categoryClassifier.classify(title, keywords, abstractText);
            result.put("category", category);

            result.put("title", title != null ? title : "");
            result.put("author", author != null ? author : "");
            result.put("year", year != null ? year : "");
            result.put("abstractText", abstractText != null ? abstractText : "");
            result.put("doi", doi != null ? doi : "");
            result.put("keywords", keywords != null ? keywords : "");
            result.put("journal", journal != null ? journal : "");

            log.info("PDF解析成功: 标题={}, 作者={}, 年份={}, 期刊={}", title, author, year, journal);

        } catch (Exception e) {
            log.error("PDF解析失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 提取中文标题 - 排除英文和冗余内容
     */
    private String extractChineseTitle(String text) {
        if (text == null || text.isEmpty()) return null;

        // 匹配 "题目：" 后面的内容
        Pattern titlePattern = Pattern.compile("题目[：:]\\s*([^。\\n]{10,100})");
        Matcher matcher = titlePattern.matcher(text);
        if (matcher.find()) {
            String title = matcher.group(1).trim();
            // 清理 DOI、引用格式等
            title = title.replaceAll("DOI.*$", "").trim();
            title = title.replaceAll("引用格式.*$", "").trim();
            title = title.replaceAll("收稿日期.*$", "").trim();
            title = title.replaceAll("网络首发日期.*$", "").trim();
            if (title.length() > 5 && title.length() < 150) {
                return title;
            }
        }

        // 匹配第一行较长的中文（10-80个字符）
        Pattern titlePattern2 = Pattern.compile("([\\u4e00-\\u9fa5]{10,80}[研究分析基于方法评价模型应用设计实现]{2,5})");
        Matcher matcher2 = titlePattern2.matcher(text);
        if (matcher2.find()) {
            String title = matcher2.group(1).trim();
            if (title.length() > 10 && title.length() < 150) {
                return title;
            }
        }

        // 取第一段中较长的行作为标题
        String firstPart = text.length() > 500 ? text.substring(0, 500) : text;
        String[] sentences = firstPart.split("[。\\n]");
        for (String sentence : sentences) {
            sentence = sentence.trim();
            // 只保留中文，过滤掉英文和包含特殊字符的行
            if (sentence.length() > 15 && sentence.length() < 150 &&
                    !sentence.matches(".*[a-zA-Z]{5,}.*") &&
                    !sentence.contains("DOI") && !sentence.contains("ISSN") &&
                    !sentence.contains("Abstract") && !sentence.contains("收稿日期")) {
                return sentence;
            }
        }

        return null;
    }

    /**
     * 提取中文作者 - 只取中文姓名
     */
    private String extractChineseAuthor(String text) {
        if (text == null || text.isEmpty()) return null;

        // 匹配 "作者：" 后面的中文姓名
        Pattern pattern = Pattern.compile("作者[：:]\\s*([\\u4e00-\\u9fa5]{2,4}(?:[，,、][\\u4e00-\\u9fa5]{2,4}){0,5})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String author = matcher.group(1).trim();
            // 清理掉多余内容
            author = author.replaceAll("收稿日期.*$", "").trim();
            author = author.replaceAll("\\d+.*$", "").trim();
            if (author.length() > 0 && author.length() < 50) {
                return author;
            }
        }

        return null;
    }

    /**
     * 提取年份
     */
    private String extractYear(String text) {
        if (text == null || text.isEmpty()) return null;

        Pattern pattern = Pattern.compile("\\b(20[0-2][0-9]|19[0-9]{2})\\b");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 提取中文摘要 - 排除英文
     */
    private String extractChineseAbstract(String text) {
        if (text == null || text.isEmpty()) return null;

        // 匹配 "摘要：" 后面的中文内容
        Pattern pattern = Pattern.compile("摘要[：:]\\s*([\\u4e00-\\u9fa5\\，\\。\\；\\“\\”\\！\\？]{50,800})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String abstractText = matcher.group(1).trim();
            // 截取到句号结束
            int endIndex = abstractText.indexOf("。");
            if (endIndex > 50 && endIndex < 500) {
                abstractText = abstractText.substring(0, endIndex + 1);
            }
            if (abstractText.length() > 500) {
                abstractText = abstractText.substring(0, 500);
            }
            return abstractText;
        }

        return null;
    }

    /**
     * 提取 DOI
     */
    private String extractDoi(String text) {
        if (text == null || text.isEmpty()) return null;

        // 匹配 DOI: 10.xxxx/xxxxx
        Pattern pattern = Pattern.compile("DOI[：:]\\s*(10\\.\\d{4,5}/[^\\s]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String doi = matcher.group(1).trim();
            doi = doi.replaceAll("[。，,.;:、\\[\\]\\(\\)]$", "");
            return doi;
        }

        // 匹配纯 DOI 格式
        Pattern pattern2 = Pattern.compile("\\b(10\\.\\d{4,5}/[^\\s]{5,50})\\b");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            String doi = matcher2.group(1).trim();
            doi = doi.replaceAll("[。，,.;:、]$", "");
            return doi;
        }

        return null;
    }

    /**
     * 提取中文关键词 - 排除英文
     */
    private String extractChineseKeywords(String text) {
        if (text == null || text.isEmpty()) return null;

        // 匹配 "关键词：" 后面的中文内容
        Pattern pattern = Pattern.compile("关键词[：:]\\s*([\\u4e00-\\u9fa5\\；\\；\\，]{10,200})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String keywords = matcher.group(1).trim();
            // 清理掉英文
            keywords = keywords.replaceAll("[a-zA-Z\\s]+", "");
            keywords = keywords.replaceAll("DOI.*$", "").trim();
            keywords = keywords.replaceAll("[。，,.;:、]$", "");
            keywords = keywords.replaceAll("[；;]", ",");
            if (keywords.length() > 0 && keywords.length() < 200) {
                return keywords;
            }
        }

        // 匹配中英文混合关键词，只取中文部分
        Pattern pattern2 = Pattern.compile("关键词[：:]\\s*([^。\\n]{10,200})");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            String keywords = matcher2.group(1).trim();
            // 过滤掉英文部分（只保留中文和标点）
            keywords = keywords.replaceAll("[a-zA-Z\\s]+", "");
            keywords = keywords.replaceAll("DOI.*$", "").trim();
            keywords = keywords.replaceAll("[。，,.;:、]$", "");
            keywords = keywords.replaceAll("[；;]", ",");
            if (keywords.length() > 0 && keywords.length() < 200) {
                return keywords;
            }
        }

        return null;
    }

    /**
     * 提取中文期刊名称
     */
    private String extractChineseJournal(String text) {
        if (text == null || text.isEmpty()) return null;

        // 匹配期刊名称（常见格式：期刊名 + 空格 + ISSN）
        Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,15}?(?:学报|杂志|期刊|研究|地理|经济|管理|理论与实践))");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String journal = matcher.group(1).trim();
            if (journal.length() > 2 && journal.length() < 30) {
                return journal;
            }
        }

        return null;
    }

}