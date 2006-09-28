package gsn.beans;

import gsn.utils.KeyValueImp;

import java.util.ArrayList;

import org.apache.commons.collections.KeyValue;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 *         Date: Aug 4, 2005 <br>
 *         Time: 10:46:16 PM <br>
 */
public final class AddressBean {

    private String wrapper;

    private ArrayList<KeyValue> predicates;

    public AddressBean() {
    }

    public AddressBean(final String wrapper,
	    final ArrayList<KeyValue> predicates) {
	this.wrapper = wrapper;
	this.predicates = predicates;
    }

    /**
         * @param predicates
         *                The predicates to set.
         */
    public void setPredicates(final ArrayList<KeyValue> predicates) {
	this.predicates = predicates;
    }

    /**
         * @param wrapper
         *                The wrapper to set.
         */
    public void setWrapper(final String wrapper) {
	this.wrapper = wrapper;
    }

    public String getWrapper() {
	return this.wrapper;
    }

    public ArrayList<KeyValue> getPredicates() {
	return this.predicates;
    }

    public void addPredicate(final String key, final String value) {
	this.predicates.add(new KeyValueImp(key, value));
    }

    /**
         * Note that the key for the value is case insensitive.
         * 
         * @param key
         * @return
         */
    public String getPredicateValue(String key) {
	key = key.trim();
	for (final KeyValue predicate : this.predicates) {
	    if (predicate.getKey().toString().trim().equalsIgnoreCase(key))
		return ((String) predicate.getValue()).trim();
	}
	return null;
    }

    /**
         * Returns true TIMEDever the set of predicates contain "port" and
         * "host" keys.
         * 
         * @return
         */
    public boolean isAbsoluteAddressSpecified() {
	boolean containsAddress = false;
	boolean containsPort = false;
	for (final KeyValue predicate : this.getPredicates()) {
	    if ("host".equalsIgnoreCase((String) predicate.getKey()))
		containsAddress = true;
	}
	for (final KeyValue predicate : this.getPredicates()) {
	    if ("port".equalsIgnoreCase((String) predicate.getKey()))
		containsPort = true;
	}
	return containsAddress && containsPort;
    }

    public boolean equals(final Object o) {
	if (this == o)
	    return true;
	if (o == null || this.getClass() != o.getClass())
	    return false;

	final AddressBean addressBean = (AddressBean) o;

	if (!this.predicates.equals(addressBean.predicates))
	    return false;
	if (!this.wrapper.equals(addressBean.wrapper))
	    return false;

	return true;
    }

    public int hashCode() {
	int result;
	result = this.wrapper.hashCode();
	result = 29 * result + this.predicates.hashCode();
	return result;
    }

    public String toString() {
	final StringBuffer result = new StringBuffer("[").append(this
		.getWrapper());
	for (final KeyValue predicate : this.predicates) {
	    result.append(predicate.getKey() + " = " + predicate.getValue()
		    + ",");
	}
	result.append("]");
	return result.toString();
    }
}
