/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.marcelo.restodroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String forecast = mForecastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        weatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numResto)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
//            final String OWM_LIST = "list";
//            final String OWM_WEATHER = "weather";
//            final String OWM_TEMPERATURE = "temp";
//            final String OWM_MAX = "max";
//            final String OWM_MIN = "min";
//            final String OWM_DATETIME = "dt";
//            final String OWM_DESCRIPTION = "main";

            final String OWM_LIST = "objetsTouristiques";
            final String OWM_NOM_RESTO = "nom";
            final String OWM_NOMFR_RESTO = "libelleFr";
            final String OWM_LOCALISATION = "localisation";
            final String OWM_ADRESSE = "adresse";
            final String OWM_ADRESSE_RESTO = "adresse1";
            final String OWM_COMMUNE = "commune";
            final String OWM_NOM_COMMUNE = "nom";


            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray restoArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numResto];
            for (int i = 0; i < restoArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String nomResto;
                String adresse;
                String nomCommune;


                // On prendle  JSON object qui représente le restorant
                JSONObject restoForecast = restoArray.getJSONObject(i);

                //on récupère le nom du resto
                JSONObject nomRestoObject = restoForecast.getJSONObject(OWM_NOM_RESTO);
                nomResto = nomRestoObject.getString(OWM_NOMFR_RESTO);


                //on récupère l'adresse

                JSONObject adresseObject = restoForecast.getJSONObject(OWM_LOCALISATION).getJSONObject(OWM_ADRESSE);
                adresse = adresseObject.getString(OWM_ADRESSE_RESTO);
                JSONObject communeObject = adresseObject.getJSONObject(OWM_COMMUNE);
                nomCommune = communeObject.getString(OWM_NOM_COMMUNE);


                resultStrs[i] = nomResto + " - " + adresse + " - " + nomCommune;
            }
                return resultStrs;

            }
            @Override
            protected String[] doInBackground (String...params){

                // If there's no zip code, there's nothing to look up.  Verify size of params.
                if (params.length == 0) {
                    return null;
                }

                // These two need to be declared outside the try/catch
                // so that they can be closed in the finally block.
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.
                String forecastJsonStr = null;

                String format = "json";
                String units = "metric";
                int nbResto = 20;

                try {
                    final String FORECAST_BASE_URL =
                            "http://api.sitra-tourisme.com/api/v002/recherche/list-objets-touristiques?";

                    final String PROJET_ID = "projetId";
                    final String QUERY = "query";
                    final String API_KEY = "apiKey";
                    final String CRITERES_QUERY = "criteresQuery";
                    final String NB_RESTO = "count";
                    final String rq = "{\"projetId\":\"1143\",\"apiKey\":\"m4VH2Zee\",\"criteresQuery\":\"type:RESTAURATION\"}";
                    final String QUERY_PARAM = "q";


                    Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                            .appendQueryParameter(QUERY, rq)
                            .build();
                    URL url = new URL(builtUri.toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.connect();


                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    forecastJsonStr = buffer.toString();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attemping
                    // to parse it.
                    return null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }
                }

                try {
                    return getWeatherDataFromJson(forecastJsonStr, nbResto);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }

                // This will only happen if there was an error getting or parsing the forecast.
                return null;
            }

            @Override
            protected void onPostExecute (String[]result){
                if (result != null) {
                    mForecastAdapter.clear();
                    for (String dayForecastStr : result) {
                        mForecastAdapter.add(dayForecastStr);
                    }
                    // New data is back from the server.  Hooray!
                }
            }
        }
    }

