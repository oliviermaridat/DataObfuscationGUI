package com.societies.privacy;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.societies.data.Geolocation;
import com.societies.privacy.data.ObfuscationType;
import com.societies.privacy.data.ObfuscationTypes;
import com.societies.privacy.obfuscation.DataObfuscationManager;
import com.societies.privacy.obfuscation.IDataObfuscationManagerCallback;
import com.societies.privacy.obfuscation.obfuscator.GeolocationObfuscator;
import com.societies.utils.GeolocationUtils;

public class LocationObfuscationActivity extends Activity implements OnItemSelectedListener, OnClickListener, OnSeekBarChangeListener {
	private String TAG = "LocationObfuscationActivity";
	
	private TextView txtGeolocation;
	private TextView txtObfuscationLevel;
	private TextView txtObfuscationResults;
	private SeekBar seekBarObfuscationLevel;
	private Spinner spinnerMode;
	private Button btnObfuscate;
	
	private ProgressBar progressBar;
	private TextView txtProgressBar;
	private View loadingPanel;
	private LocationTask locationTask;
	private ObfuscationTask obfuscationTask;
	
	private Geolocation  geolocation;
	private Geolocation  obfuscatedGeolocation;
	
	private LocationManager locationManager;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
     // Chargement éléments
        txtGeolocation = (TextView) findViewById(R.id.txtGeolocation);        
        txtObfuscationLevel = (TextView) findViewById(R.id.txtObfuscationLevel);        
        txtObfuscationResults = (TextView) findViewById(R.id.txtObfuscationResults);
        seekBarObfuscationLevel = (SeekBar) findViewById(R.id.seekBarObfuscationLevel);
        spinnerMode = (Spinner) findViewById(R.id.spinnerMode);
        btnObfuscate = (Button) findViewById(R.id.btnObfuscate);
        
        // Chargement spinner chosen mode
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.arrayMode, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(adapter1);
        spinnerMode.setSelection(6);
        spinnerMode.setOnItemSelectedListener(this);
        
        // Chargement seekbar
        int initialObfuscationLevel = 50;
        seekBarObfuscationLevel.setMax(100);
        seekBarObfuscationLevel.setProgress(50);
        txtObfuscationLevel.setText(initialObfuscationLevel+"%");
        seekBarObfuscationLevel.setOnSeekBarChangeListener(this);
        
        // Bouton refresh geolocation
        Button btnLocate = (Button) findViewById(R.id.btnLocate);
        btnLocate.setOnClickListener(this);
        
