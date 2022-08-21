//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.graphqllambda.dao;

import software.amazonaws.sample.graphqllambda.entity.Comment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommentDao {

    private Connection connection;

    public CommentDao(Connection connection) {
        this.connection = connection;
    }

    public List<Comment> getCommentsByPost(String postId) throws SQLException {
        String query = "select id, author, postId, content, upvotes, downvotes from comments "
                + "where postId = ?";

        return getCommentsByField(Optional.of(postId), query);
    }

    public List<Comment> getCommentsByPostIds(List<String> postIds) throws SQLException {
        String postList = postIds.stream()
                .map(id -> ("'" + id + "'"))
                .collect(Collectors.joining(","));

        String query = "select id, author, postId, content, upvotes, downvotes from comments "
                + "where postId in (" + postList + ")";

        return getCommentsByField(Optional.empty(),query);
    }

    public Comment getCommentsById(String commentId) throws SQLException {
        String query = "select id, author, postId, content, upvotes, downvotes from comments "
                + "where id = ?";

        List<Comment> comments = getCommentsByField(Optional.of(commentId), query);
        if (comments.size() == 0) {
            return null;
        }
        return comments.get(0);
    }

    public List<Comment> getCommentsByAuthor(String author) throws SQLException {
        String query = "select id, author, postId, content, upvotes, downvotes from comments "
                + "where author = ?";

        return getCommentsByField(Optional.of(author), query);
    }

    private List<Comment> getCommentsByField(Optional<String> param, String query) throws SQLException {
        List<Comment> commentList = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        if(param.isPresent()) {
            preparedStatement.setString(1, param.get());
        }
        ResultSet results = preparedStatement.executeQuery();

        while (results.next()) {
            Comment comment = Comment.builder()
                    .postId(results.getString("postId"))
                    .author(results.getString("author"))
                    .content(results.getString("content"))
                    .id(results.getString("id"))
                    .upvotes(results.getInt("upvotes"))
                    .downvotes(results.getInt("downvotes"))
                    .build();
            commentList.add(comment);
        }

        return commentList;
    }

    public void createComment(Comment comment) throws SQLException {
        String query = "insert into comments(id, author, postId, content, upvotes, downvotes) values (?, ? , ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, comment.getId());
        statement.setString(2, comment.getAuthor());
        statement.setString(3, comment.getPostId());
        statement.setString(4, comment.getContent());
        statement.setInt(5, comment.getUpvotes());
        statement.setInt(6, comment.getDownvotes());
        statement.executeUpdate();
    }

    public void upVoteComment(String commentId) throws SQLException {
        String query = "update comments set upvotes = upvotes+1 where id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, commentId);
        statement.executeUpdate();
    }

    public void downVoteComment(String commentId) throws SQLException {
        String query = "update comments set downvotes = downvotes+1 where id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, commentId);
        statement.executeUpdate();
    }

}
