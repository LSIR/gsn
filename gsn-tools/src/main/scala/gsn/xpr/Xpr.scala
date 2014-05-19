package gsn.xpr

trait Xpr {

}

case class VarXpr(name:String) extends Xpr
case class ValueXpr(value:Any) extends Xpr
case class BinaryXpr(operator:String,x1:Xpr,x2:Xpr) extends Xpr
