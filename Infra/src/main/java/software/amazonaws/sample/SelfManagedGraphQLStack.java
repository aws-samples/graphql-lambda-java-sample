//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigatewayv2.alpha.*;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class SelfManagedGraphQLStack extends Stack {

    public SelfManagedGraphQLStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SelfManagedGraphQLStack(final Construct scope, final String id, final SelfManagedGraphQLStackProps props) {
        super(scope, id, props);

        PropertyLoader propertyLoader = new PropertyLoader();
        Properties appConfig = propertyLoader.loadProperties("app.properties");

        String DB_USER_NAME = appConfig.getProperty("graphql.db.username");
        String DB_NAME = appConfig.getProperty("graphql.db.name");
        String DB_PORT = appConfig.getProperty("graphql.db.port");

        CommonStackUtil commonStackUtil = CommonStackUtil.builder()
                .logicalNamePrefix("SelfManagedGraphQL")
                .physicalNamePrefix("self-managed-graphql")
                .dbUserName(DB_USER_NAME)
                .vpc(props.getVpc())
                .account(this.getAccount())
                .region(this.getRegion())
                .dbPort(DB_PORT)
                .dbName(DB_NAME)
                .scope(this)
                .build();

        commonStackUtil.setupDBandProxy();

        Function lamdaFunction = new Function(this, "SelfManagedGraphQLLambdaFunction", FunctionProps.builder()
                .role(commonStackUtil.getLambdaRole())
                .securityGroups(Arrays.asList(commonStackUtil.getLambdaSG()))
                .memorySize(1024)
                .timeout(Duration.seconds(20))
                .vpc(props.getVpc())
                .runtime(Runtime.JAVA_11)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_NAT)
                        .build())
                .environment(Map.of("END_POINT", commonStackUtil.getDatabaseProxy().getEndpoint(),
                        "DATABASE_NAME", DB_NAME,
                        "DB_USER_NAME", DB_USER_NAME,
                        "DB_PORT", DB_PORT,
                        "REGION", this.getRegion()))
                .code(Code.fromAsset("../GraphqlLambda/target/graphql-lambda-0.0.1-SNAPSHOT.jar"))
                .handler("software.amazonaws.sample.graphqllambda.GraphqlLambdaHandler::handleRequest")
                .timeout(Duration.seconds(20))
                .build());


        HttpApi httpApi = new HttpApi(this, "SelfManagedGraphQLApi", HttpApiProps.builder()
                .description("Http API for Self Managed Graph QL API")
                .corsPreflight(CorsPreflightOptions.builder()
                        .allowHeaders(Arrays.asList("Content-Type"))
                        .allowMethods(Arrays.asList(CorsHttpMethod.OPTIONS, CorsHttpMethod.POST))
                        .allowOrigins(Arrays.asList("*"))
                        .build())
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/graphql")
                .methods(Arrays.asList(HttpMethod.POST))
                .integration(HttpLambdaIntegration.Builder.create("SelfManagedGraphQLHttpApi", lamdaFunction).build())
                .build());

        CfnOutput output = new CfnOutput(this, "SelfManagedGraphQLAPIURL", CfnOutputProps.builder()
                .value(httpApi.getUrl() + "graphql")
                .build());

    }

    @lombok.Builder
    @Setter
    @Getter
    public static class SelfManagedGraphQLStackProps implements StackProps {
        private Vpc vpc;
        private Environment env;
    }

}
