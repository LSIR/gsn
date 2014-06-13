package gsn.xpr;

import scala.util.Try;
import gsn.xpr.parser.XprParser;
import gsn.xpr.parser.XprParser$;

public class ParseTest {
  public static void main(String[] args){
	  Try<BinaryXpr> parsed=XprParser.parseXpr("dico > 5");
	  //Try<BinaryXpr> parsed=XprParser$.MODULE$.parseXpr("dfsfsdf");
	  if (parsed.isFailure())
		  System.out.println("abcdcdc "+parsed.failed().get());
	  else System.out.println(parsed.get().toString());
  }
}
