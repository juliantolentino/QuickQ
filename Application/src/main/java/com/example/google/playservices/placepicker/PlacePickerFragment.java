/*
* Copyright 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.google.playservices.placepicker;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.ui.PlacePicker;

import com.example.google.playservices.placepicker.cardstream.Card;
import com.example.google.playservices.placepicker.cardstream.CardStream;
import com.example.google.playservices.placepicker.cardstream.CardStreamFragment;
import com.example.google.playservices.placepicker.cardstream.OnCardClickListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sample demonstrating the use of {@link PlacePicker}.
 * This sample shows the construction of an {@link Intent} to open the PlacePicker from the
 * Google Places API for Android and select a {@link Place}.
 *
 * This sample uses the CardStream sample template to create the UI for this demo, which is not
 * required to use the PlacePicker API. (Please see the Readme-CardStream.txt file for details.)
 *
 * @see com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder
 * @see com.google.android.gms.location.places.ui.PlacePicker
 * @see com.google.android.gms.location.places.Place
 */
public class PlacePickerFragment extends Fragment implements OnCardClickListener {

    Context context;

    static Place globalplace;

    private static final String TAG = "PlacePickerSample";

    private CardStreamFragment mCards = null;

    // Buffer used to display list of place types for a place
    private final StringBuffer mPlaceTypeDisplayBuffer = new StringBuffer();

    // Tags for cards
    private static final String CARD_INTRO = "INTRO";
    private static final String CARD_PICKER = "PICKER";
    private static final String CARD_DETAIL = "DETAIL";

    /**
     * Action to launch the PlacePicker from a card. Identifies the card action.
     */
    private static final int ACTION_PICK_PLACE = 1;
    private static final int ACTION_REPORT_WAIT = 2;

