CREATE DATABASE IF NOT EXISTS library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE library;

-- 图书表
DROP TABLE IF EXISTS book;
CREATE TABLE book (
    id        INT           AUTO_INCREMENT PRIMARY KEY COMMENT '图书编号',
    bookname  VARCHAR(100)  NOT NULL COMMENT '书名',
    author    VARCHAR(50)   NOT NULL COMMENT '作者',
    price     INT           NOT NULL COMMENT '价格（单位：分）',
    stock     INT           NOT NULL DEFAULT 1 COMMENT '库存数量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    username  VARCHAR(50)   PRIMARY KEY COMMENT '用户名',
    password  VARCHAR(100)  NOT NULL COMMENT '密码',
    role      VARCHAR(10)   NOT NULL COMMENT '角色（管理员/普通用户）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 借阅记录表
DROP TABLE IF EXISTS borrow_record;
CREATE TABLE borrow_record (
    id           INT       AUTO_INCREMENT PRIMARY KEY COMMENT '借阅记录编号',
    username     VARCHAR(50) NOT NULL COMMENT '借阅用户名',
    book_id      INT         NOT NULL COMMENT '借阅图书编号',
    borrow_date  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
    return_date  TIMESTAMP   NULL DEFAULT NULL COMMENT '归还时间',
    FOREIGN KEY (username) REFERENCES `user`(username),
    FOREIGN KEY (book_id)  REFERENCES book(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';
