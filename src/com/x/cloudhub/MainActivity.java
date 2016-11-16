package com.x.cloudhub;
import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.ContextMenu;  
import android.view.ContextMenu.ContextMenuInfo;  

public class MainActivity extends Activity {
	String changed_name; String old_name;ProgressDialog dialog;
	Services s = new Services(this); 
	static AccountAdapter account_adapter; static SQLiteDatabase myDB; static ArrayList<Account> accounts;
	public class Account {
	    public String name,id,quota;
	    public int service_id;
	    public Account(String name, String id,String quota, int service_id) {
	       super();
	       this.name = name;
	       this.id = id;
	       this.quota = quota;
	       this.service_id = service_id;
	    }   
	}
	public static Account getAccountByName(String name, ArrayList<Account> accs){
    	for(int i=0; i<accs.size(); i++){
    		if(accs.get(i).name.equals(name)){
    			return accs.get(i);
    		}
    	}
    	return null;
    }
	public class AccountAdapter extends ArrayAdapter<Account> {
	    public AccountAdapter(Context context, ArrayList<Account> acc) {
	       super(context, 0, acc);
	       
	    }
	    public View getView(int position, View convertView, ViewGroup parent) {
	       Account acc = getItem(position);    
	       if (convertView == null) {
	          convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_accounts_view, parent, false);
	       }
	       TextView txtName = (TextView) convertView.findViewById(R.id.name);
	       TextView txtId = (TextView) convertView.findViewById(R.id.id);
	       TextView txtQuota = (TextView) convertView.findViewById(R.id.quota);
	       ImageView service = (ImageView) convertView.findViewById(R.id.service);
	       txtName.setText(acc.name);
	       txtId.setText(acc.id);
	       txtQuota.setText(acc.quota);
	       int drawable_id = R.drawable.ic_launcher;
	       switch(acc.service_id){
	       		case R.string.microsoft: drawable_id = R.drawable.microsoft; break;
	       		case R.string.google: drawable_id = R.drawable.google; break;
	       		case R.string.dropbox: drawable_id = R.drawable.dropbox; break; 
	       		case R.string.box: drawable_id = R.drawable.box; break; 
	       }
	       service.setImageDrawable(getResources().getDrawable(drawable_id));
	       return convertView;
	   }
	}
    public void onCreate(Bundle savedInstanceState) {
    	utils.clearWebViewDatabases(MainActivity.this);
    	accounts = new ArrayList<Account>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hub);
        myDB = this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS ACCOUNTS (NAME TEXT,METHOD VARCHAR(15),SERVICE VARCHAR(30),URL TEXT,EMAIL TEXT,CREDENTIALS TEXT, EXPIRES INTEGER)");
        int num_rows = myDB.rawQuery("SELECT * FROM ACCOUNTS",null).getCount();
        
        account_adapter = new AccountAdapter(this,accounts);
        Cursor selection = myDB.query("ACCOUNTS",new String[]{"NAME","METHOD","SERVICE","URL","EMAIL","CREDENTIALS"},null,null,null,null,null);
        for(int i=0; i<num_rows; i++){
        	selection.moveToPosition(i);
        	String method = selection.getString(1);
        	int service_id = utils.getStrId(selection.getString(2), MainActivity.this);
        	String url = selection.getString(3);
        	String email = selection.getString(4);
        	//String credentials = selection.getString(5);
        	String id="Unknown";
        	if(method.equals("OAuth2")){id = email;}else{id=url;}
        	accounts.add(new Account(selection.getString(0), id,"Unknown",service_id));
        }
        myDB.close();
        final ListView curservices = (ListView) findViewById(R.id.list);
        registerForContextMenu(curservices);
        
        curservices.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
     		 public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long id) {
     			String name = accounts.get(position).name;
     			myDB = MainActivity.this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
     			Cursor selection = myDB.query("ACCOUNTS",new String[]{"METHOD","SERVICE", "URL", "EMAIL","CREDENTIALS","EXPIRES"},"NAME='"+name+"'",null,null,null,null);
     			selection.moveToPosition(0);
     			Intent acc=new Intent(MainActivity.this, FileAccessActivity.class);
 				Bundle para=new Bundle();
 				para.putString("name", name);
 				para.putString("method",selection.getString(0));
 				para.putInt("service_id", utils.getStrId(selection.getString(1), MainActivity.this));
 				para.putString("url",selection.getString(2));
 				para.putString("email", selection.getString(3));
 				para.putString("credentials", selection.getString(4));
 				para.putLong("expires", selection.getLong(5));
     			myDB.close();
     			startActivity(acc.putExtras(para));
     		 }
     	});

        curservices.setAdapter(account_adapter);  
      Runnable runnable = new Runnable() {
          @Override
          public void run() {
          	SQLiteDatabase backDB = MainActivity.this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
        	  for(int i=0; i<accounts.size(); i++){
               	  Account old = accounts.get(i);
               	  Cursor select = backDB.query("ACCOUNTS",new String[]{"SERVICE","METHOD", "URL","CREDENTIALS","EXPIRES"},"NAME='"+old.name+"'",null,null,null,null);
               	  select.moveToPosition(0);
               	  int service = utils.getStrId(select.getString(0), MainActivity.this);  
               	  FileAccessActivity.expires = select.getLong(4);
               	  Services.AccountDetails details = s.new AccountDetails(select.getString(1),service,select.getString(2),select.getString(3));
               	  String quota = utils.bytes_in_h_format(details.quota_used)+"/"+utils.bytes_in_h_format(details.quota_total);
               	  accounts.set(i, new Account(old.name, old.id,quota,service));
               	runOnUiThread(new Runnable(){public void run(){account_adapter.notifyDataSetChanged();}});  
        	  }
        	  backDB.close();
          } 
     };
     new Thread(runnable).start();
    }
    @Override  
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {  
    	super.onCreateContextMenu(menu, v, menuInfo);  
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
     menu.setHeaderTitle("Options for " +accounts.get(info.position).name);
     menu.add(0, v.getId(), 0, "Details");
     menu.add(0, v.getId(), 0, "Rename");  
     menu.add(0, v.getId(), 0, "Disconnect");
 }  
    @Override
    public boolean onContextItemSelected(MenuItem item) { 
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        if(item.getTitle()=="Details"){viewDetails(item.getItemId(), info.position);} 
        if(item.getTitle()=="Rename"){renameService(item.getItemId(), info.position);}
        if(item.getTitle()=="Disconnect"){disconnectService(item.getItemId(),info.position);}  
        return true;  
    }      
    public void renameService(int id, final int position){
       final AlertDialog.Builder builder = new AlertDialog.Builder(this);
       final AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
       
       builder.setTitle("Title");
       builder2.setTitle("Name conflict");
       final EditText input = new EditText(this);
       input.setInputType(InputType.TYPE_CLASS_TEXT);
       builder.setView(input);
       builder.setMessage("Please enter a name: ");
       builder.setPositiveButton("OK", null);
       builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
           @Override
          public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
       });
       final AlertDialog alertdialog = builder.create();
       
       alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
    	    @Override
    	    public void onShow(DialogInterface dialog) {
    	        Button b = alertdialog.getButton(AlertDialog.BUTTON_POSITIVE);
    	        b.setOnClickListener(new View.OnClickListener() {
    	            @Override
    	            public void onClick(View view) {
    	            	 old_name = accounts.get(position).name;
    	                 changed_name = s.new ConfigureAccount().changeAccountName(old_name, input.getText().toString());
    	                 if(old_name.equals(changed_name)){
    	                	 	builder2.setMessage("Cannot change name as a unique name must be used.");
    	                	 	builder2.setPositiveButton(android.R.string.ok, null);
    	                	 	builder2.show();
    	                 }else{alertdialog.dismiss();}
    	            }
    	        });
    	    }
    	});
       alertdialog.show();
    }
    public void viewDetails(int id, int position){
        String info;
        String name = accounts.get(position).name;
        myDB = MainActivity.this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
		Cursor selection = myDB.query("ACCOUNTS",new String[]{"METHOD","SERVICE","URL","EMAIL","CREDENTIALS","EXPIRES"},"NAME='"+name+"'",null,null,null,null);
		selection.moveToPosition(0);
		String method  		= selection.getString(0);
		int service_id = utils.getStrId(selection.getString(1), MainActivity.this);
		String url     		= selection.getString(2);
		String email        = selection.getString(3);
		String credentials  = selection.getString(4);
		FileAccessActivity.expires = selection.getLong(5);
		myDB.close();
		info = s.new Service(method, service_id).getDetails(url, email, credentials);
        new AlertDialog.Builder(this)
        .setTitle("View details").setMessage(info)
        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){}})
        .setIcon(android.R.drawable.ic_dialog_info)
        .show();
     }  
    public void disconnectService(int id, final int position){  
        new AlertDialog.Builder(this)
        .setTitle("Disconnect service")
        .setMessage("Are you sure you want to disconnect this service?")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
            	s.new ConfigureAccount().disconnectAccount(accounts.get(position).name);
            }
         })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}})
        .setIcon(android.R.drawable.ic_dialog_alert)
         .show();   
    }  
    public void startsetup(View v){startActivity(new Intent(this, ChooseServiceActivity.class));}
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hub, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        	case R.id.add:startActivity(new Intent(this, ChooseServiceActivity.class)); return true;	
        	case R.id.downloads:startActivity(new Intent(this, DownloadsActivity.class).putExtra("a", "downloads")); return true;
        	default: return super.onOptionsItemSelected(item);
    	}
    }
}