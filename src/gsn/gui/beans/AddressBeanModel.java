package gsn.gui.beans;

import gsn.beans.AddressBean;
import gsn.utils.KeyValueImp;
import org.apache.commons.collections.KeyValue;
import com.jgoodies.binding.beans.Model;
import com.jgoodies.binding.list.ArrayListModel;

public class AddressBeanModel extends Model {
	public static final String PROPERTY_WRAPPER = "wrapper";

	private String wrapper;

	private ArrayListModel predicates;

	public AddressBeanModel() {
		predicates = new ArrayListModel();
	}

	public AddressBeanModel(AddressBean addressBean) {
		wrapper = addressBean.getWrapper();
		predicates = new ArrayListModel();
		addPredicateList(addressBean.getPredicates());
	}

	private void addPredicateList(KeyValue[] keyValues) {
		for (int i = 0; i < keyValues.length; i++) {
			addKeyValue(keyValues[i]);
		}
	}

	public void addKeyValue(KeyValue keyValue) {
		predicates.add(keyValue);
	}
	
	public void removeKeyValue(KeyValue keyValue){
		predicates.remove(keyValue);
	}

	public ArrayListModel getPredicates() {
		return predicates;
	}

	public void setPredicates(ArrayListModel predicates) {
		this.predicates = predicates;
	}

	public String getWrapper() {
		return wrapper;
	}

	public void setWrapper(String wrapper) {
		this.wrapper = wrapper;
	}

	public AddressBean getAddressBean() {
		AddressBean addressBean = new AddressBean(getWrapper(), (KeyValue[]) predicates.toArray(new KeyValue[0]));
		return addressBean;
	}

	public AddressBeanModel clone(){
		AddressBeanModel copy = new AddressBeanModel();
		copy.setWrapper(getWrapper());
		ArrayListModel predicatesCopy = new ArrayListModel();
		for (int i = 0; i < predicates.size(); i++) {
			KeyValue oldKeyValue = (KeyValue) predicates.get(i);
			KeyValue keyValue = new KeyValueImp(String.valueOf(oldKeyValue.getKey()), String.valueOf(oldKeyValue.getValue()));
			predicatesCopy.add(keyValue); 
		}
		copy.setPredicates(predicatesCopy);
		return copy;
	}
}
