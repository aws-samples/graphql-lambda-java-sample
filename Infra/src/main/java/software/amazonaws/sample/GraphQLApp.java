//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class GraphQLApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment environment = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        VPCStack vpcStack = new VPCStack(app, "graphql-vpc-stack", StackProps.builder()
                .env(environment)
                .build());

        new SelfManagedGraphQLStack(app, "graphql-lambda-self-managed-stack", SelfManagedGraphQLStack.SelfManagedGraphQLStackProps.builder()
                .env(environment)
                .vpc(vpcStack.getVpc())
                .build());

        new AppsyncLambdaResolverStack(app, "appsync-lambda-resolver-stack", AppsyncLambdaResolverStack.AppsyncLambdaResolverStackProps.builder()
                .env(environment)
                .vpc(vpcStack.getVpc())
                .build());

        new AppsyncAuroraStack(app, "appsync-aurora-stack", AppsyncAuroraStack.AppsyncAuroraStackProps.builder()
                .env(environment)
                .vpc(vpcStack.getVpc())
                .build());

        app.synth();
    }
}

