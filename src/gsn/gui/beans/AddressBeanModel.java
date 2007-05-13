package gsn.gui.beans;

import org.apache.commons.collections.KeyValue;

import gsn.beans.AddressBean;

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

}
