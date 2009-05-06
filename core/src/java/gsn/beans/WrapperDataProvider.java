package gsn.beans;

public class WrapperDataProvider extends DataProvider {

    public void addChild(DataProvider child) {
        throw new RuntimeException("Adding data providers to the WrapperDataProvider is not allowed.");
    }

}
