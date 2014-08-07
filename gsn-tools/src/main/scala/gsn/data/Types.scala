package gsn.data

case class DataType(name:String)

object IntDT extends DataType("int")
object LongDT extends DataType("long")
object TimeDT extends DataType("time")
object DoubleDT extends DataType("double")

