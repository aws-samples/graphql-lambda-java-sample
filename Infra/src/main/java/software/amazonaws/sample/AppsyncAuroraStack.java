//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awscdk.*;
import software.amazon.awscdk.customresources.Provider;
import software.amazon.awscdk.customresources.ProviderProps;
import software.amazon.awscdk.services.appsync.alpha.*;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseClusterEngine;
import software.amazon.awscdk.services.rds.ServerlessCluster;
import software.amazon.awscdk.services.rds.ServerlessClusterProps;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretProps;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class AppsyncAuroraStack extends Stack {

    public AppsyncAuroraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AppsyncAuroraStack(final Construct scope, final String id, final AppsyncAuroraStackProps props) {
        super(scope, id, props);

        PropertyLoader propertyLoader = new PropertyLoader();
        Properties appConfig = propertyLoader.loadProperties("app.properties");

        String DB_USER_NAME = appConfig.getProperty("appsync.db.username");
        String DB_NAME = appConfig.getProperty("appsync.db.name");

        Secret rdsSecret = new Secret(this, "GraphQLAuroraRDSPassword", SecretProps.builder()
                .secretName("graphql-aurora-rds-cred")
                .generateSecretString(SecretStringGenerator.builder()
                        .excludePunctuation(true)
                        .passwordLength(16)
                        .generateStringKey("password")
                        .secretStringTemplate("{\"username\": \"" + DB_USER_NAME + "\"}")
                        .build())
                .build());

        ServerlessCluster cluster = new ServerlessCluster(this, "GraphQLAuroraCluster", ServerlessClusterProps.builder()
                .engine(DatabaseClusterEngine.AURORA_MYSQL)
                .vpc(props.getVpc())
                .enableDataApi(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_NAT)
                        .build())
                .defaultDatabaseName(DB_NAME)
                .credentials(Credentials.fromSecret(rdsSecret))
                .build());

        GraphqlApi api = new GraphqlApi(this, "AppSyncRDSResolver", GraphqlApiProps.builder()
                .name("AppsyncRDSResolverAPI")
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

        RdsDataSource ds = api.addRdsDataSource("AuroraDS", cluster, rdsSecret, DB_NAME);

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("getPost")
                .typeName("Query")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"select * from posts where id = '$ctx.args.id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "#set($output = $utils.rds.toJsonObject($ctx.result)[0])\n" +
                        "#if ($output.isEmpty())\n" +
                        "  null\n" +
                        "#else \n" +
                        "  $utils.toJson($output[0])\n" +
                        "#end"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("getPostsByAuthor")
                .typeName("Query")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"select * from posts where author = '$ctx.args.author'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("getCommentsByAuthor")
                .typeName("Query")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"select * from comments where author = '$ctx.args.author'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("getNumberOfCommentsOnPost")
                .typeName("Query")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"select count(*) from comments where postId = '$ctx.args.postId'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[0][0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("getCommentsOnPost")
                .typeName("Query")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"select * from comments where postId = '$ctx.source.postId'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("createComment")
                .typeName("Mutation")
                .requestMappingTemplate(MappingTemplate.fromString("#set($id=$utils.autoId())\n" +
                        "{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "    \t\"insert into comments(id, author, postId, content, upvotes, downvotes) values ('$id', '$ctx.args.author', '$ctx.args.postId','$ctx.args.content', 0, 0)\",\n" +
                        "        \"select * from comments where id = '$id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[1][0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("upvoteComment")
                .typeName("Mutation")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"update comments set upvotes = upvotes+1 where id = '$ctx.args.id'\",\n" +
                        "        \"select * from comments where id = '$ctx.args.id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[1][0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("downvoteComment")
                .typeName("Mutation")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"update comments set downvotes = downvotes+1 where id = '$ctx.args.id'\",\n" +
                        "        \"select * from comments where id = '$ctx.args.id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[1][0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("createPost")
                .typeName("Mutation")
                .requestMappingTemplate(MappingTemplate.fromString(" #set($id=$utils.autoId()) {\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "    \t\"insert into posts(id, author, content, views) values ('$id', '$ctx.args.author','$ctx.args.content', 0)\",\n" +
                        "        \"select * from posts where id = '$id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[1][0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("incrementViewCount")
                .typeName("Mutation")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"update posts set views = views+1 where id = '$ctx.args.id'\",\n" +
                        "        \"select * from posts where id = '$ctx.args.id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[1][0])"))
                .build());

        ds.createResolver(BaseResolverProps.builder()
                .fieldName("comments")
                .typeName("Post")
                .requestMappingTemplate(MappingTemplate.fromString("{\n" +
                        "    \"version\": \"2018-05-29\",\n" +
                        "    \"statements\": [\n" +
                        "        \"select * from comments where postId = '$ctx.source.id'\"\n" +
                        "    ]\n" +
                        "}"))
                .responseMappingTemplate(MappingTemplate.fromString("#if($ctx.error)\n" +
                        "    $utils.error($ctx.error.message, $ctx.error.type)\n" +
                        "#end\n" +
                        "\n" +
                        "$utils.toJson($utils.rds.toJsonObject($ctx.result)[0])"))
                .build());

        CfnOutput output = new CfnOutput(this, "AppSyncAuroraAPIURL", CfnOutputProps.builder()
                .value(api.getGraphqlUrl())
                .build());

        //Lambda function custom resolver

        Role lambdaRole = new Role(this, "AppSyncAuroraStackDBInitLambdaRole", RoleProps.builder()
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build());
        lambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));
        lambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaVPCAccessExecutionRole"));

        Function dbInitFunction = new Function(this, "InitDBLambda", FunctionProps.builder()
                .role(lambdaRole)
                .memorySize(1024)
                .vpc(props.getVpc())
                .runtime(Runtime.JAVA_11)
                .environment(Map.of("RDS_SECRET", rdsSecret.getSecretArn(),
                        "CLUSTER_ARN", cluster.getClusterArn(),
                        "DATABASE_NAME", DB_NAME))
                .code(Code.fromAsset("../DBInitLambda/target/db-init-lambda-0.0.1-SNAPSHOT.jar"))
                .handler("software.amazonaws.example.handler.DBInitLambdaHandler::handleRequest")
                .timeout(Duration.seconds(70))
                .build());

        cluster.grantDataApiAccess(dbInitFunction);

        Provider dbInitProvider = new Provider(this, "AppSyncAuroraStackDBInitProvider", ProviderProps.builder()
                .onEventHandler(dbInitFunction)
                .build());

        String scriptFile = "scripts/dbinit.sql";
        String sqlScript = "";
        try {
            sqlScript = new String(Files.readAllBytes(Paths.get(scriptFile)));
        } catch (
                IOException e) {
            System.out.println("DB Initialization Failed !!!");
        }

        CustomResource cr = new CustomResource(this, "AppSyncAuroraStackInitDBCustomResource", CustomResourceProps.builder()
                .serviceToken(dbInitProvider.getServiceToken())
                .resourceType("Custom::InitDBProvider")
                .properties(Map.of("SqlScript", sqlScript))
                .build());

        cr.getNode().addDependency(cluster);

    }

    @lombok.Builder
    @Setter
    @Getter
    public static class AppsyncAuroraStackProps implements StackProps {
        private Vpc vpc;
        private Environment env;
    }


}
