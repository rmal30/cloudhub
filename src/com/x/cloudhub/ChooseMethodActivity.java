package com.x.cloudhub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ChooseMethodActivity extends Activity{
	 private RadioGroup radioGroup;
	 private RadioButton api, webdav;
	 int service_id;
	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        service_id= this.getIntent().getIntExtra("service_id",0);
	        setTitle(getResources().getString(service_id));
	        setContentView(R.layout.choose_method);
	        radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
	        api = (RadioButton) findViewById(R.id.api);
	        webdav = (RadioButton) findViewById(R.id.webdav);

	    }
	 public void config_method(View v){
		 int selectedId = radioGroup.getCheckedRadioButtonId();
		 if(selectedId==api.getId()){
			 Intent i=new Intent(ChooseMethodActivity.this, SetupActivity.class);
			 Bundle para=new Bundle();
			 para.putInt("service_id", service_id);
			 para.putString("method", "OAuth2");
			 startActivity(i.putExtras(para));}
		 else if(selectedId==webdav.getId()){
			 Intent wdav=new Intent(ChooseMethodActivity.this, SetupActivity.class);
			 Bundle para=new Bundle();
			 para.putInt("service_id", service_id);
			 para.putString("method","WebDAV");
			 if(R.string.box==service_id){para.putInt("url_id",R.string.url_webdav_box);}
			 if(R.string.microsoft==service_id){para.putInt("url_id",R.string.url_webdav_microsoft);}
			 startActivity(wdav.putExtras(para));
		}
	 }
	 public void cancel(View v){
		 startActivity(new Intent(ChooseMethodActivity.this, MainActivity.class));
	 }
	 public boolean onCreateOptionsMenu(Menu menu) {
	        getMenuInflater().inflate(R.menu.setup, menu);
	        return true;
	    }
	 public boolean onOptionsItemSelected(MenuItem item) {
		    switch (item.getItemId()) {
		        case R.id.home:startActivity(new Intent(this, MainActivity.class)); return true;
		  	    default: return super.onOptionsItemSelected(item);
		    }
		 }
}
