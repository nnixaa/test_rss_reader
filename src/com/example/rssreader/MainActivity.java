package com.example.rssreader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.example.rssreader.adapters.RssAdapter;
import com.example.rssreader.helpers.RssParserHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import static com.example.rssreader.utils.Utils.internetAvailable;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String RSS_URL = "http://habrahabr.ru/rss/best/";

    private PullToRefresh listView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        // defines onClick method
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap item = (HashMap) listView.getAdapter().getItem(position);

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) item.get("link")));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        listView.setVisibility(View.GONE);
        findViewById(R.id.progress).setVisibility(View.VISIBLE);;
        loadFromNetwork();
    }

    protected void loadFromNetwork() {
        if (internetAvailable(MainActivity.this)) {
            LoaderFromNetwork loader = new LoaderFromNetwork();
            loader.execute(RSS_URL);
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

                // gets source
                InputSource inputSource = new InputSource(new InputStreamReader(response.getEntity().getContent()));
                // parse xml
                result = RssParserHelper.parse(inputSource);

            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList result) {
            ((RssAdapter) listView.getAdapter()).setAll(result);
            listView.onRefreshComplete();
            findViewById(R.id.progress).setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

}
