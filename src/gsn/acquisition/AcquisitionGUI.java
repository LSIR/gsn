package gsn.acquisition;

import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class AcquisitionGUI extends JFrame{
  
  private static final long serialVersionUID = -8919399253033194054L;

  private AcquisitionDirectory directory;

  public AcquisitionGUI(AcquisitionDirectory directory) {
    super("Acquisition Sub System for GSN");
    this.directory = directory;
    getContentPane().add(new JScrollPane(getTableForWrapper()));
    setSize(600,600);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);
  }
  
  public JTable getTableForWrapper() {
    final ArrayList<String> wrapperClasses = new ArrayList<String>();
    final ArrayList<String> wrapperNames = new ArrayList<String>();
//    for (String name : directory.getWrappers().keys()) {
//      wrapperClasses.add(directory.getWrappers().get(name));
//      wrapperNames.add(name);
//    }
      
    TableModel model = new AbstractTableModel() {

	private static final long serialVersionUID = -1201690522528140440L;
	
	String[] columns = new String[] {"Name","Class"};
     
      public String getColumnName(int column) {
        return columns[column];
      }

      public int getColumnCount() {
        return columns.length;
      }

      public int getRowCount() {
        return directory.getWrappers().size();
      }

      public Object getValueAt(int rowIndex, int columnIndex) {
       if (columnIndex==0)
         return wrapperNames.get(rowIndex);
       if (columnIndex==1)
         return wrapperClasses.get(rowIndex);
       return "Nothing ";
      }};
      
      return new JTable(model);
  }

  
  
}
