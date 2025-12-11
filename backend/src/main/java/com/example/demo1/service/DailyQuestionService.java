package com.example.demo1.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.exception.BusinessException;
import com.example.demo1.dto.response.DailyQuestionOptionVO;
import com.example.demo1.dto.response.DailyQuestionVO;
import com.example.demo1.entity.DailyQuestion;
import com.example.demo1.entity.DailyQuestionAnswer;
import com.example.demo1.mapper.DailyQuestionAnswerMapper;
import com.example.demo1.mapper.DailyQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyQuestionService {

    private final DailyQuestionMapper dailyQuestionMapper;
    private final DailyQuestionAnswerMapper answerMapper;

    public DailyQuestionVO getTodayQuestion(Long userId) {
        DailyQuestion question = ensureTodayQuestionExists();
        DailyQuestionAnswer existingAnswer = null;
        if (userId != null) {
            existingAnswer = answerMapper.selectOne(new LambdaQueryWrapper<DailyQuestionAnswer>()
                .eq(DailyQuestionAnswer::getQuestionId, question.getId())
                .eq(DailyQuestionAnswer::getUserId, userId));
        }
        return toVo(question, existingAnswer);
    }

    @Transactional
    public DailyQuestionVO answer(Long questionId, Long userId, int optionIndex) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        DailyQuestion question = dailyQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        if (optionIndex < 0 || optionIndex > 3) {
            throw new BusinessException("选项无效");
        }
        DailyQuestionAnswer existing = answerMapper.selectOne(new LambdaQueryWrapper<DailyQuestionAnswer>()
            .eq(DailyQuestionAnswer::getQuestionId, questionId)
            .eq(DailyQuestionAnswer::getUserId, userId));
        if (existing != null) {
            throw new BusinessException("今日已经参与过答题");
        }
        DailyQuestionAnswer answer = new DailyQuestionAnswer();
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setSelectedOption(optionIndex);
        answerMapper.insert(answer);

        incrementCounter(question, optionIndex);
        dailyQuestionMapper.updateById(question);
        return toVo(question, answer);
    }

    private void incrementCounter(DailyQuestion question, int optionIndex) {
        switch (optionIndex) {
            case 0 -> question.setCountA(defaultCount(question.getCountA()) + 1);
            case 1 -> question.setCountB(defaultCount(question.getCountB()) + 1);
            case 2 -> question.setCountC(defaultCount(question.getCountC()) + 1);
            case 3 -> question.setCountD(defaultCount(question.getCountD()) + 1);
            default -> throw new BusinessException("选项无效");
        }
    }

    private int defaultCount(Integer value) {
        return value == null ? 0 : value;
    }

    private DailyQuestion ensureTodayQuestionExists() {
        LocalDate today = LocalDate.now();
        DailyQuestion question = dailyQuestionMapper.selectOne(new LambdaQueryWrapper<DailyQuestion>()
            .eq(DailyQuestion::getQuestionDate, today));
        if (question != null) {
            return question;
        }
        DailyQuestion defaultQuestion = new DailyQuestion();
        defaultQuestion.setQuestionDate(today);
        defaultQuestion.setQuestion("今天你想探索哪一种饮品知识？");
        defaultQuestion.setOptionA("葡萄酒的酿造工艺");
        defaultQuestion.setOptionB("威士忌的熟成秘诀");
        defaultQuestion.setOptionC("精酿啤酒的风味");
        defaultQuestion.setOptionD("无酒精饮品的调配技巧");
        defaultQuestion.setCorrectOption(0);
        defaultQuestion.setCountA(0);
        defaultQuestion.setCountB(0);
        defaultQuestion.setCountC(0);
        defaultQuestion.setCountD(0);
        defaultQuestion.setExplanation("葡萄酒酿造涵盖葡萄采摘、发酵、陈酿等步骤，是理解饮品风味的基础。");
        defaultQuestion.setWikiLink("/wiki/classic-wine");
        dailyQuestionMapper.insert(defaultQuestion);
        return defaultQuestion;
    }

    private DailyQuestionVO toVo(DailyQuestion question, DailyQuestionAnswer answer) {
        int[] counts = {
            defaultCount(question.getCountA()),
            defaultCount(question.getCountB()),
            defaultCount(question.getCountC()),
            defaultCount(question.getCountD())
        };
        String[] labels = {"A", "B", "C", "D"};
        String[] texts = {
            question.getOptionA(),
            question.getOptionB(),
            question.getOptionC(),
            question.getOptionD()
        };
        int total = counts[0] + counts[1] + counts[2] + counts[3];
        List<DailyQuestionOptionVO> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double percentage = total == 0 ? 0 : (counts[i] * 100.0) / total;
            options.add(DailyQuestionOptionVO.builder()
                .index(i)
                .label(labels[i])
                .text(StringUtils.defaultIfBlank(texts[i], "选项" + labels[i]))
                .count(counts[i])
                .percentage(Math.round(percentage * 10.0) / 10.0)
                .build());
        }
        Integer selectedOption = answer != null ? answer.getSelectedOption() : null;
        return DailyQuestionVO.builder()
            .id(question.getId())
            .questionDate(question.getQuestionDate())
            .question(question.getQuestion())
            .options(options)
            .correctOption(question.getCorrectOption())
            .selectedOption(selectedOption)
            .answered(selectedOption != null)
            .explanation(question.getExplanation())
            .wikiLink(question.getWikiLink())
            .build();
    }
}
