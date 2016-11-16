package com.x.cloudhub;

import java.util.Arrays;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ChooseServiceActivity extends Activity{

	 public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.list_of_services);
	    final ListView services = (ListView) findViewById(R.id.services);
	    services.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
	   		 public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long id) {
	   			 String[] cloud=getResources().getStringArray(R.array.cloud);
	   			 String[] webdav=getResources().getStringArray(R.array.webdav);
	   			 Context c = ChooseServiceActivity.this;
	   			 String chosen_service = services.getItemAtPosition(position).toString();
	   			 int chosen_service_id = utils.getStrId(chosen_service, c);
	   			 setTitle(chosen_service);
	   			 if(Arrays.asList(cloud).contains(chosen_service) && Arrays.asList(webdav).contains(chosen_service)){
	   				 startActivity(new Intent(c, ChooseMethodActivity.class).putExtra("service_id", chosen_service_id));
	   			 }
	   			 else if(Arrays.asList(cloud).contains(chosen_service)){
	   				Intent i=new Intent(ChooseServiceActivity.this, SetupActivity.class);
	   				 Bundle para=new Bundle();
	   				 para.putInt("service_id", chosen_service_id); 
	   				 para.putString("method", "OAuth2");
	   				 startActivity(i.putExtras(para));
	   			 }else if(chosen_service_id==R.string.ftp){
	   				Intent i=new Intent(ChooseServiceActivity.this, SetupActivity.class);
	   				 Bundle para=new Bundle();
	   				 para.putInt("service_id", R.string.ftp); 
	   				 para.putString("method", "FTP");
	   				 startActivity(i.putExtras(para));
	   			}else if(chosen_service_id==R.string.webdav){
	   				 Intent wdav=new Intent(ChooseServiceActivity.this, SetupActivity.class);
	   				 Bundle para=new Bundle();
	   				 para.putInt("service_id", chosen_service_id); 
	   				 para.putString("method", "WebDAV");
	   				 para.putInt("url_id", R.string.url_none);
	   				 startActivity(wdav.putExtras(para));
	   			}else{
	   				setContentView(utils.getProtoLayout(chosen_service_id));
	   			}
	   		 }	 
	   	});
	    
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
