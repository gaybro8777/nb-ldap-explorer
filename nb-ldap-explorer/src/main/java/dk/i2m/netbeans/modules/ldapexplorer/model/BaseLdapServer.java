/*
 *  Copyright 2010 Interactive Media Management
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package dk.i2m.netbeans.modules.ldapexplorer.model;

import dk.i2m.netbeans.modules.ldapexplorer.security.XTrustProvider;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.openide.filesystems.FileObject;

/**
 * Connection to an {@link LdapServer}.
 *
 * @author Allan Lykke Christensen
 */
public class BaseLdapServer {
    private Comparator<LdapEntry> labelSorter = new Comparator<LdapEntry>() {
            public int compare(LdapEntry t, LdapEntry t1) {
                return t.getLabel().compareTo(t1.getLabel());
            }
        };
    private String identifier = null;
    private String host;
    private int port;
    private boolean secure = false;
    private int timeout = 5000;
    private String baseDN;
    private Authentication authentication = Authentication.NONE;
    private String binding = "";
    private String password = "";
    private String label = "";
    private LdapContext dirCtx = null;

    /**
     * Creates a new {@link LdapServer}.
     */
    public BaseLdapServer() {
        this("", 389, "");
    }

    /**
     * Creates a new {@link LdapServer}.
     *
     * @param host
     *          Hostname or IP of the {@link LdapServer}
     * @param port
     *          Port of the {@link LdapServer}
     * @param baseDN
     *          Base distinguished name
     */
    public BaseLdapServer(String host, int port, String baseDN) {
        this.host = host;
        this.port = port;
        this.baseDN = baseDN;
    }

