//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.graphqllambda.service;

import graphql.schema.DataFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import software.amazonaws.sample.graphqllambda.dao.CommentDao;
import software.amazonaws.sample.graphqllambda.entity.Comment;
import software.amazonaws.sample.graphqllambda.entity.Post;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommentsDataService {

    private static final Logger logger = LogManager.getLogger(CommentsDataService.class);

    private CommentDao commentDao;

    public CommentsDataService(Connection connection) {
        this.commentDao = new CommentDao(connection);
    }

    public DataFetcher getCommentByPostFetcher() {
        return dataFetchingEnvironment -> {
            Post post = dataFetchingEnvironment.getSource();
            DataLoader<String, List<String>> dataLoader = dataFetchingEnvironment.getDataLoader("comments");
            return dataLoader.load(post.getId());
        };
    }

    public DataFetcher getCommentsCountFetcher() {
        return dataFetchingEnvironment -> {
            String postId = dataFetchingEnvironment.getArgument("postId");
            return commentDao.getCommentsByPost(postId).size();
        };
    }

    public DataFetcher getCommentByAuthorFetcher() {
        return dataFetchingEnvironment -> {
            String author = dataFetchingEnvironment.getArgument("author");
            return commentDao.getCommentsByAuthor(author);
        };
    }

    public DataFetcher getCommentByPostIdFetcher() {
        return dataFetchingEnvironment -> {
            String postId = dataFetchingEnvironment.getArgument("postId");
            return commentDao.getCommentsByAuthor(postId);
        };
    }

    public DataFetcher createCommentFetcher() {
        return dataFetchingEnvironment -> {
            Comment comment = Comment.builder()
                    .id(UUID.randomUUID().toString())
                    .downvotes(0)
                    .upvotes(0)
                    .postId(dataFetchingEnvironment.getArgument("postId"))
                    .content(dataFetchingEnvironment.getArgument("content"))
                    .author(dataFetchingEnvironment.getArgument("author"))
                    .build();
            commentDao.createComment(comment);
            return comment;
        };
    }

    public DataFetcher upvoteCommentFetcher() {
        return dataFetchingEnvironment -> {
            String commentId = dataFetchingEnvironment.getArgument("id");
            commentDao.upVoteComment(commentId);
            return commentDao.getCommentsById(commentId);
        };
    }

    public DataFetcher downvoteCommentFetcher() {
        return dataFetchingEnvironment -> {
            String commentId = dataFetchingEnvironment.getArgument("id");
            commentDao.downVoteComment(commentId);
            return commentDao.getCommentsById(commentId);
        };
    }

    public BatchLoader<String, List<Comment>> getCommentsDataLoader() {
        return list -> CompletableFuture.supplyAsync(() -> getCommentsForMultiplePostIds(list));
    }

    private List<List<Comment>> getCommentsForMultiplePostIds(List<String> postIds) {
        try {
            List<Comment> commentList = commentDao.getCommentsByPostIds(postIds);

            Map<String, List<Comment>> bulkComments = new HashMap<>();
            for (Comment comment : commentList) {
                List<Comment> tempList = bulkComments.computeIfAbsent(comment.getPostId(), k -> new ArrayList<>());
                tempList.add(comment);
                bulkComments.put(comment.getPostId(), tempList);
            }
            return postIds.stream()
                    .map(id -> bulkComments.containsKey(id) ? bulkComments.get(id) : new ArrayList<Comment>())
                    .collect(Collectors.toList());
        } catch (SQLException se) {
            throw new RuntimeException("Error geting comments from DB ", se);
        }
    }

}
