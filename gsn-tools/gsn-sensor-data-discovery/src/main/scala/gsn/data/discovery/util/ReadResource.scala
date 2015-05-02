package gsn.data.discovery.util

import java.io.File
import java.io.FileInputStream

trait ReadResource {
  
  def withFileInputStream(file:File)(op: FileInputStream => Unit) = {
    val is = new FileInputStream(file)
    try {
      op(is)
    } finally {
      is.close()
    }
  }
}