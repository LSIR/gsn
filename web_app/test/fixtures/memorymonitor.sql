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
-- Table structure for table `memorymonitor`
--

CREATE TABLE IF NOT EXISTS `memorymonitor` (
  `PK` int(11) NOT NULL default '0',
  `timed` bigint(11) default NULL,
  `HEAP` double default NULL,
  `CACHE` double default NULL,
  `RESIDENT` double default NULL,
  `UNMAPPED` double default NULL,
  PRIMARY KEY  (`PK`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `memorymonitor`
--

INSERT INTO `memorymonitor` (`PK`, `timed`, `HEAP`, `CACHE`, `RESIDENT`, `UNMAPPED`) VALUES
(1, 2147483647, 9873242112, 2003987584, 102, 1009),
(2, 2147483647, 9873745920, 2003987584, 102, 1009),
(3, 2147483647, 9873249280, 2003987584, 102, 1009),
(4, 2147483647, 9833245696, 2003987584, 102, 1009),
(5, 2147483647, 9853285376, 2003987584, 102, 1009),
(6, 2147483647, 9873445888, 2003987584, 102, 1009),
(7, 2147483647, 9878245376, 2003987584, 102, 1009),
(8, 2147483647, 9876245504, 2003987584, 102, 1009);
