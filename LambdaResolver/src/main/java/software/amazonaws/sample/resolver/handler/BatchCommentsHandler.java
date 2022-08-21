//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.resolver.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazonaws.sample.resolver.service.BatchCommentsService;
import software.amazonaws.sample.resolver.util.DBInitializer;
import software.amazonaws.sample.resolver.util.JsonConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for requests to Lambda function.
 */
public class BatchCommentsHandler implements RequestHandler<List<Map<String, Object>>, List<String>> {

    private static final Logger logger = LogManager.getLogger(BatchCommentsHandler.class);
    private final JsonConverter jsonConverter;
    private final DBInitializer dbInitializer;
    private BatchCommentsService batchCommentsService;

    public BatchCommentsHandler() {
        this.jsonConverter = new JsonConverter();
        this.dbInitializer = new DBInitializer();
        batchCommentsService = new BatchCommentsService(dbInitializer.getConnection(), jsonConverter);
    }

    public List<String> handleRequest(final List<Map<String, Object>> inputs, final Context context) {

        logger.info("Event :: " + jsonConverter.toJson(inputs));
        List<Map<String, String>> sources = inputs.stream()
                .filter(input -> input.get("field").equals("commentsByPost"))
                .map(input -> (Map<String, String>) input.get("source"))
                .collect(Collectors.toList());

        try {
            return batchCommentsService.getComments(sources);
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }


}
