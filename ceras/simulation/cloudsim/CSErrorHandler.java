	package ceras.simulation.cloudsim;

	import org.xml.sax.ErrorHandler;
	import org.w3c.dom.Attr;
	import org.w3c.dom.Document;
	import org.w3c.dom.NamedNodeMap;
	import org.w3c.dom.Node;
	import org.w3c.dom.NodeList;

	import org.xml.sax.ErrorHandler;
	import org.xml.sax.SAXException;
	import org.xml.sax.SAXParseException;
	import org.xml.sax.SAXNotRecognizedException;
	import org.xml.sax.SAXNotSupportedException;

	import org.w3c.dom.Element;
	import org.apache.xerces.parsers.*;
	import org.apache.xerces.dom.TextImpl;
	
public class CSErrorHandler implements ErrorHandler {
	
    //
    // ErrorHandler methods
    //

    /** Warning. */
    public void warning(SAXParseException ex) {
        System.out.println("[Warning] "+
                           getLocationString(ex)+": "+
                           ex.getMessage());
    }

    /** Error. */
    public void error(SAXParseException ex) {
        System.out.println("[Error] "+
                           getLocationString(ex)+": "+
                           ex.getMessage());
                              
    }

    /** Fatal error. */
    public void fatalError(SAXParseException ex) throws SAXException {
        System.out.println("[Fatal Error] "+
                           getLocationString(ex)+": "+
                           ex.getMessage());
        throw new SAXException("[Fatal Error] "+
                           getLocationString(ex)+": "+
                           ex.getMessage());
    }

    //
    // Private methods
    //

    /** Returns a string of the location. */
    private String getLocationString(SAXParseException ex) {
        StringBuffer str = new StringBuffer();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1) 
                systemId = systemId.substring(index + 1);
            str.append(systemId);
        }
        str.append(':');
        str.append(ex.getLineNumber());
        str.append(':');
        str.append(ex.getColumnNumber());

        return str.toString();

    } // getLocationString(SAXParseException):String
	

}

