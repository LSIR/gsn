package ch.epfl.gsn.metadata.core.model;

/**
 * Created by kryvych on 10/03/15.
 */
public class WikiInfo {

    private String measurementLocationName;

    private String deploymentName;

    private String wikiLink;

    private RelativePosition relativePosition;

    private String organisation;

    private String email;

    public WikiInfo(String measurementLocationName, String deploymentName, String wikiLink, RelativePosition relativePosition) {
        this.measurementLocationName = measurementLocationName;
        this.deploymentName = deploymentName;
        this.wikiLink = wikiLink;
        this.relativePosition = relativePosition;
    }

    public String getMeasurementLocationName() {
        return measurementLocationName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getWikiLink() {
        return wikiLink;
    }

    public RelativePosition getRelativePosition() {
        return relativePosition;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
