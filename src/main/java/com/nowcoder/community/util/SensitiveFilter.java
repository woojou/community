package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            String keyword = "";
            while ((keyword = reader.readLine()) != null) {
                this.addKeyword(keyword);
            }

        } catch (IOException e) {
            logger.error("加载文件失败！：" + e.getMessage());
            throw new IllegalArgumentException(e);
        }
    }


    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向子节点,进入下一轮循环
            tempNode = subNode;

            // 设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    public String filter(String text) {
        if (StringUtils.isBlank(text)) return null;

        int begin = 0;
        int end = 0;
        TrieNode tempNode = rootNode;
        StringBuilder sb = new StringBuilder();

        // begin指向疑似敏感词的头
        while (begin < text.length()) {

            if (end >= text.length()) {
                // 证明 begin-end 这之间的词不是敏感词，可以把begin加入
                // 从下一个位置开始重新搜寻敏感词
                sb.append(text.charAt(begin++));
                end = begin;
                continue;
            }

            Character c = text.charAt(end);

            // 如果是符号而不是一个字符，应当略过
            if(isSymbol(c)) {
                // 如果是头节点，也就是begin=end
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                end++;
                continue;
            }

            TrieNode curNode = tempNode.getSubNode(c);
            if(curNode == null) {
                // 以begin开头的字符串不是敏感词，把begin加入，而不是一整个词
                sb.append(text.charAt(begin++));
                end = begin;
                // 进行下一轮搜寻
                tempNode = this.rootNode;
            } else if(!curNode.isKeywordEnd()) {
                // 疑似敏感词，继续向下搜寻
                tempNode = curNode;
                end++;
            } else {
                sb.append("***");
                begin = ++end;
                tempNode = this.rootNode;
            }
        }
        return sb.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    // 前缀树
    private class TrieNode {

        // 关键词结束标识
        private boolean isKeywordEnd = false;

        // 子节点(key是下级字符,value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }
}
