package gsn2.conf;

import java.io.Serializable;
import java.util.Arrays;

import gsn.ConfigurationVisitor;
import gsn.Visitable;
import gsn.utils.Parameter;

public class OperatorConfig implements Visitable,Serializable{
	private String className="";
	private String identifier="";
	private boolean uniqueTimestamp = true;
	private Parameters parameters;
	private ChannelConfig[] channels ;
	
	public String getClassName() {
		if ( this.className == null ) 
			this.className = "gsn.vsensor.MirrorOperator";
		return this.className;
	}
	
	public void accept(ConfigurationVisitor v) {
		v.visit(this);
	}

  public OperatorConfig(){
    
  }
  
  public OperatorConfig(String identifier, String className) {
    this.identifier = identifier;
    this.className = className;
  }

 	public String getIdentifier() {
		return identifier.trim();
	}

	public boolean isUniqueTimestamp() {
		return uniqueTimestamp;
	}

	public Parameters getParameters() {
    if (this.parameters==null)
     return Parameters.EMPTY_PARAMETERS;
		return parameters;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public void setUniqueTimestamp(boolean uniqueTimestamp) {
		this.uniqueTimestamp = uniqueTimestamp;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

    public void setParameters(Parameter... parameters) {
		this.parameters = new Parameters(parameters);
	}

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OperatorConfig that = (OperatorConfig) o;

    if (uniqueTimestamp != that.uniqueTimestamp) return false;
    if (!Arrays.equals(channels, that.channels)) return false;
    if (className != null ? !className.equals(that.className) : that.className != null) return false;
    if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;
    if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;

    return true;
  }

  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
    result = 31 * result + (uniqueTimestamp ? 1 : 0);
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    result = 31 * result + (channels != null ? Arrays.hashCode(channels) : 0);
    return result;
  }

  public ChannelConfig[] getChannels() {
    if (this.channels==null)
     return new ChannelConfig[0];
		return channels;
  }

  public void setChannels(ChannelConfig... channels) {
    this.channels = channels;
  }

  public String toString() {
    return "OperatorConfig{" +
            "className='" + className + '\'' +
            ", identifier='" + identifier + '\'' +
            ", uniqueTimestamp=" + uniqueTimestamp +
            ", parameters=" + parameters +
            ", channels=" + (channels == null ? null : Arrays.asList(channels)) +
            '}';
  }
}