        // Bouton obfuscation
        btnObfuscate.setEnabled(false);
        btnObfuscate.setOnClickListener(this);
    }
    
	@Override
	public void onClick(View v) {
		switch (v.getId())
		{
			case R.id.btnLocate:
			{
				// --- Data
				
				// --- Action
				locationTask = new LocationTask();
				locationTask.execute();
				break;
			}
			case R.id.btnObfuscate:
			{
				// --- Data
				
				// --- Action
				obfuscationTask = new ObfuscationTask();
				obfuscationTask.execute();
				break;
			}
		}
	}

	private class ObfuscationTask extends AsyncTask<Integer, Object, Long> implements IDataObfuscationManagerCallback<Object> {
		Exception exception;
		
		@Override
		protected void onPreExecute() {
			exception = null;
			
			if (loadingPanel == null) {
				loadingPanel = ((ViewStub) findViewById(R.id.stubWaiting)).inflate();
				progressBar = (ProgressBar) loadingPanel.findViewById(R.id.progressBar);
				txtProgressBar = (TextView) loadingPanel.findViewById(R.id.txtProgressBar);
                final View cancelButton = loadingPanel.findViewById(R.id.button_cancel);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                    	onCancelObfuscationTask();
                    }
                });
            }
			publishProgress(0, "Loading...");
            showPanel(loadingPanel, false);
		}
		
		@Override
		protected Long doInBackground(Integer... params) {
			int progress = 0;
			
			// Check availability of a geolocation
			if (null == geolocation) {
				exception = new Exception("No location to obfuscate.");
				return (long) 0;
			}
			// Create copy of geolocation
			obfuscatedGeolocation = new Geolocation(geolocation.getLatitude(), geolocation.getLongitude(), geolocation.getHorizontalAccuracy());
			
			// Init
			publishProgress(progress, "Initialization");
			DataObfuscationManager dataObfuscationManager = new DataObfuscationManager();
			Log.i(TAG, ((Float) (((Integer) seekBarObfuscationLevel.getProgress()).floatValue()/(float) 100)).toString());
			obfuscatedGeolocation.setObfuscationLevel(((Integer) seekBarObfuscationLevel.getProgress()).floatValue()/(float) 100);
			int mode = spinnerMode.getSelectedItemPosition();
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("data", obfuscatedGeolocation);
			if (6 != mode) {
				data.put("obfuscationOperation", mode);
			}
	        ObfuscationType obfuscationType = new ObfuscationType(ObfuscationTypes.GEOLOCATION);
			progress += 5;
			publishProgress(progress);
			if (isCancelled()) {
				return (long) 0;
			}
	        
			// Obfuscation
	    	try {
	    		publishProgress(progress, "Obfuscation");
	    		long start = System.currentTimeMillis();
	    		
				dataObfuscationManager.obfuscateData(data, obfuscationType, obfuscatedGeolocation.getObfuscationLevel(), this);
				
				long duree = System.currentTimeMillis() - start;
				System.out.println("Durée : "+duree+"ms");
		    	progress += 90;
				publishProgress(progress);
				if (isCancelled()) {
					return (long) 0;
				}
				return duree;
			} catch (Exception e) {
				publishProgress(98);
				exception = e;
			}
			return (long) 0;
		}

		@Override
		protected void onProgressUpdate(Object... progress) {
			final ProgressBar progressBarLocal = progressBar;
			final TextView txtProgressBarLocal = txtProgressBar;

			if (progress.length == 2) {
				txtProgressBarLocal.setText((CharSequence) progress[1]);
			}	
			progressBarLocal.setProgress((Integer) progress[0]);
		}

		@Override
		protected void onPostExecute(Long response) {
			// Erreur exception
			if (null != exception) {
				txtObfuscationResults.setText("Error: "+exception.getMessage());
				exception.printStackTrace();
			}
			// No exception
			else {
				// Succès
				if (0 != response) {
					txtObfuscationResults.setText(geolocationToString(obfuscatedGeolocation, response));
				}
				// Echec
				else {
					txtObfuscationResults.setText("Sorry, epic fail!");
				}
			}
			publishProgress(100);
			hidePanel(loadingPanel, false);
		}

		@Override
		public void onCancelled() {
			txtProgressBar.setText("Canceled");
			hidePanel(loadingPanel, false);
		}

		@Override
		public void obfuscationResult(Object obfuscatedData) {
			obfuscatedGeolocation = (Geolocation) obfuscatedData;
			obfuscatedGeolocation.setObfuscationLevel(((Integer) seekBarObfuscationLevel.getProgress()).floatValue()/(float) 100);
		}
		
		@Override
		public void cancel(String msg) {
			txtObfuscationResults.setText(msg);
			onCancelled();
		}
	}
	
	private class LocationTask extends AsyncTask<Integer, Object, Boolean> implements LocationListener {
		Exception exception;
		
		@Override
		protected void onPreExecute() {
			exception = null;
			
			if (loadingPanel == null) {
				loadingPanel = ((ViewStub) findViewById(R.id.stubWaiting)).inflate();
				progressBar = (ProgressBar) loadingPanel.findViewById(R.id.progressBar);
				txtProgressBar = (TextView) loadingPanel.findViewById(R.id.txtProgressBar);
                final View cancelButton = loadingPanel.findViewById(R.id.button_cancel);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                    	onCancelLocationTask();
                    }
                });
            }
			publishProgress(0, "Loading...");
            showPanel(loadingPanel, false);
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			int progress = 5;
			
			publishProgress(progress, "Wainting for geolocation");
			locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//				Looper.prepare();
