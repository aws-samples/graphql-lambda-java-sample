//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.example.handler;

import com.amazon.rdsdata.client.RdsData;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;
import software.amazon.awssdk.services.rdsdata.model.BadRequestException;

import java.util.Arrays;
import java.util.List;


public class DBInitLambdaHandler implements RequestHandler<CloudFormationCustomResourceEvent, String> {

    private static final Logger logger = LogManager.getLogger(DBInitLambdaHandler.class);
    private static final int MAX_RETRIES = 2;

    public DBInitLambdaHandler() {
    }


    @Override
    public String handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        String requestType = event.getRequestType();

        logger.info("Request Type :: " + event.getRequestType());
        logger.info("Properties :: " + event.getResourceProperties());

        String sqlScript = event.getResourceProperties().get("SqlScript").toString();

        executeSQLStatements(getSQLStatements(sqlScript));

        JsonObject retJson = new JsonObject();
        if (requestType != null) {
            retJson.addProperty("RequestType", requestType);


        }

        if ("Update".equals(requestType) || "Delete".equals(requestType)) {
            retJson.addProperty("PhysicalResourceId", event.getPhysicalResourceId());
        }

        retJson.addProperty("scriptRun", Boolean.TRUE.toString());
        logger.info("RETURN :: " + retJson.toString());
        return retJson.toString();
    }

    private List<String> getSQLStatements(String sqlScript) {
        return Arrays.asList(sqlScript.split(";"));
    }

    private void executeSQLStatements(List<String> stmts) {
        for (String sqlStmt : stmts) {
            String secretArn = System.getenv("RDS_SECRET");
            String clusterArn = System.getenv("CLUSTER_ARN");
            String databaseName = System.getenv("DATABASE_NAME");

            RdsData client = RdsData.builder()
                    .sdkClient(RdsDataClient.builder().build())
                    .database(databaseName)
                    .resourceArn(clusterArn)
                    .secretArn(secretArn)
                    .build();

            int i = 0;
            while (i < MAX_RETRIES ) {
                try {
                    client.forSql(sqlStmt).withContinueAfterTimeout().execute();
                    logger.info("Finished running SQL :: " + sqlStmt);
                    i = MAX_RETRIES;
                } catch (BadRequestException be) {
                    //Serverless Aurora Cluster is paused. Needs a few seconds to wake up. Wait for 30 secs
                    i++;
                    logger.info("AURORA serverless Cluster is paused. Retry in 30 secs");
                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                } catch (Exception e) {
                    i = MAX_RETRIES;
                    logger.error(e);
                    throw e;
                }

            }
        }

    }


}
