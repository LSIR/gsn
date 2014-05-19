package gsn.xpr

case class Ope(symbol:String)

object Op extends Enumeration {
  type OpType = Value
  val Gt=Value(">")
  val Lt=Value("<")
  val Eq=Value("=")
  val Geq=Value(">=")
  val Leq=Value("<=")
}
