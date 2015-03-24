/*
 * Copyright (c) 2009 Intuit Inc. All Rights reserved.
 * -------------------------------------------------------------------------------------------------
 *
 * File name  : QuickBaseConnection.java
 * Created on : Dec 31, 2008
 * @author Mirko Raner
 * -------------------------------------------------------------------------------------------------
 *
 *
 * *************************************************************************************************
 */

package com.intuit.quickbase.api;

import static com.intuit.quickbase.api.QuickBaseAPICall.API_Authenticate;
import static com.intuit.quickbase.api.QuickBaseAPICall.API_FindDBByName;
import static com.intuit.quickbase.api.QuickBaseAPICall.API_SignOut;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The class {@link QuickBaseConnection} represents an HTTP connection to a QuickBase server.
 * A connection by itself is not specific to a particular database yet. The connection can be used
 * to obtain one or more {@link QuickBaseDatabase} objects. The connection object represents a
 * simulated HTTP "session", which is established by using cookies.
 *
 * @author Mirko Raner
 * @version $Revision: 13 $ $Change: 714052 $
 */
public class QuickBaseConnection
{
    private static final Logger log = LoggerFactory.getLogger(QuickBaseConnection.class);
    
    private final static NameValuePair[] NO_PARAMETERS = {};
    
    private final static int FIRST = 0;
    private final static int SECOND = 1;
    private final static char QUERY = '?';
    private final static String QB_ACTION_HEADER = "QUICKBASE-ACTION"; //$NON-NLS-1$

    private final static String ACT = "act"; //$NON-NLS-1$
    private final static String TICKET = "ticket"; //$NON-NLS-1$
    private final static String USERNAME = "username"; //$NON-NLS-1$
    private final static String PASSWORD = "password"; //$NON-NLS-1$
    private static final String HOURS = "hours"; //$NON-NLS-1$
    private final static String DBNAME = "dbname"; //$NON-NLS-1$

    private HttpClient httpClient;
    private String ticket;

    private String qbMainUrl = ""; //$NON-NLS-1$
    private String qbUrl = ""; //$NON-NLS-1$
    private Integer authHours = null;

    private PasswordAuthentication credentials;
    
    

    /**
     * Creates an authenticated connection to QuickBase.
     * 
     * If the authentication fails the connection is not established.
     * 
     * @param credentials
     * @param qbDomain
     * @param httpProtocol
     * @throws QuickBaseException
     */
    public QuickBaseConnection(PasswordAuthentication credentials, String qbDomain, String httpProtocol) throws QuickBaseException 
    {
        this(credentials, qbDomain, httpProtocol, null);
    }
    
    /**
     * Creates an authenticated connection to QuickBase.
     * 
     * If the authentication fails the connection is not established.
     * 
     * @param credentials
     * @param qbDomain
     * @param httpProtocol
     * @param authHours
     * @throws QuickBaseException
     */
    public QuickBaseConnection(PasswordAuthentication credentials, String qbDomain, String httpProtocol, Integer authHours) throws QuickBaseException 
    {
        if (authHours == null || authHours <= 0) {
            throw new QuickBaseException("Invalid authHours parameter: [" + authHours + "]");
        }
        
        try {
            this.qbMainUrl  = new URL(httpProtocol, qbDomain, -1, "/db/main?").toString();
            this.qbUrl      = new URL(httpProtocol, qbDomain, -1, "/db/").toString();
        } catch (MalformedURLException e) {
            throw new QuickBaseException("Authentication failed. Incorrect format of URL to connect to quickbase", e);
        }
        
        this.credentials = credentials;
        this.authHours = authHours;
        
        this.httpClient = new HttpClient();
        
        // Authenticate connection
        retrieveNewTicket();
    }
    
    public String getTicket() 
    {
        return ticket;
    }

