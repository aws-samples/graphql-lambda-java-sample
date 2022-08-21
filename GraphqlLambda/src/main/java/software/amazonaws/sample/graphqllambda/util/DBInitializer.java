//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.graphqllambda.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class initializes the Database tables. This class will be called with every cold start.
 * This is done for POC purpose only for ease of managing table creation process.
 * In production workloads, it is recommended to integrate the creation of tables into your CI/CD processes.
 */

public class DBInitializer {
    private static final Logger logger = LogManager.getLogger(DBInitializer.class);
    private static final long RETRY_INTERVAL = 45000;
    private static final String CREATE_COMMENTS_TBL = "CREATE TABLE IF NOT EXISTS comments (" +
            "id        VARCHAR(64) NOT NULL," +
            "author    VARCHAR(128) NOT NULL," +
            "postId    VARCHAR(64) NOT NULL," +
            "content   VARCHAR(255) NOT NULL," +
            "upvotes   INT NOT NULL," +
            "downvotes INT NOT NULL," +
            "PRIMARY KEY(id)," +
            "FOREIGN KEY(postId) REFERENCES posts(id))";

    private static final String CREATE_POSTS_TBL = "CREATE TABLE IF NOT EXISTS posts (" +
            "id        VARCHAR(64) NOT NULL," +
            "author    VARCHAR(128) NOT NULL," +
            "content   VARCHAR(255) NOT NULL," +
            "views   INT NOT NULL," +
            "PRIMARY KEY(id))";

    private Connection connection;

    public DBInitializer() {
        try {

            String databaseName = System.getenv("DATABASE_NAME");
            String endpoint = System.getenv("END_POINT");
            String dbUserName = System.getenv("DB_USER_NAME");
            String region = System.getenv("REGION");
            int port = Integer.parseInt(System.getenv("DB_PORT"));

            DBUtil dbUtil = new DBUtil();
            this.connection = dbUtil.createConnectionViaIamAuth(dbUserName, endpoint, region, port);
            connection.setCatalog(databaseName);

            if (this.connection != null) {
                createTables();
            }

            logger.info("Finished Creating tables");

        } catch (SQLException e) {
            logger.info("Error initializing the DB", e);
            throw new RuntimeException("Error initializing the DB", e);
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    private void createTables() {
        try {
            Statement statement = this.connection.createStatement();
            statement.executeUpdate(CREATE_POSTS_TBL);
            statement.executeUpdate(CREATE_COMMENTS_TBL);
        } catch (SQLException e) {
            logger.error("Cannot create DB tables ", e);
        }
    }

}
