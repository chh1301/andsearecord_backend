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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测评类应用评分策略
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AppService appService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        // 1.根据id查询题目和题目结果信息
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, app.getId()));

        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId));
        // 2.统计用户每个选型对应的属性个数，如I:10个，E:5个
        Map<String, Integer> optionCount = new HashMap<>();
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

                        if (!optionCount.containsKey(result)) {
                            optionCount.put(result, 0);
                        }

                        optionCount.put(result, optionCount.get(result) + 1);
                    }
                }
            }
        }

        // 3. 遍历每种评分结果，计算那个结果的得分最高
        // 初始化最高分和最高分数对应的评分结果
        int maxScore = 0;
        ScoringResult maxScoreResult = scoringResultList.get(0);
        // 遍历评分结果链表
        for (ScoringResult scoringResult : scoringResultList) {
            List<String> resultProp = JSONUtil.toList(scoringResult.getResultProp(), String.class);
            //计算当前评分结果
            int score = resultProp.stream().mapToInt(prop -> optionCount.getOrDefault(prop, 0)).sum();

            if (score > maxScore) {
                maxScore = score;
                maxScoreResult = scoringResult;
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
        return userAnswer;

    }
}

