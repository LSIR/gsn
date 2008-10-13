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
(1, 2147483647, 1.04999995231628, 103.449996948242, -12.3000001907349, 0.5),
(2, 2147483647, 1.01999998092651, 103.75, -11.3000001907349, 0.550000011920929),
(3, 2147483647, 0.990000009536743, 103.949996948242, -12.7700004577637, 0.519999980926514),
(4, 2147483647, 0.860000014305115, 104.050003051758, -19.5, 0.529999971389771),
(5, 2147483647, 0.949999988079071, 102.550003051758, -17.8899993896484, 0.509999990463257),
(6, 2147483647, 0.730000019073486, 194.270004272461, -12.5, 0.589999973773956),
(7, 2147483647, 1.38999998569489, 101.110000610352, -8.39000034332275, 0.519999980926514),
(8, 2147483647, 1.0900000333786, 108.949996948242, -2.90000009536743, 0.519999980926514);
