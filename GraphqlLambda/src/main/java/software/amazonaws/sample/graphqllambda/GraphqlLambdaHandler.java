//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.graphqllambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.common.net.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazonaws.sample.graphqllambda.util.DBInitializer;
import software.amazonaws.sample.graphqllambda.util.GraphQLUtil;
import software.amazonaws.sample.graphqllambda.util.JsonConverter;

import java.util.Map;


public class GraphqlLambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger logger = LogManager.getLogger(GraphqlLambdaHandler.class);
    static String CONTENT_TYPE_JSON = "application/json";
    static String CONTENT_TYPE_GRAPHQL = "application/graphql";
    static String CONTENT_TYPE = "content-type";
    private final JsonConverter jsonConverter;
    private final GraphQLUtil graphQLUtil;

    public GraphqlLambdaHandler() {
        this.jsonConverter = new JsonConverter();
        DBInitializer dbInitializer = new DBInitializer();
        this.graphQLUtil = new GraphQLUtil(jsonConverter, dbInitializer.getConnection());
    }


    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        logger.info("Event ::" + jsonConverter.toJson(event));
        String httpMethod = event.getRequestContext().getHttp().getMethod();
        if (httpMethod.equals("POST")) {
            try {
                return APIGatewayV2HTTPResponse.builder()
                        .withBody(handlePostRequest(event))
                        .withStatusCode(200)
                        .build();
            } catch (IllegalArgumentException e) {
                return APIGatewayV2HTTPResponse.builder()
                        .withBody(e.getMessage())
                        .withStatusCode(400)
                        .build();
            }
        } else {
            return APIGatewayV2HTTPResponse.builder()
                    .withBody("Operation not supported. Only POST is supported!")
                    .withStatusCode(501)
                    .build();
        }

    }

    private String handlePostRequest(APIGatewayV2HTTPEvent event) {

        if (event.getHeaders().get(CONTENT_TYPE).equals(CONTENT_TYPE_JSON)) {

            Map<String, Object> graphQLParams = jsonConverter.fromJson(event.getBody(), Map.class);
            Object query = graphQLParams.get("query");
            Object operationName = graphQLParams.get("operationName");
            Object variablesJson = graphQLParams.get("variables");

            String queryStr = "";
            if (query != null) {
                queryStr = query.toString();
            }
            return graphQLUtil.processGraphQlRequest(queryStr, operationName == null ? null : operationName.toString(), variablesJson == null ? null : variablesJson.toString());
        } else if (event.getHeaders().get(HttpHeaders.CONTENT_TYPE).equals(CONTENT_TYPE_GRAPHQL)) {
            return graphQLUtil.processGraphQlRequest(event.getBody(), null, null);
        } else {
            throw new IllegalArgumentException("Invalid or Missing Content-type header !!");
        }
    }


}
