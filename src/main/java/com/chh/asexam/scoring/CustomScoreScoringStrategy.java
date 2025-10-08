package com.chh.asexam.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chh.asexam.model.dto.question.QuestionContentDTO;
import com.chh.asexam.model.entity.App;
import com.chh.asexam.model.entity.Question;
import com.chh.asexam.model.entity.ScoringResult;
import com.chh.asexam.model.entity.UserAnswer;
import com.chh.asexam.model.vo.QuestionVO;
import com.chh.asexam.service.AppService;
import com.chh.asexam.service.QuestionService;
import com.chh.asexam.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/***
 * 打分类应用的评分策略
 */

@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AppService appService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        // 1.根据id查询题目和题目结果信息(根据分数从高到低来排)
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, app.getId()));

        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, app.getId()).orderByDesc(ScoringResult::getResultScoreRange));

        // 2.统计用户总得分
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        // 遍历题目链表
        for (QuestionContentDTO questionContentDTO : questionContent) {
            // 遍历答案链表
            for (String answer : choices) {
                // 遍历题目中的选项
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    if (option.getKey().equals(answer)) {
                        String result = option.getResult();
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }
        // 3.遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果

        ScoringResult maxScoreResult = scoringResultList.get(0);
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoreResult = scoringResult;
                break;
            }
        }
        // 4.构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoreResult.getId());
        userAnswer.setResultName(maxScoreResult.getResultName());
        userAnswer.setResultDesc(maxScoreResult.getResultDesc());
        userAnswer.setResultPicture(maxScoreResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}

