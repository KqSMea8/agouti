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
 *
 */
package com.ztianzeng.agouti.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ztianzeng.agouti.core.AgoutiException;
import com.ztianzeng.agouti.core.WorkFlow;
import com.ztianzeng.agouti.core.WorkFlowTask;
import com.ztianzeng.agouti.core.executor.http.HttpMethod;
import com.ztianzeng.common.tasks.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-04-15 11:51
 */
@Slf4j
public class HttpTask extends WorkFlowTask {
    public static final String REQUEST_PARAMETER_NAME = "http_request";

    static final String MISSING_REQUEST = "Missing HTTP request. Task input MUST have a '" + REQUEST_PARAMETER_NAME + "' key with HttpTask.Input as value. See documentation for HttpTask for required input parameters";

    protected ObjectMapper om = objectMapper();
    private TypeReference<Map<String, Object>> mapOfObj = new TypeReference<Map<String, Object>>() {
    };

    private TypeReference<List<Object>> listOfObj = new TypeReference<List<Object>>() {
    };

    @Override
    public void start(WorkFlow workflow, Task task) throws JsonProcessingException {
        // request info
        Object request = task.getInputData().get(REQUEST_PARAMETER_NAME);

        if (request == null) {
            task.setReasonForFail(MISSING_REQUEST);
            task.setStatus(Task.Status.FAILED);
            return;
        }

        Input input = om.convertValue(request, Input.class);
        if (input.getUri() == null) {
            String reason = "Missing HTTP URI.  See documentation for HttpTask for required input parameters";
            task.setReasonForFail(reason);
            task.setStatus(Task.Status.FAILED);
            return;
        }

        if (input.getMethod() == null) {
            String reason = "No HTTP method specified";
            task.setReasonForFail(reason);
            task.setStatus(Task.Status.FAILED);
            return;
        }

        HttpResponseWrapper response = httpCall(input);

        if (response.status > 199 && response.status < 300) {
            task.setStatus(Task.Status.COMPLETED);
            if (response.body != null) {
                task.setReasonForFail(response.body.toString());
            } else {
                task.setReasonForFail("No response from the remote service");
            }
        } else {
            task.setStatus(Task.Status.FAILED);
        }


        task.getOutputData().put("response", response.asMap());


    }


    /**
     * request implementation
     *
     * @param input input param
     * @return okhttp Response wrap to inner response
     */
    private HttpResponseWrapper httpCall(Input input) throws JsonProcessingException {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        RequestBody requestBody = RequestBody.create(
                MediaType.parse(input.accept),
                om.writeValueAsString(input.body)
        );
        input.headers.forEach(builder::header);
        builder.url(input.uri).method(input.method.name(), requestBody);
        Response response;

        try {
            response = client.newCall(builder.build()).execute();
            HttpResponseWrapper responseWrapper = new HttpResponseWrapper();
            responseWrapper.status = response.code();
            responseWrapper.body = extractBody(response.body());
            responseWrapper.headers = response.headers().toMultimap();

            return responseWrapper;

        } catch (IOException e) {
            throw new AgoutiException(e);
        }
    }

    private Object extractBody(ResponseBody responseBody) {
        if (responseBody == null) {
            return null;
        }
        String json = null;
        try {
            json = responseBody.string();
            log.info(json);
            JsonNode node = om.readTree(json);
            if (node == null) {
                return null;
            }
            if (node.isArray()) {
                return om.convertValue(node, listOfObj);
            } else if (node.isObject()) {
                return om.convertValue(node, mapOfObj);
            } else if (node.isNumber()) {
                return om.convertValue(node, Double.class);
            } else {
                return node.asText();
            }

        } catch (IOException jpe) {
            log.error(jpe.getMessage(), jpe);
            return json;
        }
    }

    @Data
    public static class Input {

        private HttpMethod method;

        private String uri;

        private Object body;

        private String accept = "application/json";

        private String contentType = "application/json";

        private Map<String, String> headers = new HashMap<>();
    }


    /**
     * http response
     */
    public static class HttpResponseWrapper {

        private int status;

        private Map<String, List<String>> headers;

        private Object body;

        public Map<String, Object> asMap() {

            Map<String, Object> map = new HashMap<>();
            map.put("body", body);
            map.put("headers", headers);
            map.put("status", status);

            return map;
        }

    }


    private static ObjectMapper objectMapper() {
        final ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return om;
    }
}