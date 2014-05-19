package gsn.xpr.parser
import scala.util.parsing.combinator._
import gsn.xpr._

object XprParser extends JavaTokenParsers{
  def num: Parser[ValueXpr] = floatingPointNumber ^^ (a=>ValueXpr(a.toFloat))
  def vari: Parser[VarXpr] = stringLiteral ^^ (a=>VarXpr(a))
  def term: Parser[Xpr] = num | vari
  def op: Parser[Ope] = (">" | "<")  ^^ (a=>Ope(a))
  def biXpr: Parser[BinaryXpr] = ( term ~ op ~ term) ^^ {
    case  t1 ~ o ~ t2 =>        
      BinaryXpr(o.symbol,t1,t2)
  }
  def parse(input: String): Xpr = parseAll(num, input).get
} 