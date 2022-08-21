//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.resolver.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazonaws.sample.resolver.entity.Post;
import software.amazonaws.sample.resolver.service.QueryService;
import software.amazonaws.sample.resolver.util.DBInitializer;
import software.amazonaws.sample.resolver.util.JsonConverter;

import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class IncrementViewsLambdaHandler implements RequestHandler<Map<String, Object>, Post> {

    private static final Logger logger = LogManager.getLogger(IncrementViewsLambdaHandler.class);
    private final QueryService queryService;
    private final JsonConverter jsonConverter;
    private final DBInitializer dbInitializer;

    public IncrementViewsLambdaHandler() {
        this.jsonConverter = new JsonConverter();
        this.dbInitializer = new DBInitializer();
        this.queryService = new QueryService(dbInitializer.getConnection(), jsonConverter);
    }

    public Post handleRequest(final Map<String, Object> input, final Context context) {

        logger.info("Event :: " + jsonConverter.toJson(input));
        Map<String, String> arguments = (Map<String, String>) input.get("arguments");

        try {
            return queryService.incrementViewCount(arguments);
        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }


}