//				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
//				Looper.loop();
				geolocation = new Geolocation(48.856666, 2.350987, 542);
	        }
			else {
				progress += 90;
				publishProgress(progress, "No GPS data");
				return false;
			}
			progress += 90;
			publishProgress(progress);
			if (isCancelled()) {
				return false;
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(Object... progress) {
			final ProgressBar progressBarLocal = progressBar;
			final TextView txtProgressBarLocal = txtProgressBar;

			if (progress.length == 2) {
				txtProgressBarLocal.setText((CharSequence) progress[1]);
			}	
			progressBarLocal.setProgress((Integer) progress[0]);
		}

		@Override
		protected void onPostExecute(Boolean response) {
			// Erreur exception
			if (null != exception) {
				txtGeolocation.setText("Error: "+exception.getMessage());
			}
			// Succès
			else {
				btnObfuscate.setEnabled(true);
				txtGeolocation.setText(geolocationToString(geolocation));
			}
			publishProgress(100);
			hidePanel(loadingPanel, false);
		}

		@Override
		public void onCancelled() {
			txtProgressBar.setText("Canceled");
			hidePanel(loadingPanel, false);
		}

		@Override
		public void onLocationChanged(Location location) {
			geolocation = new Geolocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
			// Stop listening for location updates
			locationManager.removeUpdates(this);
			Looper.myLooper().quit();
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	private void onCancelObfuscationTask() {
		if (null != obfuscationTask && obfuscationTask.getStatus() == AsyncTask.Status.RUNNING) {
			obfuscationTask.cancel(true);
			obfuscationTask = null;
		}
	}
	
	private void onCancelLocationTask() {
		if (null != locationTask && locationTask.getStatus() == AsyncTask.Status.RUNNING) {
			locationTask.cancel(true);
			locationTask = null;
		}
	}

	private void showPanel(View panel, boolean slideUp) {
		panel.startAnimation(AnimationUtils.loadAnimation(this,
				slideUp ? R.anim.slide_in : R.anim.slide_out_top));
		panel.setVisibility(View.VISIBLE);
	}

	private void hidePanel(View panel, boolean slideDown) {
		panel.startAnimation(AnimationUtils.loadAnimation(this,
				slideDown ? R.anim.slide_out : R.anim.slide_in_top));
		panel.setVisibility(View.GONE);
	}
	
	public String geolocationToString(Geolocation geolocation) {
		return geolocationToString(geolocation, 0);
	}
	public String geolocationToString(Geolocation geolocation, long duree) {
		String algorithm = "";
		if (-1 != geolocation.getObfuscationAlgorithm()) {
			switch(geolocation.getObfuscationAlgorithm()) {
			case GeolocationObfuscator.OPERATION_E:
				algorithm = "E operation";
				break;
			case GeolocationObfuscator.OPERATION_R:
				algorithm = "R operation";
				break;
			case GeolocationObfuscator.OPERATION_S:
				algorithm = "S operation";
				break;
			case GeolocationObfuscator.OPERATION_ES:
				algorithm = "ES operation";
				break;
			case GeolocationObfuscator.OPERATION_SE:
				algorithm = "SE operation";
				break;
			case GeolocationObfuscator.OPERATION_SR:
				algorithm = "SR operation";
				break;
			}
		}
		String obfuscationLevel = "";
		if (0 != geolocation.getObfuscationLevel()) {
			obfuscationLevel = "to "+(geolocation.getObfuscationLevel()*100)+"%";
		}
		return "Geolocation"+("" != algorithm && "" != obfuscationLevel ? " ("+algorithm+" "+obfuscationLevel+(0 != duree ? " in "+duree+"ms" : "")+")" : "")+":\n"+
				"<"+GeolocationUtils.floor(geolocation.getLatitude(), 6)+", "+GeolocationUtils.floor(geolocation.getLongitude(), 6)+">\n"+
				"Accuracy: "+geolocation.getHorizontalAccuracy();
	}
	
	@Override
    public void onDestroy() {
		super.onDestroy();
		onCancelObfuscationTask();
    }

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		txtObfuscationLevel.setText(progress+"%");
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
	}

}