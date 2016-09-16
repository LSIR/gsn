package ch.epfl.gsn.xpr;

import ch.epfl.gsn.xpr.BinaryXpr;
import ch.epfl.gsn.xpr.parser.XprParser;
import scala.util.Try;

public class ParseTest {
  public static void main(String[] args){
	  Try<BinaryXpr> parsed=XprParser.parseXpr("dico > 5");
	  //Try<BinaryXpr> parsed=XprParser$.MODULE$.parseXpr("dfsfsdf");
	  if (parsed.isFailure())
		  System.out.println("abcdcdc "+parsed.failed().get());
	  else System.out.println(parsed.get().toString());
  }
}