    /**
     * Request code passed to the PlacePicker intent to identify its result when it returns.
     */
    private static final int REQUEST_PLACE_PICKER = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if cards are visible, at least the picker card is always shown.
        CardStreamFragment stream = getCardStream();
        if (stream.getVisibleCardCount() < 1) {
            // No cards are visible, sample is started for the first time.
            // Prepare all cards and show the intro card.
            initialiseCards();
            // Show the picker card and make it non-dismissible.
            getCardStream().showCard(CARD_PICKER, false);
        }

    }

    // AsyncTask

    public class powergridServerRestaurants extends AsyncTask<Object, Void, String> {
        HttpPost httppost;
        Place place;

        @Override
        protected String doInBackground(Object... params) {
            HttpResponse response = null;
            InputStream inputStream = null;
            String result = null;
            httppost = (HttpPost) params[0];
            place = (Place) params[1];
            globalplace = place;

            try {
                HttpClient client = new DefaultHttpClient(new BasicHttpParams());
                response = client.execute(httppost);
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();
                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                result = sb.toString();


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            finally {
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }

            return result;

        }

        @Override
        protected void onPostExecute(String result) {
            //Do something with result
            if (result != null) {
                try {
                    JSONObject jObject = new JSONObject(result);
                    JSONArray jArray = jObject.getJSONArray("RESTAURANT");
                    try {
                        JSONObject oneObject = jArray.getJSONObject(0);
                        // Pulling items from the array
                        String placeid = oneObject.getString("placeid");
                        String datevisited = oneObject.getString("datevisited");
                        int waittime = oneObject.getInt("waittime");
                            /* A Place object contains details about that place, such as its name, address
                               and phone number. Extract the name, address, phone number, place ID and place types.
                             */
                        final CharSequence name = place.getName();
                        final CharSequence address = place.getAddress();
                        final CharSequence phone = place.getPhoneNumber();
                        final String placeId = place.getId();

                        // Update data on card.
                        getCardStream().getCard(CARD_DETAIL)
                                .setTitle(name.toString())
                                .setDescription(getString(R.string.detail_text, address, phone, waittime, datevisited));

                        // Print data to debug log
                        Log.d(TAG, "Place selected: " + placeId + " (" + name.toString() + ")");

                        // Show the card.
                        getCardStream().showCard(CARD_DETAIL);
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                        // Try displaying the card anyway
                        /* A Place object contains details about that place, such as its name, address
                           and phone number. Extract the name, address, phone number, place ID and place types.
                        */
                        final CharSequence name = place.getName();
                        final CharSequence address = place.getAddress();
                        final CharSequence phone = place.getPhoneNumber();
                        final String placeId = place.getId();

                        // Update data on card.
                        getCardStream().getCard(CARD_DETAIL)
                                .setTitle(name.toString())
                                .setDescription(getString(R.string.detail_text, address, phone, "Unknown", "No information"));

                        // Print data to debug log
                        Log.d(TAG, "Place selected: " + placeId + " (" + name.toString() + ")");

                        // Show the card.
                        getCardStream().showCard(CARD_DETAIL);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    // AsyncTask for reporting times

    public class powergridServerReportTime extends AsyncTask<HttpPost, Void, String> {
        HttpPost httppost;

        @Override
        protected String doInBackground(HttpPost... params) {
            HttpResponse response = null;
            InputStream inputStream = null;
            String result = null;
            httppost = params[0];

            try {
                HttpClient client = new DefaultHttpClient(new BasicHttpParams());
                response = client.execute(httppost);
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();
                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                result = sb.toString();


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            finally {
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }

            return result;

        }

        @Override
        protected void onPostExecute(String result) {
            //Do something with result
            if (result != null) {
                try {
                    JSONObject jObject = new JSONObject(result);
                    JSONArray jArray = jObject.getJSONArray("RESTAURANTPOST");
                    try {
                        JSONObject oneObject = jArray.getJSONObject(0);
                        // Pulling items from the array
                        boolean valid = oneObject.getBoolean("valid");
                        // TODO: Notify the user
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onCardClick(int cardActionId, String cardTag) {
        if (cardActionId == ACTION_PICK_PLACE) {
            // BEGIN_INCLUDE(intent)
            /* Use the PlacePicker Builder to construct an Intent.
            Note: This sample demonstrates a basic use case.
            The PlacePicker Builder supports additional properties such as search bounds.
             */
            try {
                PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(getActivity());
                // Start the Intent by requesting a result, identified by a request code.
                startActivityForResult(intent, REQUEST_PLACE_PICKER);

                // Hide the pick option in the UI to prevent users from starting the picker
                // multiple times.
                showPickAction(false);

            } catch (GooglePlayServicesRepairableException e) {
                GooglePlayServicesUtil
                        .getErrorDialog(e.getConnectionStatusCode(), getActivity(), 0);
            } catch (GooglePlayServicesNotAvailableException e) {
                Toast.makeText(getActivity(), "Google Play Services is not available.",
                        Toast.LENGTH_LONG)
                        .show();
            }

            // END_INCLUDE(intent)
        } else if (cardActionId == ACTION_REPORT_WAIT) {
            final Dialog dialog = new Dialog(this.getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.report);

            final NumberPicker np = (NumberPicker) dialog.findViewById(R.id.numpicker);
            np.setMaxValue(120);
            np.setMinValue(0);

            // Report
            Button dialogButtonReport = (Button) dialog.findViewById(R.id.dialogButtonReport);
            dialogButtonReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HttpPost httppost = new HttpPost("http://powergrid.xyz/quickq/restaurantpost.php");
                    httppost.setHeader("Content-type", "application/x-www-form-urlencoded");
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    // TODO
                    nameValuePairs.add(new BasicNameValuePair("placeid", globalplace.getId()));
                    nameValuePairs.add(new BasicNameValuePair("waittime", String.valueOf(np.getValue())));
                    try {
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    powergridServerReportTime task = new powergridServerReportTime();
                    task.execute(httppost);

                    dialog.dismiss();
                }
            });

            // Cancel
            Button dialogButtonCancel = (Button) dialog.findViewById(R.id.dialogButtonCancel);
            // If button is clicked, close the custom dialog
            dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }
    }

    /**
     * Extracts data from PlacePicker result.
     * This method is called when an Intent has been started by calling
     * {@link #startActivityForResult(android.content.Intent, int)}. The Intent for the
     * {@link com.google.android.gms.location.places.ui.PlacePicker} is started with
     * {@link #REQUEST_PLACE_PICKER} request code. When a result with this request code is received
     * in this method, its data is extracted by converting the Intent data to a {@link Place}
     * through the
     * {@link com.google.android.gms.location.places.ui.PlacePicker#getPlace(android.content.Intent,
     * android.content.Context)} call.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // BEGIN_INCLUDE(activity_result)
        if (requestCode == REQUEST_PLACE_PICKER) {
            // This result is from the PlacePicker dialog.

            // Enable the picker option
            showPickAction(true);

            if (resultCode == Activity.RESULT_OK) {
                /* User has picked a place, extract data.
                   Data is extracted from the returned intent by retrieving a Place object from
                   the PlacePicker.
                 */
                final Place place = PlacePicker.getPlace(data, getActivity());
                if (place.getPlaceTypes().contains(place.TYPE_RESTAURANT)) {
                    HttpPost httppost = new HttpPost("http://powergrid.xyz/quickq/restaurantget.php");
                    httppost.setHeader("Content-type", "application/x-www-form-urlencoded");
                    List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                    nameValuePairs.add(new BasicNameValuePair("placeid", place.getId()));
                    try {
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    powergridServerRestaurants task = new powergridServerRestaurants();
                    task.execute(httppost,place);
                } else {
                    Toast.makeText(context,"You must select a restaurant.", Toast.LENGTH_SHORT).show();
                    getCardStream().hideCard(CARD_DETAIL);
                }

            } else {
                // User has not selected a place, hide the card.
                getCardStream().hideCard(CARD_DETAIL);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        // END_INCLUDE(activity_result)
    }

    /**
     * Initializes the picker and detail cards and adds them to the card stream.
     */
    private void initialiseCards() {
        // Add picker card.
        Card c = new Card.Builder(this, CARD_PICKER)
                .setTitle(getString(R.string.pick_title))
                .setDescription(getString(R.string.pick_text))
                .addAction(getString(R.string.pick_action), ACTION_PICK_PLACE, Card.ACTION_NEUTRAL)
                .setLayout(R.layout.card_google)
                .build(getActivity());
        getCardStream().addCard(c, false);

        // Add detail card.
        c = new Card.Builder(this, CARD_DETAIL)
                .setTitle(getString(R.string.empty))
                .setDescription(getString(R.string.empty))
                .addAction(getString(R.string.report_action), ACTION_REPORT_WAIT, Card.ACTION_NEUTRAL)
                .build(getActivity());
        getCardStream().addCard(c, false);

        // Add and show introduction card.
        c = new Card.Builder(this, CARD_INTRO)
                .setTitle(getString(R.string.intro_title))
                .setDescription(getString(R.string.intro_message))
                .build(getActivity());
        getCardStream().addCard(c, true);
    }

    /**
     * Sets the visibility of the 'Pick Action' option on the 'Pick a place' card.
     * The action should be hidden when the PlacePicker Intent has been fired to prevent it from
     * being launched multiple times simultaneously.
     * @param show
     */
    private void showPickAction(boolean show){
        mCards.getCard(CARD_PICKER).setActionVisibility(ACTION_PICK_PLACE, show);
    }

    /**
     * Returns the CardStream.
     * @return
     */
    private CardStreamFragment getCardStream() {
        if (mCards == null) {
            mCards = ((CardStream) getActivity()).getCardStream();
        }
        return mCards;
    }

}
