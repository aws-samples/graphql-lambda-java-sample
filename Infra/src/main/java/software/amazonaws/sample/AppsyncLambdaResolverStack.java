//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.appsync.alpha.*;
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

public class AppsyncLambdaResolverStack extends Stack {

    private static String DB_USER_NAME = "appsyncadmin";
    private static String DB_NAME = "AppSyncLambdaDB";
    private static String DB_PORT = "3306";

    public AppsyncLambdaResolverStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AppsyncLambdaResolverStack(final Construct scope, final String id, final AppsyncLambdaResolverStackProps props) {
        super(scope, id, props);

        CommonStackUtil commonStackUtil = CommonStackUtil.builder()
                .logicalNamePrefix("AppsyncLambdaResolver")
                .physicalNamePrefix("appsync-lambda-resolver")
                .dbUserName(DB_USER_NAME)
                .vpc(props.getVpc())
                .account(this.getAccount())
                .region(this.getRegion())
                .dbPort(DB_PORT)
                .dbName(DB_NAME)
                .scope(this)
                .build();

        commonStackUtil.setupDBandProxy();

        Function getPostFunction = getFunction(props, commonStackUtil, "GetPostResolverFunction", "GetPostByIdLambdaHandler");
        Function getPostByAuthorFunction = getFunction(props, commonStackUtil, "GetPostByAuthorResolverFunction", "GetPostByAuthorLambdaHandler");
        Function getCommentsOnPostFunction = getFunction(props, commonStackUtil, "GetCommentsOnPostResolverFunction", "GetCommentsOnPostLambdaHandler");
        Function getCommentsByAuthorFunction = getFunction(props, commonStackUtil, "GetCommentsByAuthorResolverFunction", "GetCommentsByAuthorLambdaHandler");
        Function getNumberOfCommentsFunction = getFunction(props, commonStackUtil, "GetNumberOfCommentsFunction", "GetNumberOfCommentsLambdaHandler");
        Function createCommentFunction = getFunction(props, commonStackUtil, "CreateCommentFunction", "CreateCommentLambdaHandler");
        Function upVoteCommentFunction = getFunction(props, commonStackUtil, "UpVoteCommentFunction", "UpVoteCommentLambdaHandler");
        Function downVoteCommentFunction = getFunction(props, commonStackUtil, "DownVoteCommentFunction", "DownVoteCommentLambdaHandler");
        Function addPostFunction = getFunction(props, commonStackUtil, "AddPostFunction", "AddPostLambdaHandler");
        Function incrementViewsFunction = getFunction(props, commonStackUtil, "IncrementViewsFunction", "IncrementViewsLambdaHandler");

        Function batchResolver = getFunction(props, commonStackUtil, "AppSyncBatchResolverFunction", "BatchCommentsHandler");

        GraphqlApi api = new GraphqlApi(this, "AppSyncLambdaResolver", GraphqlApiProps.builder()
                .name("AppsyncLambdaResolverAPI")
                .schema(Schema.fromAsset("src/main/resources/schema.graphql"))
                .authorizationConfig(AuthorizationConfig.builder()
                        .defaultAuthorization(AuthorizationMode.builder()
                                .authorizationType(AuthorizationType.API_KEY)
                                .apiKeyConfig(ApiKeyConfig.builder()
                                        .expires(Expiration.after(Duration.days(365)))
                                        .build())
                                .build())
                        .build())
                .build());

        LambdaDataSource getPostDS = api.addLambdaDataSource("GetPostDataSource", getPostFunction);
        LambdaDataSource getPostByAuthorDS = api.addLambdaDataSource("GetPostByAuthorDataSource", getPostByAuthorFunction);
        LambdaDataSource getCommentsOnPostDS = api.addLambdaDataSource("GetCommentsOnPostDataSource", getCommentsOnPostFunction);
        LambdaDataSource getCommentsByAuthorDS = api.addLambdaDataSource("GetCommentsByAuthorDataSource", getCommentsByAuthorFunction);
        LambdaDataSource getNumberOfCommentsDS = api.addLambdaDataSource("GetNumberOfCommentsDataSource", getNumberOfCommentsFunction);
        LambdaDataSource createCommentDS = api.addLambdaDataSource("CreateCommentDataSource", createCommentFunction);
        LambdaDataSource upVoteCommentDS = api.addLambdaDataSource("UpVoteCommentDataSource", upVoteCommentFunction);
        LambdaDataSource downVoteCommentDS = api.addLambdaDataSource("DownVoteCommentDataSource", downVoteCommentFunction);
        LambdaDataSource addPostDS = api.addLambdaDataSource("AddPostDataSource", addPostFunction);
        LambdaDataSource incrementViewsDS = api.addLambdaDataSource("IncrementViewsDataSource", incrementViewsFunction);
        LambdaDataSource bds = api.addLambdaDataSource("BatchDataSource", batchResolver);

        getPostDS.createResolver(BaseResolverProps.builder()
                .fieldName("getPost")
                .typeName("Query")
                .build());

        getPostByAuthorDS.createResolver(BaseResolverProps.builder()
                .fieldName("getPostsByAuthor")
                .typeName("Query")
                .build());

        getCommentsByAuthorDS.createResolver(BaseResolverProps.builder()
                .fieldName("getCommentsByAuthor")
                .typeName("Query")
                .build());

        getNumberOfCommentsDS.createResolver(BaseResolverProps.builder()
                .fieldName("getNumberOfCommentsOnPost")
                .typeName("Query")
                .build());

        getCommentsOnPostDS.createResolver(BaseResolverProps.builder()
                .fieldName("getCommentsOnPost")
                .typeName("Query")
                .build());

        createCommentDS.createResolver(BaseResolverProps.builder()
                .fieldName("createComment")
                .typeName("Mutation")
                .build());

        upVoteCommentDS.createResolver(BaseResolverProps.builder()
                .fieldName("upvoteComment")
                .typeName("Mutation")
                .build());

        downVoteCommentDS.createResolver(BaseResolverProps.builder()
                .fieldName("downvoteComment")
                .typeName("Mutation")
                .build());

        addPostDS.createResolver(BaseResolverProps.builder()
                .fieldName("createPost")
                .typeName("Mutation")
                .build());

        incrementViewsDS.createResolver(BaseResolverProps.builder()
                .fieldName("incrementViewCount")
                .typeName("Mutation")
                .build());

        bds.createResolver(BaseResolverProps.builder()
                .fieldName("comments")
                .typeName("Post")
                .requestMappingTemplate(MappingTemplate.fromString("{ \"version\" : \"2017-02-28\", \"operation\": \"BatchInvoke\", \"payload\": { \"field\": \"commentsByPost\", \"source\":  $utils.toJson($context.source) }}"))
                .responseMappingTemplate(MappingTemplate.fromString("$context.result"))
                .build());

        CfnOutput output = new CfnOutput(this, "LambdaResolverAppSyncAPIURL", CfnOutputProps.builder()
                .value(api.getGraphqlUrl())
                .build());

    }

    @NotNull
    private Function getFunction(AppsyncLambdaResolverStackProps props, CommonStackUtil commonStackUtil, String logicalName, String handlerClass) {
        return new Function(this, logicalName, FunctionProps.builder()
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
                .code(Code.fromAsset("../LambdaResolver/target/appsync-lambda-resolver-0.0.1-SNAPSHOT.jar"))
                .handler("software.amazonaws.sample.resolver.handler." + handlerClass + "::handleRequest")
                .timeout(Duration.seconds(20))
                .build());
    }

    @lombok.Builder
    @Setter
    @Getter
    public static class AppsyncLambdaResolverStackProps implements StackProps {
        private Vpc vpc;
        private Environment env;
    }

}
