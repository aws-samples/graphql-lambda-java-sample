//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.graphqllambda.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import software.amazonaws.sample.graphqllambda.service.CommentsDataService;
import software.amazonaws.sample.graphqllambda.service.PostDataService;

import java.net.URL;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

public class GraphQLUtil {

    private static final Logger logger = LogManager.getLogger(GraphQLUtil.class);

    static String SCHEMA_FILE_NAME = "schema.graphqls";

    private GraphQL graphQL;
    private JsonConverter jsonConverter;
    private Connection connection;
    private PostDataService postDataService;
    private CommentsDataService commentsDataService;

    public GraphQLUtil(JsonConverter jsonConverter, Connection connection) {
        this.connection = connection;
        this.jsonConverter = jsonConverter;
        this.postDataService = new PostDataService(this.connection);
        this.commentsDataService = new CommentsDataService(this.connection);
        this.graphQL = GraphQL.newGraphQL(buildSchema()).build();
    }

    public GraphQL getGraphQL() {
        return this.graphQL;
    }

    private GraphQLSchema buildSchema() {
        try {
            URL url = Resources.getResource(SCHEMA_FILE_NAME);
            String sdl = Resources.toString(url, Charsets.UTF_8);
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
            RuntimeWiring runtimeWiring = buildWiring();
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        } catch (Exception e) {
            logger.error("Error while building GraphQL Schema ", e);
            throw new RuntimeException(e);
        }

    }

    private RuntimeWiring buildWiring() {
        logger.info("POST DATA SERVICE ... " + postDataService);
        return RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("getPost", postDataService.getPostByIdFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Post")
                        .dataFetcher("comments", commentsDataService.getCommentByPostFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("getPostsByAuthor", postDataService.getPostByAuthorFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("getCommentsOnPost", commentsDataService.getCommentByPostIdFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("getNumberOfCommentsOnPost", commentsDataService.getCommentsCountFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("getCommentsByAuthor", commentsDataService.getCommentByAuthorFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                        .dataFetcher("createPost", postDataService.createPostFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                        .dataFetcher("incrementViewCount", postDataService.incrementViewCounterFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                        .dataFetcher("createComment", commentsDataService.createCommentFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                        .dataFetcher("upvoteComment", commentsDataService.upvoteCommentFetcher()))
                .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                        .dataFetcher("downvoteComment", commentsDataService.downvoteCommentFetcher()))
                .build();
    }

    public String processGraphQlRequest(String query, String operationName, String variablesJson) {

        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("comments", DataLoaderFactory.newDataLoader(commentsDataService.getCommentsDataLoader()));

        Map<String, Object> variableMap = convertVariablesJson(variablesJson);
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(operationName)
                .dataLoaderRegistry(registry)
                .variables(variableMap)
                .build();

        Object returnObj = graphQL.execute(executionInput).toSpecification();
        return jsonConverter.toJson(returnObj);
    }

    private Map<String, Object> convertVariablesJson(String jsonMap) {
        if (jsonMap == null) {
            return Collections.emptyMap();
        }
        return jsonConverter.fromJson(jsonMap, Map.class);
    }


}
