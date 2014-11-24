package gsn.xpr.parser
import scala.util.parsing.combinator._
import gsn.xpr._
import scala.util.Try

object XprParser extends JavaTokenParsers{
  def num: Parser[ValueXpr] = floatingPointNumber ^^ (a=>ValueXpr(a.toFloat))
  def vari: Parser[VarXpr] = this.ident ^^ (a=>VarXpr(a))
  def term: Parser[Xpr] = num | vari  
  import OpEnum._
  def op: Parser[Op] = (">="|"<="| "="|"<"|">")  ^^ (a=>OpEnum.withName(a))
  def biXpr: Parser[BinaryXpr] = ( term ~ op ~ term) ^^ {
    case  t1 ~ o ~ t2 => BinaryXpr(o,t1,t2)
  }
  def parseXpr(input: String) = Try{
    val p=parseAll(biXpr, input)
    if (p.successful) p.get
    else throw new Exception(p.toString)    
  }
} 