    /**
     * Finds a database by its name. If multiple databases exist with that name
     * only the first database will be returned. In QuickBase, only database IDs
     * are unique (but database names not necessarily).
     * 
     * @param databaseName the name of the data base
     * @return the {@link QuickBaseDatabase} object of the database
     * @throws QuickBaseException if no database of that name could be found
     */
    public QuickBaseDatabase findDBByName(String databaseName) throws QuickBaseException 
    {
        return findDBsByName(databaseName).get(FIRST);
    }

    /**
     * Finds all databases with a given name. In QuickBase, only database IDs
     * are unique (but database names not necessarily).
     * 
     * @param databaseName the name of the data base
     * @return a {@link List} of {@link QuickBaseDatabase} objects
     * @throws QuickBaseException if no database of that name could be found
     */
    public List<QuickBaseDatabase> findDBsByName(final String databaseName) throws QuickBaseException 
    {
        Document response = executeRequest(new RequestBuilder() {
            @Override
            public HttpMethod getRequestMethod() {
                return method(API_FindDBByName, dbname(databaseName));
            };
        });
        
        NodeList dbids;
        try {
            dbids = (NodeList) QuickBaseXPath.QDBAPI_DBID.evaluate(response, XPathConstants.NODESET);
        } catch (XPathExpressionException xpathException) {
            throw new QuickBaseException(xpathException);
        }

        // TODO: com.intuit.tcr.utilities.NodeListIterable would come in handy here:
        int numberOfChildren = dbids.getLength();
        List<QuickBaseDatabase> databases = new ArrayList<QuickBaseDatabase>();
        for (int index = 0; index < numberOfChildren; index++) {
            Node dbidNode = dbids.item(index);
            String dbid = dbidNode.getTextContent();
            databases.add(new QuickBaseDatabase(this, dbid));
        }
        return databases;
    }

    /**
     * Executes a {@link QuickBaseAPICall} for a certain database and returns
     * the database response as a SAX {@link InputSource}. Uses the Http GET
     * method.
     * 
     * @param dbid the database ID
     * @param call the {@link QuickBaseAPICall} to be executed
     * @param parameters {@link NameValuePair}s for additional parameters
     * @return a SAX {@link InputSource} that contains the server's response
     * @throws QuickBaseException if the execution was unsuccessful
     */
    public Document execute(final String dbid, final QuickBaseAPICall call, final NameValuePair... parameters) throws QuickBaseException 
    {
        return executeRequest(new RequestBuilder() {
            @Override
            public HttpMethod getRequestMethod() {
                HttpMethod method = new GetMethod(qbUrl + dbid + QUERY);
                NameValuePair[] query = new NameValuePair[parameters.length + 2];
                query[FIRST] = act(call);
                query[SECOND] = ticket(ticket);
                System.arraycopy(parameters, 0, query, 2, parameters.length);
                method.setQueryString(query);
                return method;
            }
        });
    }
    
