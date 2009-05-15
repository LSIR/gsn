package gsn.beans.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class WebCommand extends NameDescriptionClass {
    private String rule;

    @ManyToOne
    private WebInput webInput;

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public WebInput getWebInput() {
        return webInput;
    }

    public void setWebInput(WebInput webInput) {
        this.webInput = webInput;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof WebCommand)) return false;

        WebCommand that = (WebCommand) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}
