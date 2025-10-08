package com.chh.asexam.scoring;

import com.chh.asexam.model.entity.App;
import com.chh.asexam.model.entity.UserAnswer;

import java.util.List;

public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}