    /**
     * Executes a {@link QuickBaseAPICall} for a certain QuickBase object as an
     * XML payload and returns the response as a SAX {@link InputSource}. Uses
     * the HTTP Post method.
     * 
     * @param qbid The id of the object the call is acting upon
     * @param call {@link QuickBaseAPICall} to be executed
     * @param elements the XML elements to put into the payload
     * @return a SAX {@link InputSource} that contains the server's response
     * @throws QuickBaseException if the execution was unsuccessful
     */
    public Document executeXml(final String qbid, final QuickBaseAPICall call, final String... elements) throws QuickBaseException 
    {
        return executeRequest(new RequestBuilder() {
            @Override
            public HttpMethod getRequestMethod() throws QuickBaseException {

                StringBuilder xml = new StringBuilder();
                xml.append("<qdbapi>\n");

                xml.append("<ticket>");
                xml.append(getTicket());
                xml.append("</ticket>\n");

                for (String element : elements) {
                    xml.append(element);
                    xml.append("\n");
                }
                
                xml.append("</qdbapi>\n");
                
                String payload = xml.toString();
                
                log.debug("executeXml payload: {}", payload);

                StringRequestEntity formEntity;
                try {
                    formEntity = new StringRequestEntity(payload, "application/xml", "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.error("Cannot create a string request", e);
                    throw new QuickBaseException("Cannot create string request entity", e);
                }
                
                PostMethod method = new PostMethod(qbUrl + qbid);
                method.setRequestEntity(formEntity);
                method.addRequestHeader(QB_ACTION_HEADER, call.toString());
                return method;
            }
        });
    }

    /**
     * Executes a {@link QuickBaseAPICall} without additional parameters for a
     * certain database and returns the database response as a SAX
     * {@link InputSource}. Uses the Http Get Method
     * 
     * @param dbid the database ID
     * @param call the {@link QuickBaseAPICall} to be executed
     * @return a SAX {@link InputSource} that contains the server's response
     * @throws QuickBaseException if the execution was unsuccessful
     */
    public Document execute(String dbid, QuickBaseAPICall call) throws QuickBaseException 
    {
        return execute(dbid, call, NO_PARAMETERS);
    }

    /**
     * Logs off from QuickBase by invalidating the token.
     * 
     * @return document from QuickBase with the result.
     * @throws QuickBaseException
     */
    public Document logOff() throws QuickBaseException 
    {
        Document docResponse;
        try {
            docResponse = executeRequest(new RequestBuilder() {
                @Override
                public HttpMethod getRequestMethod() {
                    HttpMethod method = signOut();
                    return method;
                }
            });
        } catch (QuickBaseException e) {
            throw new QuickBaseException("Log off failed.", e);
        }
        
        return docResponse;
    }

    // ------------------------------------- PRIVATE SECTION BELOW

    /**
     * Executes any HTTP Get request. checks for basic error codes in response
     * document. returns the response document
     * 
     * @param requestBuilder
     * @return
     * @throws QuickBaseException
     */
    private Document executeRequest(RequestBuilder requestBuilder) throws QuickBaseException 
    {
        log.debug(">>> >>> >>> Execute request with ticket: {}", ticket);
        
        try {

            // Get the ticket before building the request method.
            String oldTicket = ticket;
            HttpMethod request = requestBuilder.getRequestMethod();

            httpClient.executeMethod(request);
            Document docResponse = getResponse(request);
            QuickBaseErrorCode errorCode = parseErrorCode(docResponse);
            
            if (errorCode == QuickBaseErrorCode.OK) {
                return docResponse;
            }
            
            log.warn("Got QuickBase error code: {}", errorCode);
            
            if (errorCode == QuickBaseErrorCode.INVALID_TICKET) {
                log.warn("Ticked might have expired. Trying to renew ticket and execute again.");
                
                synchronized (this) {
                    if (oldTicket.equals(ticket)) {
                        retrieveNewTicket();
                    }
                }
                
                log.debug(">>> >>> >>> Re execute request with ticket: {}", ticket);
                
                // Rebuild the request with the new ticket
                request = requestBuilder.getRequestMethod();
                
                httpClient.executeMethod(request);
                docResponse = getResponse(request);
                errorCode = parseErrorCode(docResponse);

                if (errorCode == QuickBaseErrorCode.OK) {
                    return docResponse;
                }
                
                log.info("After re-login got error code: {}", errorCode);
            }

            String errorText = QuickBaseXPath.QDBAPI_ERRTEXT.evaluate(docResponse);
            errorText += " (error code " + errorCode + ')'; //$NON-NLS-1$
            throw new QuickBaseException(errorText);
            
        } catch (IOException | SAXException | XPathExpressionException | ParserConfigurationException e ) {
            throw new QuickBaseException(e);
        }
    }

    private QuickBaseErrorCode parseErrorCode(Document docResponse) throws QuickBaseException 
    {
        try {
            int code = Integer.parseInt(QuickBaseXPath.QDBAPI_ERRCODE.evaluate(docResponse));
            QuickBaseErrorCode errorCode = QuickBaseErrorCode.valueOf(code);
            if (QuickBaseErrorCode.ERROR_CODE_NOT_RECOGNIZED == errorCode) {
                log.warn("Error code not recognized [{}]. Please update enum QuickBaseErrorCode (from https://www.quickbase.com/api-guide/whnjs.htm)", code);
            }
            return errorCode;
        } catch (XPathExpressionException e) {
            throw new QuickBaseException("Cannot retrieve error code.", e);
        }
    }

    private Document getResponse(HttpMethod method) throws IOException, SAXException, ParserConfigurationException 
    {
        // String response = method.getResponseBodyAsString();
        InputSource inputSource = new InputSource(method.getResponseBodyAsStream());
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(inputSource);
    }

    // A session is having a valid ticket from QuickBase
    private void retrieveNewTicket() throws QuickBaseException 
    {
        Document docResponse;
        try {
            docResponse = executeRequest(new RequestBuilder() {
                @Override
                public HttpMethod getRequestMethod() {
                    HttpMethod method = authenticate(credentials.getUserName(), credentials.getPassword());
                    return method;
                }
            });
        } catch (QuickBaseException e) {
            throw new QuickBaseException("Authentication failed.", e);
        }

        String ticket = null;
        try {
            ticket = QuickBaseXPath.QDBAPI_TICKET.evaluate(docResponse);
        } catch (XPathExpressionException e) {
            throw new QuickBaseException("Could not retrieve ticket.", e);
        }
        
        if (StringUtils.stripToNull(ticket) == null) {
            throw new QuickBaseException("Authentication failed. No ticket found in response.");
        }

        this.ticket = ticket;
    }

    private HttpMethod method(QuickBaseAPICall call, NameValuePair... parameters) 
    {
        HttpMethod method = new GetMethod(qbMainUrl);
        NameValuePair[] query = new NameValuePair[parameters.length + 1];
        System.arraycopy(parameters, 0, query, 1, parameters.length);
        query[FIRST] = act(call);
        method.setQueryString(query);
        return method;
    }

    private HttpMethod authenticate(String username, char[] password) 
    {
        GetMethod authenticate = new GetMethod(qbMainUrl);
        return authHours != null ? 
                query(authenticate, act(API_Authenticate), username(username), password(password), hours(authHours)) : 
                query(authenticate, act(API_Authenticate), username(username), password(password)); // default QuickBase
    }

    private HttpMethod signOut() 
    {
        GetMethod signOut = new GetMethod(qbMainUrl);
        return query(signOut, act(API_SignOut));
    }

    private HttpMethod query(HttpMethod method, NameValuePair... parameters) 
    {
        method.setQueryString(parameters);
        return method;
    }

    private NameValuePair act(QuickBaseAPICall action) {
        return new NameValuePair(ACT, action.toString());
    }

    private NameValuePair ticket(String ticket) {
        return new NameValuePair(TICKET, ticket);
    }

    private NameValuePair username(String username) {
        return new NameValuePair(USERNAME, username);
    }

    private NameValuePair password(char[] password) {
        return new NameValuePair(PASSWORD, String.valueOf(password));
    }

    private NameValuePair hours(Integer hours) {
        return new NameValuePair(HOURS, String.valueOf(hours));
    }

    private NameValuePair dbname(String dbname) {
        return new NameValuePair(DBNAME, dbname);
    }

    /**
     * Helper method which delegates the actual construction of the request method so
     * that it can be re constructed with new ticket when authentication fails due to
     * the ticket being expired.
     * 
     * @author cristian.baciu
     *
     */
    interface RequestBuilder {
        /**
         * Builds a new request method using the current authenticated ticket.
         * 
         * @return A HTTP request method build with the current authenticated ticket.
         * @throws QuickBaseException In case the request cannot be build for some reason.
         */
        public HttpMethod getRequestMethod() throws QuickBaseException;
    }
    
}
