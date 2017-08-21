--Run this code with mysql, preferably by a root user (mysql user, not necissarily root unix user)
-- You can run this file by going to the mysql terminal: 'mysql -u <user> -p'
-- within mysql you can type each command one by one or run the file itself.


--Create user paul, with the password. 
--(Password is not to be secure, but just to have something in place. 
-- The security is closing SQL for external connections, 
-- as this tool does not need external access or anything.)
CREATE USER 'paul'@'localhost' IDENTIFIED BY '[paul3514]';

--Create the *database* named paul, this is NOT the user, it's just a bad naming scheme I used... sorry.
CREATE DATABASE paul;

# Grant user Paul priviliges to the paul.* tables.
GRANT ALL PRIVILEGES ON paul.* TO 'paul'@'localhost';

--Start using the paul database.
USE paul;

--Create the tables
CREATE TABLE `refs` (
  `cochrane_id` char(15) NOT NULL,
  `reference_pubmed_id` char(15) NOT NULL,
  `topic` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `refs_author` (
  `pubmed_id` char(15) NOT NULL,
  `author_first` varchar(50) DEFAULT NULL,
  `author_last` varchar(50) DEFAULT NULL,
  `first_gender` varchar(10) DEFAULT NULL,
  `last_gender` varchar(10) DEFAULT NULL,
  `first_probability` int(11) DEFAULT NULL,
  `last_probability` int(11) DEFAULT NULL,
  `title` varchar(300) DEFAULT NULL,
  `year` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`pubmed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


--Ready to go?
