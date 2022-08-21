
CREATE TABLE IF NOT EXISTS posts (
    id        VARCHAR(64) NOT NULL,
    author    VARCHAR(128) NOT NULL,
    content   VARCHAR(255) NOT NULL,
    views   INT NOT NULL,
    PRIMARY KEY(id)
    );

CREATE TABLE IF NOT EXISTS comments (
            id        VARCHAR(64) NOT NULL,
            author    VARCHAR(128) NOT NULL,
            postId    VARCHAR(64) NOT NULL,
            content   VARCHAR(255) NOT NULL,
            upvotes   INT NOT NULL,
            downvotes INT NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(postId) REFERENCES posts(id)
);
