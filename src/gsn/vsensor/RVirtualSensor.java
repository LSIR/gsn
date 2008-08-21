package gsn.vsensor;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;

import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import org.apache.log4j.Logger;

public class RVirtualSensor extends AbstractVirtualSensor
{
  public String device = "jpeg"; // device we'll call (this would work with pretty much any bitmap device)
  public RConnection rc;
  public Image img;
  public REXP xp, w;
  public String [ ] fieldNames;

  private static final String           IMAGE_OUTPUT_FIELD      = "DATA";
  private static final int              IMAGE_OUTPUT_FIELD_TYPE = DataTypes.BINARY;
  private static final String [ ]       OUTPUT_FIELDS           = new String [ ] { IMAGE_OUTPUT_FIELD };
  private static final Byte [ ]      OUTPUT_TYPES            = new Byte [ ] { IMAGE_OUTPUT_FIELD_TYPE };


  private static final transient Logger logger = Logger.getLogger(RVirtualSensor.class);

  public boolean initialize()
  {
    TreeMap <  String , String > params = getVirtualSensorConfiguration().getMainClassInitialParams();
    
    try
    {
      // connect to Rserve (if the user specified a server at the command line,
      // use it, otherwise connect locally)
      rc = new RConnection("127.0.0.1");

/*      // if Cairo is installed, we can get much nicer graphics, so try to load it
      if (rc.parseAndEval("suppressWarnings(require('Cairo',quietly=TRUE))")
          .asInteger() > 0)
        device = "CairoJPEG"; // great, we can use Cairo device
      else
        logger.info("(consider installing Cairo package for better bitmap output)");

      // we are careful here - not all R binaries support jpeg
      // so we rather capture any failures
      xp = rc.parseAndEval("try(" + device + "('test.jpg',quality=90))");

      if (xp.inherits("try-error"))
      { 
        // if the result is of the class try-error then there was a problem
        logger.error("Can't open " + device + " graphics device:\n" + xp.asString());
        // this is analogous to 'warnings', but for us it's sufficient to get
        // just the 1st warning
        w = rc.eval("if (exists('last.warning') && length(last.warning)>0) names(last.warning)[1] else 0");
        
        if (w.isString())
          logger.error(w.asString());
        return false;
      }*/


    }
    catch (RserveException rse)
    { 
      // RserveException (transport layer - e.g. Rserve is not running)
      logger.warn(rse);
    } 
/*    catch (REXPMismatchException mme)
    { 
      // REXP mismatch exception (we got something we didn't think we get)
      logger.warn(mme);
      mme.printStackTrace();
    }*/
    catch (Exception e)
    { 
      // something else
      logger.warn("Something went wrong, but it's not the Rserve: " + e.getMessage());
      e.printStackTrace();
    }

    return true;
  }

  public void dataAvailable(String inputStreamName, StreamElement data)
  {
    logger.info("Data received under the name: " + inputStreamName);
    
    Integer val =  Integer.parseInt(data.getData("packet_type").toString());
    
    logger.info("HERE -> " + val);
    
    val += 10;
    
    String xtitle = "Sample X-axis";
    String ytitle = "Sample Y-axis";
    String gtitle = "Random Number Graph";
    
    String statement =  new String(
                        "graphics.off(); jpeg('test.jpg',quality=90); dev.cur(); " + 
                        "x <- rnorm(10); " + 
                        "y <- rnorm(10); " + 
                        "plot(x,y, xlab='Sample X-axis', ylab='Sample Y-axis', main='Random Number Graph'); " + //, xlab=\"" + xtitle + "\", ylab=\"" + ytitle + "\", main=\"" + gtitle + "\"); "+ 
                        "dev.off(2);"
                        );
    
    try
    {
      // ok, so the device should be fine - let's plot - replace this by any
      // plotting code you desire ...
      //rc.parseAndEval("data(iris); attach(iris); plot(Sepal.Length, Petal.Length, col=unclass(Species)); dev.off()");
      
      rc.parseAndEval(statement);
      
      // There is no I/O API in REngine because it's actually more efficient to
      // use R for this
      // we limit the file size to 1MB which should be sufficient and we delete
      // the file as well
      
      xp = rc.parseAndEval("r=readBin('test.jpg','raw',1024*1024); unlink('test.jpg'); r");

    } catch (Exception e)
    {
      e.printStackTrace();
    }
    
    try
    {
      StreamElement output = new StreamElement( OUTPUT_FIELDS , OUTPUT_TYPES , new Serializable [ ] { xp.asBytes() } , data.getTimeStamp() );
    
      dataProduced(output);

    } catch (REXPMismatchException e)
    {
      e.printStackTrace();
    }
    
  }

  public void finalize()
  {
    // close RConnection, we're done
    rc.close();

  }

}
