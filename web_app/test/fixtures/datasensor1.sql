-- phpMyAdmin SQL Dump
-- version 2.11.3deb1ubuntu1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Oct 13, 2008 at 07:14 PM
-- Server version: 5.0.51
-- PHP Version: 5.2.4-2ubuntu5.2

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `gsn`
--

-- --------------------------------------------------------

--
-- Table structure for table `datasensor1`
--

CREATE TABLE IF NOT EXISTS `datasensor1` (
  `PK` int(11) NOT NULL default '0',
  `timed` bigint(11) default NULL,
  `RH` double default NULL,
  `TA` double default NULL,
  `AP` double default NULL,
  `AL` double default NULL,
  PRIMARY KEY  (`PK`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `datasensor1`
--

INSERT INTO `datasensor1` (`PK`, `timed`, `RH`, `TA`, `AP`, `AL`) VALUES
(1, 1224009124417, 1.04, 103.44, -12.30, 0.53),
(2, 1224009224417, 1.01, 103.75, -11.30, 0.55),
(3, 1224009324417, 0.99, 103.94, -12.77, 0.51),
(4, 1224009424417, 0.86, 104.05, -19.50, 0.52),
(5, 1224009524417, 0.94, 102.55, -17.88, 0.50),
(6, 1224009624417, 0.73, 194.27, -12.50, 0.58),
(7, 1224009724417, 1.38, 101.11, -8.391, 0.51),
(8, 1224009824417, 1.09, 108.94, -2.901, 0.51);
