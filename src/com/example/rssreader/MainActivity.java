package com.example.rssreader;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.rssreader.adapters.RssAdapter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderAdapter;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import static com.example.rssreader.utils.Utils.internetAvailable;

public class MainActivity extends Activity
{
    private static final String TAG = "MainActivity";

    private PullToRefresh listView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // gets a list view
        listView = (PullToRefresh) findViewById(R.id.list_view);

        // sets an empty adapter
        ArrayList<HashMap> entities = new ArrayList<HashMap>();
        RssAdapter adapter = new RssAdapter(MainActivity.this, entities, R.layout.rss_item);
        listView.setAdapter(adapter);

        // defines onRefresh method
        listView.setOnRefreshListener(new PullToRefresh.OnRefreshListener() {
            public void onRefresh() {
                loadFromNetwork();
            }
        });

        loadFromNetwork();
    }

    protected void loadFromNetwork() {
        if (internetAvailable(MainActivity.this)) {
            LoaderFromNetwork loader = new LoaderFromNetwork();
            loader.execute("http://habrahabr.ru/rss/best/");
        } else {
            Toast.makeText(MainActivity.this, getString(R.string.shared_no_connection), Toast.LENGTH_SHORT).show();
            listView.onRefreshComplete();
        }
    }

    // ----------------------------- //

    private class LoaderFromNetwork extends AsyncTask<String, ArrayList, ArrayList> {

        @Override
        protected ArrayList doInBackground(String... params) {
            ArrayList<HashMap> result = new ArrayList<HashMap>();
            DefaultHttpClient httpClient = new DefaultHttpClient();
            String url = params[0];

            try {
                HttpGet request = new HttpGet(url);
                HttpResponse response = httpClient.execute(request);

                XPath xpath = XPathFactory.newInstance().newXPath();
                InputSource inputSource = new InputSource(new InputStreamReader(response.getEntity().getContent()));

                NodeList nodes = (NodeList) xpath.evaluate("//channel/item", inputSource, XPathConstants.NODESET);
                Log.i(TAG, ((Integer) nodes.getLength()).toString());

                int index = 0;
                while (index < nodes.getLength()) {
                    HashMap hash = new HashMap();

                    NodeList items = nodes.item(index).getChildNodes();
                    String title = items.item(1).getTextContent();

                    if (title.length() > 0) {
                        hash.put("title", title);
                        result.add(hash);
                    }
                    index++;
                }


            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } catch (XPathExpressionException e) {
                Log.e(TAG, e.toString());
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList result) {
            ((RssAdapter) listView.getAdapter()).setAll(result);
            listView.onRefreshComplete();
        }
    }

}
