//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.graphqllambda.service;

import graphql.schema.DataFetcher;
import software.amazonaws.sample.graphqllambda.dao.PostDao;
import software.amazonaws.sample.graphqllambda.entity.Post;

import java.sql.Connection;
import java.util.UUID;

public class PostDataService {

    private PostDao postDao;

    public PostDataService(Connection connection) {
        this.postDao = new PostDao(connection);
    }

    public DataFetcher getPostByIdFetcher() {
        return dataFetchingEnvironment -> {
            String postId = dataFetchingEnvironment.getArgument("id");
            return postDao.getPostById(postId);
        };
    }

    public DataFetcher getPostByAuthorFetcher() {
        return dataFetchingEnvironment -> {
            String author = dataFetchingEnvironment.getArgument("author");
            return postDao.getPostByAuthor(author);
        };
    }

    public DataFetcher createPostFetcher() {
        return dataFetchingEnvironment -> {
            String author = dataFetchingEnvironment.getArgument("author");
            String content = dataFetchingEnvironment.getArgument("content");
            String uuid = UUID.randomUUID().toString();
            Post post = Post.builder()
                    .id(uuid)
                    .author(author)
                    .content(content)
                    .views(0)
                    .build();
            postDao.createPost(post);
            return post;
        };
    }

    public DataFetcher incrementViewCounterFetcher() {
        return dataFetchingEnvironment -> {
            String postId = dataFetchingEnvironment.getArgument("id");
            postDao.incrementViewCount(postId);
            return postDao.getPostById(postId);
        };
    }


}
