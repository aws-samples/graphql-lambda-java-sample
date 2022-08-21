//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.resolver.service;

import software.amazonaws.sample.resolver.dao.CommentDao;
import software.amazonaws.sample.resolver.dao.PostDao;
import software.amazonaws.sample.resolver.entity.Comment;
import software.amazonaws.sample.resolver.entity.Post;
import software.amazonaws.sample.resolver.util.JsonConverter;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueryService {
    private JsonConverter jsonConverter;
    private PostDao postDao;
    private CommentDao commentDao;

    public QueryService(Connection connection, JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
        this.postDao = new PostDao(connection);
        this.commentDao = new CommentDao(connection);
    }

    public List<Post> getPostByAuthor(Map<String, String> arguments) throws Exception {
        String author = arguments.get("author");
        return postDao.getPostByAuthor(author);
    }

    public Post getPostById(Map<String, String> arguments) throws Exception {
        String id = arguments.get("id");
        return postDao.getPostById(id);
    }

    public List<Comment> getCommentsByPost(Map<String, String> source) throws Exception {
        String postId = source.get("id");
        return commentDao.getCommentsByPost(postId);
    }

    public List<Comment> getCommentsOnPost(Map<String, String> arguments) throws Exception {
        String postId = arguments.get("postId");
        return commentDao.getCommentsByPost(postId);
    }

    public List<Comment> getCommentsByAuthor(Map<String, String> arguments) throws Exception {
        String author = arguments.get("author");
        return commentDao.getCommentsByAuthor(author);
    }

    public int getNumberOfCommentsOnPost(Map<String, String> arguments) throws Exception {
        String postId = arguments.get("postId");
        return commentDao.getCommentsByPost(postId).size();
    }

    public Post createPost(Map<String, String> arguments) throws Exception {
        Post post = Post.builder()
                .author(arguments.get("author"))
                .content(arguments.get("content"))
                .views(0)
                .id(UUID.randomUUID().toString())
                .build();
        postDao.createPost(post);
        return post;
    }

    public Comment createComment(Map<String, String> arguments) throws Exception {
        Comment comment = Comment.builder()
                .author(arguments.get("author"))
                .content(arguments.get("content"))
                .postId(arguments.get("postId"))
                .upvotes(0)
                .downvotes(0)
                .id(UUID.randomUUID().toString())
                .build();

        commentDao.createComment(comment);
        return comment;
    }

    public Comment upvoteComment(Map<String, String> arguments) throws Exception {
        String id = arguments.get("id");
        commentDao.upVoteComment(id);
        Comment comment = commentDao.getCommentsById(id);
        return comment;
    }

    public Comment downVoteComment(Map<String, String> arguments) throws Exception {
        String id = arguments.get("id");
        commentDao.downVoteComment(id);
        Comment comment = commentDao.getCommentsById(id);
        return comment;
    }

    public Post incrementViewCount(Map<String, String> arguments) throws Exception {
        String id = arguments.get("id");
        commentDao.upVoteComment(id);
        postDao.incrementViewCount(id);
        Post post = postDao.getPostById(id);
        return post;
    }
}