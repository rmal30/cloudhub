package com.x.cloudhub;

import java.net.Socket;
import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

public class DebugActivity extends Activity{
	Services s=new Services(this);
	int service_id; String name; String email;
	String baseurl="";String method="";String credentials; Long expires;
	TextView usrInfoText; Socket ftp_socket;ArrayList<String> items; ArrayList<String> item_ids;static ArrayAdapter<String> adapter2;
	Services.FTPSocket ftpsock;
	 public void onCreate(Bundle savedInstanceState) {
		
	     super.onCreate(savedInstanceState);
	     
		 Bundle extras = this.getIntent().getExtras();
		  service_id = extras.getInt("service_id");
		  name = extras.getString("name");
		  method = extras.getString("method");
		  baseurl = extras.getString("url");  
		  email = extras.getString("email");
		  credentials = extras.getString("credentials");
		  expires = extras.getLong("expires");
	        setContentView(R.layout.debug);
	        usrInfoText = (TextView) findViewById(R.id.usrInfoText);
	        setTitle(name);
	        TextView baseURL = (TextView) findViewById(R.id.baseURL);
	        baseURL.append("Base URL: "+baseurl+"\n");
	        if(service_id==R.string.ftp){
	        	Services.URL u = s.new URL(baseurl);
	        	String[] details = credentials.split(":"); 
	        	ftpsock =  s.new FTPSocket(u.host,8000,details[0],details[1],usrInfoText);
	        }
	    }
	 public void btnDoRequest(View view){
		 //items = new ArrayList<String>();
		 //item_ids = new ArrayList<String>();
		 EditText edit_path = (EditText) findViewById(R.id.edit_path);
		 EditText edit_cmd = (EditText) findViewById(R.id.edit_cmd);
		 Log.i("Method",method);
		 //String response;
		 ArrayList<String> headers = new ArrayList<String>();
		 Services.HTTPRequest req=null;
		 //Services.URL u = s.new URL(baseurl+edit_path.getText().toString());
		 Services.Service service = s.new Service(method, service_id);
		 headers.add(service.useAuthHeader(credentials));
		 if(method.equals("OAuth2")){
			 req=s.new HTTPRequest("GET",baseurl+edit_path.getText().toString(), headers,new byte[0]);
		 }else if(method.equals("WebDAV")){
			 headers.add("Depth: 1");
			 req = s.new HTTPRequest("PROPFIND",baseurl+edit_path.getText().toString(), headers,new byte[0]);
			 //response = s.new HTTPRequest("PROPFIND",baseurl+edit_path.getText().toString(), headers,"").getContent();
			 System.out.println(req.getHeaders().toString());
		 }else{
			ftpsock.runFTPcommand(edit_cmd.getText().toString(),usrInfoText);
			 //response = "ftp";	 
		 }
		 if(service_id!=R.string.ftp){usrInfoText.setText(req.getContent());}
	 }
	 public boolean onCreateOptionsMenu(Menu menu) {
	        getMenuInflater().inflate(R.menu.access, menu);
	        return true;
	    }
	 public boolean onOptionsItemSelected(MenuItem item) {
	    	switch (item.getItemId()) {
	        	case R.id.home:startActivity(new Intent(this, MainActivity.class)); return true;
	        	//case R.id.options:
	        		case R.id.details:
	        			String info;
	        			info = s.new Service(method, service_id).getDetails(baseurl,email, credentials);
	        	        new AlertDialog.Builder(this)
	        	        .setTitle("View details")
	        	        .setMessage(info)
	        	        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        	            public void onClick(DialogInterface dialog, int which){}})
	        	        .setIcon(android.R.drawable.ic_dialog_info)
	        	        .show();
	        	        return true;
	        		case R.id.rename:
	        		       final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        		       builder.setTitle("Rename "+name+" to:");
	        		       final EditText input = new EditText(this);
	           		       input.setInputType(InputType.TYPE_CLASS_TEXT);
	        		       builder.setView(input);
	        		       builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	        		           @Override
	        		           public void onClick(DialogInterface dialog, int which) {
	        		              String new_name = s.new ConfigureAccount().changeAccountName(name, input.getText().toString());
	        		              if(name.equals(new_name)){builder.setMessage("Cannot change name as a unique name must be used.");
	        		              }else{name=new_name;}
	        		           }
	        		       });
	        		       builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	        		           @Override
	        		           public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        		       });
	        		       builder.show();
	        		     return true;
	        		case R.id.disconnect:
	        			 new AlertDialog.Builder(this)
	        		        .setTitle("Disconnect service").setMessage("Are you sure you want to disconnect this service?")
	        		        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	        		            public void onClick(DialogInterface dialog, int which) { 
	        		            	s.new ConfigureAccount().disconnectAccount(name);
	        		            	startActivity(new Intent(DebugActivity.this, MainActivity.class)); return;
	        		            }
	        		         })
	        		        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	        		            public void onClick(DialogInterface dialog, int which) {dialog.cancel();}})
	        		        .setIcon(android.R.drawable.ic_dialog_alert)
	        		         .show();
	        			 return true;
	  	        	default: return super.onOptionsItemSelected(item);
	    	}
	 }
}

/*
WebDAV

PROPFIND - List directories and files
MOVE - Move and rename files and dirs
DELETE - Delete
COPY - Copy
MKCOL - Create dir
PUT - Upload/Modify file
GET - Download file
 */
