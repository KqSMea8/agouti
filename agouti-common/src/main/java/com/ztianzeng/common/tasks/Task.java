/*
 * Copyright 2018-2019 zTianzeng Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ztianzeng.common.tasks;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 具体任务
 *
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-01-28 17:39
 */
@NoArgsConstructor
@Data
public class Task {
    /**
     * 调用类型
     */
    private TaskType taskType;
    /**
     * 调用的目标
     */
    private String target;

    private String alias;

    private String method;

    /**
     * 入参情况，多个入参通过key:value的结构书写
     */
    private Map<String, Object> originInputs;

    private Map<String, Object> inputs;

    private Map<String, Object> inputData = new HashMap<>();

    private Status status;

    private Map<String, Object> outputData = new HashMap<>();


    /**
     * runtime fail reason
     */
    private String reasonForFail;

    public enum Status {
        FAILED, COMPLETED;
    }

    public enum TaskType {
        /**
         * FEIGN 调用
         */
        METHOD,
        /**
         * URL调用
         */
        URL;
    }
}