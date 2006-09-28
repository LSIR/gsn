package gsn.beans;

import java.util.Collection;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class Modifications {

    private Collection<String> add;

    private Collection<String> remove;

    /**
         * @return Returns the add.
         */
    public Collection<String> getAdd() {
	return this.add;
    }

    /**
         * @param add
         *                The add to set.
         */
    public void setAdd(final Collection<String> add) {
	this.add = add;
    }

    /**
         * @return Returns the remove.
         */
    public Collection<String> getRemove() {
	return this.remove;
    }

    /**
         * @param remove
         *                The remove to set.
         */
    public void setRemove(final Collection<String> remove) {
	this.remove = remove;
    }

    public Modifications(final Collection<String> add,
	    final Collection<String> remove) {
	this.add = add;
	this.remove = remove;
    }
}
