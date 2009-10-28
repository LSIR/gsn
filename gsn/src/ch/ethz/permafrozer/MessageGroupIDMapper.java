package ch.ethz.permafrozer;

/**
 * Data type for SerialForwarderLogWrapper2
 * @author kellmatt
 *
 */
public class MessageGroupIDMapper
{
	private Integer _amType = null;
	
	private String _source = null;
	
    public MessageGroupIDMapper(String source, Integer amType) {
        _source = source;
        _amType = amType;
    }
    
    public boolean equals(Object anObject) {
        if(this == anObject) {
            return true;
        }
        if(anObject instanceof MessageGroupIDMapper) {
            MessageGroupIDMapper o1 = (MessageGroupIDMapper)anObject;
            if(o1._amType.equals(this._amType) && o1._source.equals(this._source)) {
                return true;
            }
        }
        return false;
    }
    
    public int hashCode() {
        return _source.hashCode() + _amType + 17;
    }
}
