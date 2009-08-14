package gsn.core;

import java.io.Serializable;
import java.util.Arrays;

import gsn.ConfigurationVisitor;
import gsn.Visitable;
import gsn.channels.ChannelConfig;
import gsn.utils.Param;
import gsn2.conf.Parameters;

public class OperatorConfig implements Visitable,Serializable{
	private String className="";
	private String identifier="";
	private boolean uniqueTimestamp = true;
	private Parameters parameters;
	private ChannelConfig[] channels ;
	
	public String getClassName() {
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

    public void setParameters(Param... params) {
		this.parameters = new Parameters(params);
	}

  public ChannelConfig[] getChannels() {
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
