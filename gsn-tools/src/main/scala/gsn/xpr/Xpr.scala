package gsn.xpr

import OpEnum._

trait Xpr {
  def toString:String
}

case class VarXpr(name:String) extends Xpr{
  override def toString=name
}
case class ValueXpr(value:Any) extends Xpr{
  override def toString= if (value==null) "null" else value.toString
}
case class BinaryXpr(operator:Op,x1:Xpr,x2:Xpr) extends Xpr{
  override def toString=x1.toString+" "+operator+" "+x2.toString
}
