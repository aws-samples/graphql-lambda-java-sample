//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.resolver.service;

import software.amazonaws.sample.resolver.dao.CommentDao;
import software.amazonaws.sample.resolver.util.JsonConverter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchCommentsService {
    private JsonConverter jsonConverter;
    private CommentDao commentDao;

    public BatchCommentsService(Connection connection, JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
        this.commentDao = new CommentDao(connection);
    }

    public List<String> getComments(List<Map<String, String>> sources) throws SQLException {
        return sources.stream()
                .map(source -> {
                    try {
                        return commentDao.getCommentsByPost(source.get("id"));
                    } catch (SQLException se) {
                        throw new RuntimeException(se);
                    }
                })
                .map(comments -> jsonConverter.toJson(comments))
                .collect(Collectors.toList());
    }

}