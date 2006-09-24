package gsn.utils;

/**
 * Created by IntelliJ IDEA.
 * User: alisalehi
 * Date: Sep 6, 2006
 * Time: 5:58:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ChangeListener {
    public void changeHappended(String changeType,Object changedKey, Object changedValue);
}
