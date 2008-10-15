-- phpMyAdmin SQL Dump
-- version 2.11.3deb1ubuntu1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Oct 13, 2008 at 07:15 PM
-- Server version: 5.0.51
-- PHP Version: 5.2.4-2ubuntu5.2

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `gsn`
--

-- --------------------------------------------------------

--
-- Table structure for table `datasensor2`
--

CREATE TABLE IF NOT EXISTS `datasensor2` (
  `PK` int(11) NOT NULL default '0',
  `timed` bigint(11) default NULL,
  `RH` double default NULL,
  `TA` double default NULL,
  `AP` double default NULL,
  `AL` double default NULL,
  PRIMARY KEY  (`PK`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `datasensor2`
--

INSERT INTO `datasensor2` (`PK`, `timed`, `RH`, `TA`, `AP`, `AL`) VALUES
(1, 1224009024417, -1.049, -103.449, 12.30, -0.587),
(2, 1224009124417, -1.019, -103.754, 11.30, -0.550),
(3, 1224009224417, -0.990, -103.949, 12.77, -0.519),
(4, 1224009324417, -0.860, -104.050, 19.54, -0.529),
(5, 1224009424417, -0.949, -102.550, 17.88, -0.509),
(6, 1224009524417, -0.730, -194.270, 12.51, -0.589),
(7, 1224009624417, -1.389, -101.110, 8.390, -0.519),
(8, 1224009724417, -1.090, -108.949, 2.900, -0.519);
