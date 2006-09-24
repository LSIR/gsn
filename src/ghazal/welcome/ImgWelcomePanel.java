package ghazal.welcome ;

import java.awt.BorderLayout ;
import java.awt.FlowLayout ;
import java.io.IOException ;

import javax.swing.ImageIcon ;
import javax.swing.JFrame ;
import javax.swing.JLabel ;
import javax.swing.JPanel ;

import com.izforge.izpack.installer.InstallData ;
import com.izforge.izpack.installer.InstallerFrame ;
import com.izforge.izpack.installer.IzPanel ;
import com.izforge.izpack.installer.ResourceManager ;
import com.izforge.izpack.installer.ResourceNotFoundException ;

public class ImgWelcomePanel extends IzPanel {
   public ImgWelcomePanel ( InstallerFrame parent , InstallData idata ) {
      super ( parent , idata ) ;
      JPanel panel = new JPanel ( new BorderLayout ( 1 , 1 ) ) ;
      ImageIcon logo = null ;
      try {
         logo = ResourceManager.getInstance ( ).getImageIconResource ( "ImgWelcomePanel.img" ) ;
      } catch ( ResourceNotFoundException e ) {
         e.printStackTrace ( ) ;
         System.exit ( 1 ) ;
      } catch ( IOException e ) {
         e.printStackTrace ( ) ;
         System.exit ( 1 ) ;
      }

      panel.add ( new JLabel ( logo ) ) ;
      String msg = "<html><center>" + idata.info.getAppName ( ) + " " + idata.info.getAppVersion ( ) + "<br>" + idata.info.getAppURL ( ) ;
      JLabel msgLbl = new JLabel ( msg , JLabel.CENTER ) ;
      panel.add ( msgLbl , BorderLayout.SOUTH ) ;
      add ( panel ) ;
   }

   public static void main ( String [ ] args ) {
      JFrame f = new JFrame ( ) ;
      JPanel panel = new JPanel ( new FlowLayout ( FlowLayout.CENTER ) ) ;
      panel.add ( new JLabel ( new ImageIcon ( "/home/ali/workspace/GSN/installer/ghazallogo.jpeg" ) ) ) ;
      String msg = "<html><center>" + "MyPPlication" + " " + "Beta2" + "<br>" + "http://..." ;
      JLabel msgLbl = new JLabel ( msg , JLabel.CENTER ) ;
      panel.add ( msgLbl ) ;
      f.getContentPane ( ).add ( panel ) ;

      f.setSize ( 400 , 400 ) ;
      f.setVisible ( true ) ;

   }
}
