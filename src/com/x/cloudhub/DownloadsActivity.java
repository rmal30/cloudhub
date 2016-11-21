package com.x.cloudhub;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
public class DownloadsActivity extends Activity{
	String view = new String(); ArrayList<Download>downloads; DownloadAdapter download_adapter;
	SQLiteDatabase myDB; String[] services;
	public class Download{
		String name; String path; String service_name; boolean isFolder;
		public Download(){
			super();
		}
		public Download(String name, String path,boolean isFolder, String service_name){
			super();
			this.name = name; 
			this.path=path;
			this.isFolder=isFolder;
			this.service_name = service_name;
			
		}
		
	}
	 public class DownloadAdapter extends ArrayAdapter<Download> {
		    public DownloadAdapter(Context context, ArrayList<Download> downloads) {super(context, 0,downloads);}
		    public View getView(int position, View convertView, ViewGroup parent) {
		    	Download download = getItem(position);    
		       if (convertView == null) {convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_items_view, parent, false);}
		       TextView txtName = (TextView) convertView.findViewById(R.id.item_name);
		       TextView txtId = (TextView) convertView.findViewById(R.id.item_type);
		       TextView txtQuota = (TextView) convertView.findViewById(R.id.item_quota);
		       ImageView service = (ImageView) convertView.findViewById(R.id.icon);
		       txtName.setText(download.name);txtId.setText(download.path); txtQuota.setText(download.service_name);
		       int drawable_id = R.drawable.filetype_unknown;
		       if(download.isFolder){drawable_id = R.drawable.folder;}
		       service.setImageDrawable(getResources().getDrawable(drawable_id));
		       return convertView;
		   }
		}
	 public void onCreate(Bundle savedInstanceState) {
		 
		services =  utils.getServices(this);
		 	super.onCreate(savedInstanceState);
		 	view = this.getIntent().getStringExtra("a");
		 	String openfile = this.getIntent().getStringExtra("file");
		 	if(openfile!=null){
		 		System.out.println(openfile);
		 		utils.open(new File(openfile), this);
		 		
		 	}
		 	this.setTitle(view);
	        if(view.equals("downloads")){
	        	downloads = new ArrayList<Download>();
	        	setContentView(R.layout.download_list);this.setTitle(R.string.download);
	        	myDB = DownloadsActivity.this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
	        	myDB.execSQL("CREATE TABLE IF NOT EXISTS DOWNLOADS (FILE_NAME TEXT,PATH TEXT,SERVICE_NAME TEXT)");
	        	 int num_rows = myDB.rawQuery("SELECT * FROM DOWNLOADS",null).getCount();
	        	 download_adapter = new DownloadAdapter(this,downloads);
	             Cursor selection = myDB.query("DOWNLOADS",new String[]{"FILE_NAME","PATH","SERVICE_NAME"},null,null,null,null,null);
	             for(int i=num_rows-1; i>-1; i--){
	            	 Download download  = new Download();
	             	selection.moveToPosition(i);
	             	download.name = selection.getString(0);
	             	download.path =selection.getString(1);
	             	try {
						download.isFolder = new File(new URI(download.path+Uri.encode(download.name))).isDirectory();
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	             	download.service_name = selection.getString(2);
	             	downloads.add(download);	
	             }
	             myDB.close();	
	             final ListView curdownloads = (ListView) findViewById(R.id.downloads);
	             registerForContextMenu(curdownloads);
	             curdownloads.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
		    		 public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long id) {
		    			 Download d = downloads.get(position);
		    			 try {
		    			 if(d.isFolder){
		    				 new Services(DownloadsActivity.this).new FileChooser("open").chooseFile(new URI(d.path+Uri.encode(d.name)));
		    			 }else{
								utils.open(new File(new URI(d.path+Uri.encode(d.name))), DownloadsActivity.this);
		    			 }
		    			 } catch (URISyntaxException e) {e.printStackTrace();}
		    		 }
		    	});
	             curdownloads.setAdapter(download_adapter);    
	        }	        
	  }
	
	 public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {  
	    super.onCreateContextMenu(menu, v, menuInfo);  
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	     menu.setHeaderTitle("Options for " +downloads.get(info.position).name);
	     menu.add(0, v.getId(), 0, "Details");
	     menu.add(0, v.getId(), 0, "Upload");
	     menu.add(0, v.getId(), 0, "Remove from list");
	     menu.add(0, v.getId(), 0, "Delete");
	 }
	 public boolean onContextItemSelected(MenuItem item) { 
	    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	        if(item.getTitle()=="Details"){viewDetails(info.position);} 
	        if(item.getTitle()=="Upload"){
	        	UploadDownload(info.position);
	        	} 
	        if(item.getTitle()=="Remove from list"){deleteDownload(info.position,false);}  
	        if(item.getTitle()=="Delete"){deleteDownload(info.position,true);}  
	        return true;  
	    }

	private void deleteDownload(int position, boolean b) {
		Download d = downloads.get(position);
		if(b){try {
			deleteLocalItem(new File(new URI(d.path+Uri.encode(d.name))));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}}
		myDB= openOrCreateDatabase("services.db", MODE_PRIVATE, null);
		myDB.execSQL("DELETE FROM DOWNLOADS WHERE FILE_NAME='"+d.name+"' AND PATH='"+d.path+"'");
		myDB.close();
		downloads.remove(position);
		download_adapter.notifyDataSetChanged();
	}
	public void deleteLocalItem(File f){
		if(f.isDirectory()){
			File[] files = f.listFiles();
			for(int i=0; i<files.length; i++){deleteLocalItem(files[i]);}
		}
		f.delete();
	}
	private void UploadDownload(int position) {
		//Select cloud service
		//Use shared pref
		//Go to file system view with upload button
		//Perform upload current_id
		final Download d = downloads.get(position);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose a service").setItems(services, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialogInterface, int i){
				String name = services[i];
				myDB = DownloadsActivity.this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
				Cursor selection = myDB.query("ACCOUNTS",new String[]{"METHOD","SERVICE", "URL", "EMAIL","CREDENTIALS"},"NAME='"+name+"'",null,null,null,null);
				selection.moveToPosition(0);
				Intent acc=new Intent(DownloadsActivity.this, FileAccessActivity.class);
				Bundle para=new Bundle();
				para.putString("name", name);
				para.putString("method",selection.getString(0));
				para.putInt("service_id",utils.getStrId(selection.getString(1), DownloadsActivity.this));
				para.putString("url",selection.getString(2));
				para.putString("email", selection.getString(3));
				para.putString("credentials", selection.getString(4));
				para.putBoolean("upload", true);
				myDB.close();
				Editor edit = DownloadsActivity.this.getSharedPreferences("com.x.cloudhub.upload", Context.MODE_PRIVATE).edit();
				edit.clear();
				edit.putBoolean("isFolder",d.isFolder);
				edit.putString("name", d.name);
				edit.putString("path", d.path);
				edit.apply();
				startActivity(acc.putExtras(para));	
			}
		} ).show();
	
		
			
	}

	private void viewDetails(int position) {
		String info="";
		Download d = downloads.get(position);
		File f;
		try {
			f = new File(new URI(d.path+Uri.encode(d.name)));
			info = info.concat("Name: "+f.getName()+"\n");
			info = info.concat("Path: "+f.getParent()+"\n");
		     if(!f.isDirectory()){
		       info = info.concat("Type: "+MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(f.getPath().replaceAll(" ","")))+"\n");
		  	   info = info.concat("Size: "+utils.bytes_in_h_format(f.length())+"\n");
		     }
		     info = info.concat("Date modified: "+new Date(f.lastModified()).toString()+"\n");
		} catch (URISyntaxException e) {
			info = "The file system may be corrupted";
			e.printStackTrace();
		}
		
	     new AlertDialog.Builder(this)
	        .setTitle(d.name)
	        .setMessage(info)
	        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which){}})
	        .setIcon(android.R.drawable.ic_dialog_info)
	        .show();
	}  

}
