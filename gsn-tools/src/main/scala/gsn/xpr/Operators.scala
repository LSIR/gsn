package gsn.xpr

object OpEnum extends Enumeration {
  type Op = Value
  val Gt=Value(">")
  val Lt=Value("<")
  val Eq=Value("=")
  val Geq=Value(">=")
  val Leq=Value("<=")
}


