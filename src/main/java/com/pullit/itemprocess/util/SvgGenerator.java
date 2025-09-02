package com.pullit.itemprocess.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.pullit.itemprocess.entity.ProcessItemHtmlData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SvgGenerator {
    
    private static final String SVG_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"1200\" viewBox=\"0 0 800 1200\">\n" +
            "<defs>\n" +
            "<style>\n" +
            ".title { font-family: Arial, sans-serif; font-size: 18px; font-weight: bold; fill: #333; }\n" +
            ".content { font-family: Arial, sans-serif; font-size: 14px; fill: #333; }\n" +
            ".passage { font-family: Arial, sans-serif; font-size: 12px; fill: #666; }\n" +
            ".question { font-family: Arial, sans-serif; font-size: 14px; fill: #000; font-weight: bold; }\n" +
            ".choice { font-family: Arial, sans-serif; font-size: 12px; fill: #333; }\n" +
            ".answer { font-family: Arial, sans-serif; font-size: 14px; fill: #0066cc; font-weight: bold; }\n" +
            ".explain { font-family: Arial, sans-serif; font-size: 12px; fill: #666; }\n" +
            "</style>\n" +
            "</defs>\n";
    
    private static final String SVG_FOOTER = "</svg>";
    
    /**
     * ProcessItemHtmlData를 SVG로 변환 (전체 문항)
     */
    public String generateFullSvg(ProcessItemHtmlData htmlData, Long itemId) {
        log.info("[SvgGenerator] 전체 SVG 생성 시작 - itemId: {}", itemId);
        
        StringBuilder svg = new StringBuilder();
        svg.append(SVG_HEADER);
        
        int yPosition = 50;
        
        // 제목
        svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"title\">문항 ID: %d</text>\n", yPosition, itemId));
        yPosition += 40;
        
        // 지문 (passage)
        if (htmlData.getPassage() != null && !htmlData.getPassage().trim().isEmpty()) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"passage\">[지문]</text>\n", yPosition));
            yPosition += 25;
            
            String passageText = cleanHtmlText(htmlData.getPassage());
            List<String> passageLines = wrapText(passageText, 70);
            for (String line : passageLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"passage\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 20;
            }
            yPosition += 20;
        }
        
        // 문제 (question)
        if (htmlData.getQuestion() != null && !htmlData.getQuestion().trim().isEmpty()) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"question\">[문제]</text>\n", yPosition));
            yPosition += 25;
            
            String questionText = cleanHtmlText(htmlData.getQuestion());
            List<String> questionLines = wrapText(questionText, 70);
            for (String line : questionLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"question\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 20;
            }
            yPosition += 20;
        }
        
        // 선택지 (choices)
        List<String> choices = extractChoices(htmlData);
        if (!choices.isEmpty()) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"content\">[선택지]</text>\n", yPosition));
            yPosition += 25;
            
            for (int i = 0; i < choices.size(); i++) {
                String choiceText = cleanHtmlText(choices.get(i));
                List<String> choiceLines = wrapText(choiceText, 65);
                for (String line : choiceLines) {
                    svg.append(String.format("<text x=\"40\" y=\"%d\" class=\"choice\">(%d) %s</text>\n", 
                            yPosition, i + 1, escapeXml(line)));
                    yPosition += 18;
                }
                yPosition += 5;
            }
            yPosition += 20;
        }
        
        // 답안 (answer)
        if (htmlData.getAnswer() != null && !htmlData.getAnswer().trim().isEmpty()) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"answer\">[답안]</text>\n", yPosition));
            yPosition += 25;
            
            String answerText = cleanHtmlText(htmlData.getAnswer());
            List<String> answerLines = wrapText(answerText, 70);
            for (String line : answerLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"answer\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 20;
            }
            yPosition += 20;
        }
        
        // 해설 (explanation)
        if (htmlData.getExplainText() != null && !htmlData.getExplainText().trim().isEmpty()) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"explain\">[해설]</text>\n", yPosition));
            yPosition += 25;
            
            String explainText = cleanHtmlText(htmlData.getExplainText());
            List<String> explainLines = wrapText(explainText, 70);
            for (String line : explainLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"explain\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 18;
            }
        }
        
        svg.append(SVG_FOOTER);
        
        log.info("[SvgGenerator] 전체 SVG 생성 완료 - itemId: {}, 크기: {} bytes", itemId, svg.length());
        return svg.toString();
    }
    
    /**
     * 지문만 SVG로 변환
     */
    public String generatePassageSvg(ProcessItemHtmlData htmlData, Long itemId) {
        log.info("[SvgGenerator] 지문 SVG 생성 시작 - itemId: {}", itemId);
        
        if (htmlData.getPassage() == null || htmlData.getPassage().trim().isEmpty()) {
            return generateEmptySvg("지문 없음", itemId);
        }
        
        StringBuilder svg = new StringBuilder();
        svg.append(SVG_HEADER);
        
        int yPosition = 50;
        
        // 제목
        svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"title\">지문 - 문항 ID: %d</text>\n", yPosition, itemId));
        yPosition += 40;
        
        // 지문 내용
        String passageText = cleanHtmlText(htmlData.getPassage());
        List<String> passageLines = wrapText(passageText, 70);
        for (String line : passageLines) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"passage\">%s</text>\n", yPosition, escapeXml(line)));
            yPosition += 20;
        }
        
        svg.append(SVG_FOOTER);
        
        log.info("[SvgGenerator] 지문 SVG 생성 완료 - itemId: {}, 크기: {} bytes", itemId, svg.length());
        return svg.toString();
    }
    
    /**
     * 문제만 SVG로 변환
     */
    public String generateQuestionSvg(ProcessItemHtmlData htmlData, Long itemId) {
        log.info("[SvgGenerator] 문제 SVG 생성 시작 - itemId: {}", itemId);
        
        StringBuilder svg = new StringBuilder();
        svg.append(SVG_HEADER);
        
        int yPosition = 50;
        
        // 제목
        svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"title\">문제 - 문항 ID: %d</text>\n", yPosition, itemId));
        yPosition += 40;
        
        // 문제 내용
        if (htmlData.getQuestion() != null && !htmlData.getQuestion().trim().isEmpty()) {
            String questionText = cleanHtmlText(htmlData.getQuestion());
            List<String> questionLines = wrapText(questionText, 70);
            for (String line : questionLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"question\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 20;
            }
            yPosition += 20;
        }
        
        // 선택지
        List<String> choices = extractChoices(htmlData);
        if (!choices.isEmpty()) {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"content\">[선택지]</text>\n", yPosition));
            yPosition += 25;
            
            for (int i = 0; i < choices.size(); i++) {
                String choiceText = cleanHtmlText(choices.get(i));
                List<String> choiceLines = wrapText(choiceText, 65);
                for (String line : choiceLines) {
                    svg.append(String.format("<text x=\"40\" y=\"%d\" class=\"choice\">(%d) %s</text>\n", 
                            yPosition, i + 1, escapeXml(line)));
                    yPosition += 18;
                }
                yPosition += 5;
            }
        }
        
        svg.append(SVG_FOOTER);
        
        log.info("[SvgGenerator] 문제 SVG 생성 완료 - itemId: {}, 크기: {} bytes", itemId, svg.length());
        return svg.toString();
    }
    
    /**
     * 답안만 SVG로 변환
     */
    public String generateAnswerSvg(ProcessItemHtmlData htmlData, Long itemId) {
        log.info("[SvgGenerator] 답안 SVG 생성 시작 - itemId: {}", itemId);
        
        StringBuilder svg = new StringBuilder();
        svg.append(SVG_HEADER);
        
        int yPosition = 50;
        
        // 제목
        svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"title\">답안 - 문항 ID: %d</text>\n", yPosition, itemId));
        yPosition += 40;
        
        // 답안 내용
        if (htmlData.getAnswer() != null && !htmlData.getAnswer().trim().isEmpty()) {
            String answerText = cleanHtmlText(htmlData.getAnswer());
            List<String> answerLines = wrapText(answerText, 70);
            for (String line : answerLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"answer\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 20;
            }
        } else {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"content\">답안 정보 없음</text>\n", yPosition));
        }
        
        svg.append(SVG_FOOTER);
        
        log.info("[SvgGenerator] 답안 SVG 생성 완료 - itemId: {}, 크기: {} bytes", itemId, svg.length());
        return svg.toString();
    }
    
    /**
     * 해설만 SVG로 변환
     */
    public String generateExplainSvg(ProcessItemHtmlData htmlData, Long itemId) {
        log.info("[SvgGenerator] 해설 SVG 생성 시작 - itemId: {}", itemId);
        
        StringBuilder svg = new StringBuilder();
        svg.append(SVG_HEADER);
        
        int yPosition = 50;
        
        // 제목
        svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"title\">해설 - 문항 ID: %d</text>\n", yPosition, itemId));
        yPosition += 40;
        
        // 해설 내용
        if (htmlData.getExplainText() != null && !htmlData.getExplainText().trim().isEmpty()) {
            String explainText = cleanHtmlText(htmlData.getExplainText());
            List<String> explainLines = wrapText(explainText, 70);
            for (String line : explainLines) {
                svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"explain\">%s</text>\n", yPosition, escapeXml(line)));
                yPosition += 18;
            }
        } else {
            svg.append(String.format("<text x=\"20\" y=\"%d\" class=\"content\">해설 정보 없음</text>\n", yPosition));
        }
        
        svg.append(SVG_FOOTER);
        
        log.info("[SvgGenerator] 해설 SVG 생성 완료 - itemId: {}, 크기: {} bytes", itemId, svg.length());
        return svg.toString();
    }
    
    /**
     * 빈 SVG 생성 (내용이 없을 때)
     */
    private String generateEmptySvg(String message, Long itemId) {
        StringBuilder svg = new StringBuilder();
        svg.append(SVG_HEADER);
        svg.append(String.format("<text x=\"20\" y=\"100\" class=\"content\">%s - 문항 ID: %d</text>\n", message, itemId));
        svg.append(SVG_FOOTER);
        return svg.toString();
    }
    
    /**
     * HTML 태그 제거 및 텍스트 정리
     */
    private String cleanHtmlText(String html) {
        if (html == null) return "";
        
        // HTML 태그 제거
        String text = html.replaceAll("<[^>]+>", "");
        
        // HTML 엔티티 디코딩
        text = text.replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&amp;", "&")
                  .replace("&quot;", "\"")
                  .replace("&#39;", "'")
                  .replace("&nbsp;", " ");
        
        // 연속된 공백 제거
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    /**
     * 텍스트를 지정된 길이로 줄바꿈
     */
    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return lines;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // 단어가 너무 길면 강제로 자름
                    lines.add(word.substring(0, Math.min(word.length(), maxLength)));
                    if (word.length() > maxLength) {
                        currentLine.append(word.substring(maxLength));
                    }
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * 선택지 추출
     */
    private List<String> extractChoices(ProcessItemHtmlData htmlData) {
        List<String> choices = new ArrayList<>();
        
        if (htmlData.getChoice1Html() != null && !htmlData.getChoice1Html().trim().isEmpty()) {
            choices.add(htmlData.getChoice1Html());
        }
        if (htmlData.getChoice2Html() != null && !htmlData.getChoice2Html().trim().isEmpty()) {
            choices.add(htmlData.getChoice2Html());
        }
        if (htmlData.getChoice3Html() != null && !htmlData.getChoice3Html().trim().isEmpty()) {
            choices.add(htmlData.getChoice3Html());
        }
        if (htmlData.getChoice4Html() != null && !htmlData.getChoice4Html().trim().isEmpty()) {
            choices.add(htmlData.getChoice4Html());
        }
        if (htmlData.getChoice5Html() != null && !htmlData.getChoice5Html().trim().isEmpty()) {
            choices.add(htmlData.getChoice5Html());
        }
        
        return choices;
    }
    
    /**
     * XML 특수문자 이스케이프
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
