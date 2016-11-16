package com.x.cloudhub;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class UserInfoActivity extends Activity{
	Services s=new Services(this);
	String url="";int service_id; String defined_name=""; 
	String method=""; String credentials = ""; String email;
	 public void onCreate(Bundle savedInstanceState) {
	     super.onCreate(savedInstanceState);
	     Bundle extras = this.getIntent().getExtras();
	     service_id = extras.getInt("service_id");
	     method = extras.getString("method");
	     email = extras.getString("email");
	     credentials = extras.getString("credentials");
	     url = extras.getString("url");
	     FileAccessActivity.expires = extras.getLong("expires");
	     setContentView(R.layout.user_info);
	     TextView baseText = (TextView) findViewById(R.id.baseText);
	     baseText.setText(s.new Service(method,service_id).getDetails(url,email,credentials));
	 }
	 public void btnAccessFileSystem(View v){
		
		 defined_name = ((EditText) findViewById(R.id.name)).getText().toString();
		 
		 SQLiteDatabase myDB = this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
	      myDB.execSQL("CREATE TABLE IF NOT EXISTS ACCOUNTS (" +
	      		"NAME TEXT," +
	      		"METHOD VARCHAR(15),"+
	      		"SERVICE VARCHAR(30)," +
	      		"URL TEXT," +
	      		"EMAIL TEXT,"+
	      		"CREDENTIALS TEXT,"+
	      		"EXPIRES INTEGER"+
	      		")");
	      ContentValues values = new ContentValues();
	      
	      int duplicates = myDB.query("ACCOUNTS",null,"NAME='"+defined_name+"'",null,null,null,null).getCount();
	      if(duplicates!=0){
	    	  TextView name_conflict = (TextView) findViewById(R.id.name_conflict);
	    	  name_conflict.setVisibility(View.VISIBLE);
	    	  return;
	      }  
	      values.put("NAME",defined_name);
	      values.put("METHOD",method);
	      values.put("SERVICE",getResources().getString(service_id));
	      values.put("URL",url);
	      values.put("EMAIL", email);
	      values.put("CREDENTIALS", credentials);
	      values.put("EXPIRES", FileAccessActivity.expires);
	      myDB.insert("ACCOUNTS",null,values);
	      myDB.close();
	      
	     Intent acc=new Intent(UserInfoActivity.this, FileAccessActivity.class);
	     Bundle para=new Bundle();
	 
		 para.putInt("service_id", service_id);
		 para.putString("name", defined_name);
		 para.putString("method", method);
		 para.putString("url", url);
		 para.putString("email", email);
		 para.putString("credentials",credentials);
		 para.putLong("expires",FileAccessActivity.expires);
		 startActivity(acc.putExtras(para));   
	 }
}
