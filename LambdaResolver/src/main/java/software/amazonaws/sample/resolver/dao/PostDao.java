//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.resolver.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazonaws.sample.resolver.entity.Post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostDao {

    private static final Logger logger = LogManager.getLogger(PostDao.class);
    private Connection connection;

    public PostDao(Connection connection) {
        this.connection = connection;
    }

    public Post getPostById(String postId) throws SQLException {

        String query = "select id, author, content, views from posts "
                + "where id = ?";

        List<Post> posts = getPostByField(postId, query);
        if (posts.size() == 0) {
            return null;
        }
        return posts.get(0);
    }

    public List<Post> getPostByAuthor(String author) throws SQLException {

        String query = "select id, author, content, views from posts "
                + "where author = ?";

        return getPostByField(author, query);
    }

    private List<Post> getPostByField(String param, String query) throws SQLException {
        List<Post> posts = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, param);
        logger.info("Prepared Statement::" + preparedStatement.toString());
        ResultSet results = preparedStatement.executeQuery();

        while (results.next()) {
            posts.add(Post.builder()
                    .id(results.getString("id"))
                    .author(results.getString("author"))
                    .content(results.getString("content"))
                    .views(results.getInt("views"))
                    .build());
        }
        return posts;
    }

    public void createPost(Post post) throws SQLException {
        String query = "insert into posts(id, author, content, views) values (?, ? , ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, post.getId());
        statement.setString(2, post.getAuthor());
        statement.setString(3, post.getContent());
        statement.setInt(4, post.getViews());
        statement.executeUpdate();

    }

    public void incrementViewCount(String postId) throws SQLException {
        String query = "update posts set views = views+1 where id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, postId);
        statement.executeUpdate();
    }


}
