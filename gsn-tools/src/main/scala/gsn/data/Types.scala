package gsn.data

case class DataType(name:String)

object IntType extends DataType("int")
object LongType extends DataType("long")
object TimeType extends DataType("time")
object DoubleType extends DataType("double")
object StringType extends DataType("string")
object BinaryType extends DataType("binary")

