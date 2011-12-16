package com.example.rssreader.helpers;

import android.util.Log;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Parse rss feeds and returns HashMap
 */
public class RssParserHelper {

    private static final String TAG = "RssParserHelper";

    private static final String EXPR_LIST           = "//channel/item";
    private static final String EXPR_TITLE          = "title";
    private static final String EXPR_DESCRIPTION    = "description";
    private static final String EXPR_DATE           = "pubDate";
    private static final String EXPR_LINK           = "link";

    /**
     * Parse xml input
     * @param input
     * @return ArrayList<HashMap>
     */
    public static ArrayList<HashMap> parse(InputSource input) {
        ArrayList<HashMap> result  = new ArrayList<HashMap>();

        try {

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(EXPR_LIST, input, XPathConstants.NODESET);

            int index = 0;
            while (index < nodes.getLength()) {
                HashMap hash = new HashMap();

                Node item = nodes.item(index);

                // finds necessary data
                Node title          = (Node) xpath.evaluate(EXPR_TITLE, item, XPathConstants.NODE);
                Node description    = (Node) xpath.evaluate(EXPR_DESCRIPTION, item, XPathConstants.NODE);
                Node date           = (Node) xpath.evaluate(EXPR_DATE, item, XPathConstants.NODE);
                Node link           = (Node) xpath.evaluate(EXPR_LINK, item, XPathConstants.NODE);

                hash.put("title", title.getTextContent());
                hash.put("description", description.getTextContent());
                hash.put("date", date.getTextContent());
                hash.put("link", link.getTextContent());

                result.add(hash);

                index++;
            }

        } catch (XPathExpressionException e) {
            Log.e(TAG, e.toString());
        }

        return result;
    }
}
