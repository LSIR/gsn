/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/utils/jndi/GSNContext.java
*
* @author Timotee Maret
*
*/

package gsn.utils.jndi;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.naming.*;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class GSNContext  implements Context {

    private static final transient Logger logger = LoggerFactory.getLogger(GSNContext.class);

    private static InitialContext mainContext;

    static {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, GSNContextFactory.class.getCanonicalName());
        try {
            mainContext = new InitialContext(props);
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static Context getMainContext() {
        return mainContext;
    }

    private ConcurrentHashMap<String,Object> map = new ConcurrentHashMap();

    public Object lookup(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookup(String name) throws NamingException {
        return map.get(name);
    }

    public void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void bind(String name, Object obj) throws NamingException {
        map.put(name,obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rebind(String name, Object obj) throws NamingException {
        map.put(name,obj);
    }

    public void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void unbind(String name) throws NamingException {
        map.remove(name);
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookupLink(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookupLink(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NameParser getNameParser(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NameParser getNameParser(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public String composeName(String name, String prefix) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void close() throws NamingException {
        throw new OperationNotSupportedException();
    }

    public String getNameInNamespace() throws NamingException {
        return "";
    }
}
