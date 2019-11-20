

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for password
-- ----------------------------
DROP TABLE IF EXISTS `password`;
CREATE TABLE `password` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `plaintext` varchar(32) DEFAULT NULL,
  `ciphertext` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `plaintext` (`plaintext`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2965593 DEFAULT CHARSET=utf8;
