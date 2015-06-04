package ch.epfl.gsn.metadata.core.model;

/**
 * Created by kryvych on 10/03/15.
 */
public class Contact {

    private String name;

    private String organisation;

    private String email;

    public Contact(String name, String organisation, String email) {
        this.name = name;
        this.organisation = organisation;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getOrganisation() {
        return organisation;
    }

    public String getEmail() {
        return email;
    }
}