    /**
     * Gets the unique identifier of the {@link LdapServer}. The identifier
     * is the same as the name of the {@link FileObject} storing the
     * {@link LdapServer}.
     *
     * @return Unique identifier of the {@link LdapServer}
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the unique identifier of the {@link LdapServer}. The identifier
     * should not be changed after the {@link LdapServer} is stored as it the
     * same as the name of the {@link FileObject} storing the
     * {@link LdapServer}.
     *
     * @param identifier
     *          Unique identifier of the {@link LdapServer}.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the type of {@link Authentication} for the server.
     *
     * @return Type of {@link Authentication} for the server
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * Sets the type of {@link Authentication} for the server.
     *
     * @param authentication
     *          Type of {@link Authentication}
     */
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
        fire("authentication", null, authentication);
    }

    /**
     * Gets the base distinguished name of the connection.
     * 
     * @return Base distinguished name of the connection
     */
    public String getBaseDN() {
        return baseDN;
    }

    /**
     * Sets the base distinguished name of the connection.
     *
     * @param baseDN
     *          Base distinguished name of the connection
     */
    public void setBaseDN(String baseDN) {
        String oldBaseDN = this.baseDN;
        this.baseDN = baseDN;
        fire("baseDN", oldBaseDN, baseDN);
    }

    /**
     * Gets the host name or IP address of the LDAP service.
     *
     * @return Host name or IP address of the LDAP service
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the host name or IP address of the LDAP service.
     *
     * @param host
     *          Host name or IP address of the LDAP service
     */
    public void setHost(String host) {
        String oldHost = this.host;
        this.host = host;
        fire("host", oldHost, host);
    }

    /**
     * Gets the port of the LDAP service.
     *
     * @return Port of the LDAP service
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Sets the port of the LDAP service.
     *
     * @param port
     *          Port of the LDAP service
     */
    public void setPort(Integer port) {
        Integer oldPort = Integer.valueOf(this.port);
        this.port = port;
        fire("port", oldPort, port);
    }

    /**
     * Determines if the connection to the {@link LdapServer} is secure.
     * 
     * @return <code>true</code> if the connection to the {@link LdapServer}
     *         is secure, otherwise <code>false</code>
     */
    public Boolean isSecure() {
        return secure;
    }

    public Boolean getSecure() {
        return secure;
    }

    /**
     * Sets the secure status of the {@link LdapServer} connection.
     *
     * @param secure
     *          Secure status of the {@link LdapServer} connection
     */
    public void setSecure(Boolean secure) {
        this.secure = secure;
        fire("secure", null, secure);
    }

    /**
     * Gets the user binding of the connection. This is only considered when
     * the {@link Authentication} type requires it (e.g.
     * {@link Authentication#SIMPLE}).
     *
     * @return User binding of the connection
     */
    public String getBinding() {
        return binding;
    }

    /**
     * Sets the user binding of the connection. This is only considered when
     * the {@link Authentication} type requires it (e.g.
     * {@link Authentication#SIMPLE}).
     *
     * @param binding
     *          User binding
     */
    public void setBinding(String binding) {
        this.binding = binding;
        fire("binding", null, binding);
    }

    /**
     * Gets the password for the binding.
     *
     * @return Password for the binding
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for the binding.
     *
     * @param password
     *          Password for the binding
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user friendly label of the server.
     * 
     * @return User friendly label of the server
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the user friendly label of the server.
     *
     * @param label
     *          User friendly label of the server
     */
    public void setLabel(String label) {
        this.label = label;
        fire("label", null, label);
    }

    /**
     * Gets the connection timeout of the server.
     *
     * @return Connection time out of the server
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Sets the connection timeout of the server.
     *
     * @param timeout
     *          Connection timeout of the server
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
        fire("timeout", null, timeout);
    }

    /**
     * Determines if the {@link LdapServer} is new. The {@link LdapServer} is
     * new if the {@link LdapServer#identifier} has not been set.
     *
     * @return <code>true</code> if the {@link LdapServer} is new, otherwise
     *         <code>false</code>
     */
    public boolean isNew() {
        if (this.identifier == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if the label has been set for the server.
     *
     * @return <code>true</code> if the label was set on the server, otherwise
     *         <code>false</code>
     */
    public boolean isLabelSet() {
        if (this.label == null || this.label.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Equality of an {@link LdapServer} is based on
     * {@link LdapServer#getIdentifier()}. Note that {@link LdapServer}s
     * without an identifier will all be equal.
     *
     * @param obj
     *          {@link LdapServer} to compare
     * @return <code>true</code> if the {@link LdapServer} is equal, otherwise
     *         <code>false</code>
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseLdapServer other = (BaseLdapServer) obj;
        if ((this.identifier == null) ? (other.identifier != null)
                : !this.identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash =
                41 * hash + (this.identifier != null
                ? this.identifier.hashCode() : 0);
        return hash;
    }

    /**
     * The {@link String} representation of this object is the connection
     * string for the LDAP service.
     *
     * @return String representation of the server
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (secure) {
            sb.append("ldaps://");
        } else {
            sb.append("ldap://");
        }
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append("/");
        sb.append(baseDN);
        return sb.toString();
    }

    /**
     * Generates a {@link Hashtable} with the environment for connecting to
     * the {@link LdapServer}.
     *
     * @return {@link Hashtable} containing a configured LDAP environment
     */
    protected Hashtable<String, String> getConnectionEnvironment() {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, toString());
        env.put("com.sun.jndi.ldap.connect.timeout",
                String.valueOf(this.timeout));

        if (this.authentication.equals(Authentication.SIMPLE)) {
            env.put(Context.SECURITY_AUTHENTICATION, this.authentication.name().
                    toLowerCase());
            env.put(Context.SECURITY_PRINCIPAL, this.binding);
            env.put(Context.SECURITY_CREDENTIALS, this.password);
        }

        return env;
    }

    /**
     * Connects to the {@link LdapServer}.
     *
     * @throws ConnectionException
     *          If a connection could not be established
     */
    public void connect() throws ConnectionException {
        try {
            if (isSecure()) {
                XTrustProvider.install();
            }

            this.dirCtx =
                    new InitialLdapContext(getConnectionEnvironment(), null);
        } catch (NamingException ex) {
            throw new ConnectionException(ex);
        }
    }

    /**
     * Disconnects from an {@link LdapServer}.
     * 
     * @throws ConnectionException
     *          If the connection could not be abrupted
     */
    public void disconnect() throws ConnectionException {
        if (this.dirCtx != null) {
            try {
                this.dirCtx.close();
            } catch (NamingException ex) {
                throw new ConnectionException(ex);
            }
        }
    }

    /**
     * Determines if a connection has already been established.
     *
     * @return <code>true</code> if a connection has already been established,
     *         otherwise <code>false</code>
     */
    public boolean isConnected() {
        return this.dirCtx != null;
    }

    /**
     * Gets a {@link List} of {@link LdapEntry} objects residing in the given
     * path.
     *
     * @param path
     *          Path for which to obtain the {@link LdapEntry} objects
     * @return {@link List} of {@link LdapEntry} objects residing in
     *         <code>path</code>
     * @throws QueryException
     *          If the path is invalid or the server could not be queried
     */
    public List<LdapEntry> getTree(String path) throws QueryException {
        List<LdapEntry> entries = new ArrayList<LdapEntry>();

        if (!isConnected()) {
            return entries;
        }

        try {
            NamingEnumeration<NameClassPair> results = this.dirCtx.list(path);

            while (results != null & results.hasMore()) {
                NameClassPair nc = results.next();

                // TASK: Instead of prepending the path to the name, it may be
                //       possibly to use nc.getNameInNamespace(). Need to verify

                LdapEntry entry = new LdapEntry();
                if (!path.isEmpty()) {
                    entry.setDn(nc.getName() + "," + path);
                    entry.setLabel(nc.getName());
                } else {
                    entry.setDn(nc.getName());
                    entry.setLabel(nc.getName());
                }

                addObjectClasses(entry);
                entries.add(entry);
            }

        } catch (NamingException ex) {
            if (ex instanceof SizeLimitExceededException) {
                Logger.getLogger(BaseLdapServer.class.getName()).warning(ex.
                        getMessage());
            } else {
                throw new QueryException(ex);
            }
        }

        Collections.sort(entries, labelSorter);

        return entries;
    }

    /**
     * Searches the subtree for LDAP entries matching the given {@code filter}.
     *
     * @param filter
     *          LDAP filter
     * @return {@link List} of {@link LdapEntry} objects matching the filter
     * @throws QueryException
     *          If the search failed
     */
    public List<LdapEntry> search(String filter) throws QueryException {
        List<LdapEntry> entries = new ArrayList<LdapEntry>();

        if (!isConnected()) {
            return entries;
        }

        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = this.dirCtx.search("",
                    filter, controls);

            while (results != null & results.hasMore()) {
                SearchResult sr = results.next();

                LdapEntry entry = new LdapEntry();

                entry.setDn(sr.getName());
                entry.setLabel(sr.getName());
                addObjectClasses(entry);

                entries.add(entry);
            }

        } catch (NamingException ex) {
            if (ex instanceof SizeLimitExceededException) {
                Logger.getLogger(BaseLdapServer.class.getName()).warning(ex.
                        getMessage());
            } else {
                throw new QueryException(ex);
            }
        }

        Collections.sort(entries, labelSorter);

        return entries;
    }

    /**
     * Adds object classes to an {@link LdapEntry}.
     *
     * @param entry
     *          {@link LdapEntry} for which to add object classes
     */
    private void addObjectClasses(LdapEntry entry) {
        try {
            Attributes attrs = this.dirCtx.getAttributes(entry.getDn(), new String[]{
                        "objectclass"});

            for (NamingEnumeration<? extends Attribute> ae = attrs.getAll(); ae.
                    hasMore();) {
                Attribute attr = ae.next();

                for (NamingEnumeration ne = attr.getAll(); ne.hasMore();) {
                    String objClass = (String) ne.next();
                    entry.addObjectClass(objClass);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(BaseLdapServer.class.getName()).warning(ex.getMessage());
        }
    }

    /**
     * Gets an {@link LdapEntry} from the LDAP server
     *
     * @param dn
     *          Distinguished name of the entry
     * @return {@link LdapEntry} matching the <code>dn</code>
     * @throws QueryException
     *          If the entry could not be retrieved
     */
    public LdapEntry getEntry(String dn) throws QueryException {
        StringBuilder fullDn = new StringBuilder(dn);
        fullDn.append(",");
        fullDn.append(baseDN);

        LdapEntry entry = new LdapEntry();
        entry.setDn(fullDn.toString());
        if (!isConnected()) {
            throw new QueryException("Not connected");
        }

        try {
            Attributes attrs = this.dirCtx.getAttributes(dn);

            for (NamingEnumeration<? extends Attribute> ae = attrs.getAll(); ae.
                    hasMore();) {
                Attribute attr = ae.next();

                for (NamingEnumeration ne = attr.getAll(); ne.hasMore();) {
                    entry.setAttribute(attr.getID(), ne.next());
                }
            }
        } catch (NamingException ex) {
            throw new QueryException(ex);
        }

        return entry;
    }
    private List<PropertyChangeListener> listeners = Collections.
            synchronizedList(new LinkedList<PropertyChangeListener>());

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        listeners.add(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        listeners.remove(pcl);
    }

    protected void fire(String propertyName, Object old, Object nue) {
        PropertyChangeListener[] pcls = listeners.toArray(
                new PropertyChangeListener[0]);
        for (int i = 0; i < pcls.length; i++) {
            pcls[i].propertyChange(new PropertyChangeEvent(this, propertyName,
                    old, nue));
        }
    }
}