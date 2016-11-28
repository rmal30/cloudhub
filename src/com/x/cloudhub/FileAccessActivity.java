package com.x.cloudhub;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Stack;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
public class FileAccessActivity extends Activity{
	 Services s=new Services(this);
	 int notify_id;
  	 int service_id; String name,credentials,baseurl="",method="",email; 
	 Stack<String> previousIds; Socket ftp_socket; String current_id; Context ctx = FileAccessActivity.this;
	 static Long expires; ArrayList<Services.Item> items;static ItemAdapter item_adapter;
	 Bundle extras; Services.Service service; Services.FileIO io;Services.FTPSocket ftpsock;
	 ArrayList<String> children;
	 static Notification notification; static NotificationManager mNotifyManager;
	 public class ItemAdapter extends BaseAdapter {
		private ArrayList<Services.Item> internal_items;
		public int internal_view;
	    public ItemAdapter(Context context, ArrayList<Services.Item> items) {
	    	this.internal_items = items;
	    	this.internal_view = R.layout.list_items_view;
	    }
		@Override
		public Services.Item getItem(int arg0) {return this.internal_items.get(arg0);}
		@Override
		public long getItemId(int arg0) {return arg0;}
	    public View getView(final int position, View convertView, ViewGroup parent) {
	       Services.Item item = getItem(position);    
	       
	       if (convertView==null || convertView.getId()!=this.internal_view) {
	    	   convertView = LayoutInflater.from(ctx).inflate(this.internal_view, parent, false);
	       }
	       TextView txtName = (TextView) convertView.findViewById(R.id.item_name);
	       TextView txtId = (TextView) convertView.findViewById(R.id.item_type);
	       TextView txtQuota = (TextView) convertView.findViewById(R.id.item_quota);
	       ImageView service = (ImageView) convertView.findViewById(R.id.icon);
	       CheckBox select = (CheckBox) convertView.findViewById(R.id.checkBox1);
	       txtName.setText(item.name); txtId.setText(item.type); 
	       txtQuota.setText(utils.bytes_in_h_format(item.size));
	       select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				getItem(position).selected = isChecked;
				if(getSelectedCount(items)>0){
					findViewById(R.id.actions).setVisibility(View.VISIBLE);
					findViewById(R.id.default1).setVisibility(View.GONE);
				}else{
					findViewById(R.id.actions).setVisibility(View.GONE);
					findViewById(R.id.default1).setVisibility(View.VISIBLE);
				}
			}  
	       });
	       select.setChecked(item.selected);
	       
	       int drawable_id = R.drawable.filetype_unknown;
	       if(item.isFolder){drawable_id = R.drawable.folder;
	       }else{
	    	   if(item.type.contains("pdf")){drawable_id = R.drawable.filetype_pdf;}
	       }
	       service.setImageDrawable(getResources().getDrawable(drawable_id));
	       return convertView;
	   }
		@Override
		public int getCount() {return internal_items.size();}
	 }
	 public void onCreate(Bundle savedInstanceState) {
	     super.onCreate(savedInstanceState);
		  extras = this.getIntent().getExtras();
		  setContentView(R.layout.file_system);
		  service_id = extras.getInt("service_id"); name = extras.getString("name");
		  method = extras.getString("method"); baseurl = extras.getString("url");
		  email = extras.getString("email"); credentials = extras.getString("credentials");
		  FileAccessActivity.expires = extras.getLong("expires");
		  boolean upload=extras.getBoolean("upload");
		  
		  children = new ArrayList<String>();
		  if(upload){
			  findViewById(R.id.upload).setVisibility(View.VISIBLE);
			  findViewById(R.id.addFile).setVisibility(View.GONE);
		  }
	        items = new ArrayList<Services.Item>();
	        previousIds = new Stack<String>();
	        item_adapter = new ItemAdapter(this,items);
			 final ListView curitems = (ListView) findViewById(R.id.curitems);
			 final GridView curitems2 = (GridView) findViewById(R.id.curitems2);
			 registerForContextMenu(curitems);
			 registerForContextMenu(curitems2);
			  setTitle(name);
			  service = s.new Service(method, service_id); io = s.new FileIO(method, service_id, baseurl, credentials);  
	        curitems.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
	    		 public void onItemClick(AdapterView<?> parentAdapter, View view, final int position, long id) {
	    			
	    			 itemClicked(position);
	    		 }
	    	 });
	        curitems2.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
	    		 public void onItemClick(AdapterView<?> parentAdapter, View view, final int position, long id) {
	    			itemClicked(position);
	    		 }
	    	 });
	        curitems.setAdapter(item_adapter); curitems2.setAdapter(item_adapter);
	        current_id = service.getPaths(baseurl).root;
	        showFiles(current_id,"List",null);
	    }
	 public void itemClicked(final int position){
		  String chosen_id = items.get(position).id;
		 	if(items.get(position).isFolder){
		 		previousIds.add(current_id);
				 current_id = chosen_id; showFiles(current_id,"List",null);
			 }else{
				 String info = utils.getItemDetails(items, position);
			        new AlertDialog.Builder(ctx)
			        .setTitle(items.get(position).name).setMessage(info)
			        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which){}})
			        .setNegativeButton("Download File", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which){downloadItem(position);}})
			        .setIcon(android.R.drawable.ic_dialog_info)
			        .show();
			 }
	}
	 public void deselect(View view){
		 for(int i=0; i<items.size(); i++){
			 items.get(i).selected = false;
		 }
		 findViewById(R.id.actions).setVisibility(View.GONE);
		 findViewById(R.id.default1).setVisibility(View.VISIBLE);
		 item_adapter.notifyDataSetChanged();
	 }
	 public void showFiles(final String dir_id, final String op,final String[] params){
		 items.clear();
		 findViewById(R.id.linearLayout4).setVisibility(View.VISIBLE);
		 findViewById(R.id.back).setVisibility(View.GONE);
		 Thread t = new Thread(){
			 public void run(){
				 if(op.equals("List")){children = io.list_children(dir_id);}				
				 else if(op.equals("Sort")){children = io.sort(children, params[0], Boolean.parseBoolean(params[1]));}
				 else if(op.equals("Search")){children = io.search_children(dir_id, params[0]);}
				 else if(op.equals("Filter")){children = io.filter(children, params[0], params[1], params[2]);}
			 runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                	 for(int i=0; i<children.size(); i++){
        				 Services.Item item=s.new Item(method, service_id,children.get(i));
        				 items.add(item);
        			 }
                	 findViewById(R.id.linearLayout4).setVisibility(View.GONE);
                	 item_adapter.notifyDataSetChanged();
                	 if(previousIds.size()>0){
                		 findViewById(R.id.back).setVisibility(View.VISIBLE);
                	 }
                 }});
			 
			 }
		 };
		 t.start(); 
	 }
	 public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {  
    	super.onCreateContextMenu(menu, v, menuInfo);  
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	     
	     items.get(info.position).selected = true;
	     item_adapter.notifyDataSetChanged();
	     if(getSelectedCount(items)>1){
	    	 menu.setHeaderTitle("Options for " +getSelectedCount(items)+" items");
		     menu.add(0, v.getId(), 0,"Download items");
		     menu.add(0, v.getId(), 0,"Cut items");
		     menu.add(0, v.getId(), 0,"Copy items");
		     if(method.equals("OAuth2")){
		    	 menu.add(0, v.getId(), 0, "Send items to Trash");
		     }
		     if(service_id==R.string.google || service_id==R.string.box || method.equals("WebDAV")){
		    	 menu.add(0, v.getId(), 0, "Delete items permanently");
		     }
	     }else{
	    	 menu.setHeaderTitle("Options for " +items.get(info.position).name);
	         menu.add(0, v.getId(), 0, "Details");
		     menu.add(0, v.getId(), 0, "Rename");
		     menu.add(0, v.getId(), 0,"Download");
		     menu.add(0, v.getId(), 0, "Cut");
		     menu.add(0, v.getId(), 0, "Copy");
		     
		     if(method.equals("OAuth2")){
		    	 menu.add(0,v.getId(),0,"View online");
		    	 menu.add(0, v.getId(), 0, "Send to Trash");
		     }
		     if(service_id==R.string.google || service_id==R.string.box || method.equals("WebDAV")){
		    	 menu.add(0, v.getId(), 0, "Delete permanently");
		     }
	     }
	 }  
	 public boolean onContextItemSelected(MenuItem item) { 
	    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	        if(item.getTitle()=="Details"){viewDetails(info.position);} 
	        if(item.getTitle()=="Download"){downloadItem(info.position);}
	        if(item.getTitle()=="Download items"){downloadItems(null);}
	        if(item.getTitle()=="Rename"){renameItem(info.position);}
	        if(item.getTitle()=="Cut"){storeItem(info.position, "move");}
	        if(item.getTitle()=="Cut items"){storeItems("move");}
	        if(item.getTitle()=="Copy"){storeItem(info.position,"copy");}
	        if(item.getTitle()=="Copy items"){storeItems("copy");}
	        if(item.getTitle()=="View online"){viewItem(info.position);}
	        if(item.getTitle()=="Delete permanently"){deleteItem(info.position,true);}  
	        if(item.getTitle()=="Delete items permanently"){deleteItems(true);}
	        if(item.getTitle()=="Send to Trash"){deleteItem(info.position,false);}
	        if(item.getTitle()=="Send items to Trash"){deleteItems(false);}
	        return true;  
	}  
	 public void trashItems(View v){
		 if(current_id.equals("#trash")){
			 deleteItems(true);
		 }else{
			 deleteItems(false);
		 }
	 }
	 public void cutItems(View v){
		 storeItems("move");
	 }
	 public void copyItems(View v){
		 storeItems("copy");
	 }
	 private void deleteItems(final boolean permanent) {
		new AlertDialog.Builder(this)
        .setTitle("Delete").setMessage("Are you sure you want to delete selected items?")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
            	final ProgressDialog dialog2 = new ProgressDialog(ctx);
            	Thread thr = new Thread(){
            		public void run(){
		            	for(int i=0; i<items.size(); i++){
		            		
				            Services.Item item = items.get(i);
				            if(item.selected){
				            	io.delete(item.isFolder,item.id,permanent);
				            }
		            	}
		            	runOnUiThread(new Runnable(){
		            		public void run(){
		            			Toast.makeText(ctx,"Items deleted",2000).show();
		            			showFiles(current_id, "List", null);
		            			deselect(null);
		            			dialog2.dismiss();
		            		}
		            	});
		            	
            		}
            	};
            	thr.start();
            	dialog2.setMessage("Deleting items. Please wait...");
            	dialog2.show();
            	
   		     } 
         })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}})
        .setIcon(android.R.drawable.ic_dialog_alert)
         .show();
	}
	 public void storeItems(String op) {
		int count = 0;
		for(int i=0; i<items.size(); i++){
			Services.Item item = items.get(i);
			if(item.selected){
				count++;
				Editor edit = this.getSharedPreferences("com.x.cloudhub.clipboard"+"-"+count, Context.MODE_PRIVATE).edit();
				edit.clear();
				edit.putBoolean("isFolder",item.isFolder);
				edit.putString("name", item.name);
				edit.putString("id", item.id);
				edit.putString("parent_id", current_id);
				edit.putString("operation", op);
				edit.putBoolean("hasNext", count!=getSelectedCount(items));
				edit.apply();
			}
		}
		deselect(null);
		findViewById(R.id.paste).setVisibility(View.VISIBLE);
	}
	 public void downloadItems(View v) {
		final Context ctx = this;
		ProgressActivity.progress = new ProgressDialog(ctx);
		 final int notify_id = utils.genRandomNum(10000);
		 
		 final int num = getSelectedCount(items);
 		
	       final Thread mThread = new Thread() {
			    @Override
			    public void run() {
			    		for(int i=0; i<items.size(); i++){
				    		final Services.Item item =  items.get(i);				    		
				    		if(item.selected){
				    			final int index = i;
				    			runOnUiThread(new Runnable(){
				    				public void run(){
				    					ProgressActivity.progress.setMessage("Downloading "+items.get(index)+". Please wait...");
						    		    ProgressActivity.progress.setMax(0);		
						    		    io.getTotalSize(item.isFolder, item.id, Integer.parseInt(item.size));
				    				}
				    			});
				    			
					    		 File file = io.download(item.isFolder,item.id, item.name,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),notify_id);	 
						    	SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
						    	myDB.execSQL("CREATE TABLE IF NOT EXISTS DOWNLOADS (FILE_NAME TEXT,PATH TEXT,SERVICE_NAME TEXT)");
						    	ContentValues values = new ContentValues();
						    		values.put("FILE_NAME", file.getName());
						    		values.put("PATH", Uri.fromFile(file.getParentFile()).toString()+"/");
						    		values.put("SERVICE_NAME", name);
						    	myDB.insert("DOWNLOADS",null,values);
						    	myDB.close();	
					    	}
			    		}
			    		runOnUiThread(new Runnable(){
			    			public void run(){
			    			deselect(null);
			    			}
			    		});
			    		 FileAccessActivity.mNotifyManager.cancel(notify_id);
				    	String title ="Downloaded "+num+" items"; 
				    	 Notification download_done=new Notification(android.R.drawable.stat_sys_download_done, title, System.currentTimeMillis());
				    	download_done.setLatestEventInfo(ctx, title,"",
				    			PendingIntent.getActivity(ctx, 0, new Intent(ctx, DownloadsActivity.class).putExtra("a", "downloads"),PendingIntent.FLAG_UPDATE_CURRENT));
				    	download_done.flags |= Notification.FLAG_AUTO_CANCEL; 
				    	FileAccessActivity.mNotifyManager.notify(title, 0, download_done);		
				    	
				    	ProgressActivity.progress.dismiss();
			    }
	       };
	       mThread.start(); 
	       
	       ProgressActivity.progress.setButton(ProgressDialog.BUTTON_POSITIVE, "Do in background", new DialogInterface.OnClickListener() {
		 	        @Override
		 	        public void onClick(DialogInterface dialog, int which) {ProgressActivity.progress.dismiss();}
		 	    });
	       String title = "Downloading "+num+" items";
	       if(num==1){
	    	  title = "Downloading "+items.get(0).name;
			}
	       ProgressActivity.progress.setTitle(title);
	       ProgressActivity.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	       
			 FileAccessActivity.mNotifyManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
			 FileAccessActivity.notification=new Notification(android.R.drawable.stat_sys_download, title, System.currentTimeMillis());
			 FileAccessActivity.notification.flags = FileAccessActivity.notification.flags | Notification.FLAG_ONGOING_EVENT;
			 FileAccessActivity.notification.setLatestEventInfo(ctx, title, "", 
			 PendingIntent.getActivity(ctx, utils.genRandomNum(100000),new Intent(ctx,ProgressActivity.class),Intent.FLAG_ACTIVITY_CLEAR_TOP));
			 FileAccessActivity.mNotifyManager.notify(notify_id,FileAccessActivity.notification);
	}
	 private void viewItem(int position) {
		String content = children.get(position);
		String edit_url = utils.getProperty(method, content, "alternateLink");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("View");
		WebView wv1 = new WebView(this) {
			  @Override
			  public boolean onCheckIsTextEditor() {
			    return true;
			  }
			};
		wv1.loadUrl(edit_url);
		wv1.setMinimumHeight(400);
		wv1.getSettings().setJavaScriptEnabled(true);
		wv1.setWebViewClient(new WebViewClient() {
		    @Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
		        view.loadUrl(url);
		        return true;
		    }
		});
		alert.setView(wv1);
		alert.show();
	}
	 private void downloadItem(int position){
		 final Context ctx = this;
		 final Services.Item item =  items.get(position);
		 final int notify_id = utils.genRandomNum(10000);
		   ProgressActivity.progress = new ProgressDialog(ctx);
	       final Thread mThread = new Thread() {
			    @Override
			    public void run() {
			    		 File file = io.download(item.isFolder,item.id, item.name,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),notify_id);
			    		 FileAccessActivity.mNotifyManager.cancel(notify_id);
				    	 String title ="Downloaded "+item.name; 
				    	 Notification download_done=new Notification(android.R.drawable.stat_sys_download_done, title, System.currentTimeMillis());
				    	Bundle download_params = new Bundle();
							download_params.putString("file",file.getPath());
							download_params.putString("a", "downloads");
				    	download_done.setLatestEventInfo(ctx, title,"",
				    	PendingIntent.getActivity(ctx, 0, new Intent(ctx, DownloadsActivity.class).putExtras(download_params),PendingIntent.FLAG_UPDATE_CURRENT));
				    	download_done.flags |= Notification.FLAG_AUTO_CANCEL; 
				   
				    	FileAccessActivity.mNotifyManager.notify(title, 0, download_done);		
				    	
				    	SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
				    	myDB.execSQL("CREATE TABLE IF NOT EXISTS DOWNLOADS (FILE_NAME TEXT,PATH TEXT,SERVICE_NAME TEXT)");
				    	ContentValues values = new ContentValues();
				    		values.put("FILE_NAME", file.getName());
				    		values.put("PATH", Uri.fromFile(file.getParentFile()).toString()+"/");
				    		values.put("SERVICE_NAME", name);
				    	myDB.insert("DOWNLOADS",null,values);
				    	myDB.close();
				    	ProgressActivity.progress.dismiss();
			    }
	       };
	       mThread.start(); 
	       ProgressActivity.progress.setButton(ProgressDialog.BUTTON_POSITIVE, "Do in background", new DialogInterface.OnClickListener() {
		 	        @Override
		 	        public void onClick(DialogInterface dialog, int which) {ProgressActivity.progress.dismiss();}
		 	    });
	       ProgressActivity.progress.setTitle("Downloading "+item.name+". Please wait...");
	       ProgressActivity.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	       ProgressActivity.progress.setMax(0);
			 if(item.isFolder){ProgressActivity.progress.setMessage("Getting total size");}
			 ProgressActivity.progress.setIndeterminate(true);
			 FileAccessActivity.mNotifyManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
			 FileAccessActivity.notification=new Notification(android.R.drawable.stat_sys_download, "Downloading "+item.name, System.currentTimeMillis());
			 FileAccessActivity.notification.flags = FileAccessActivity.notification.flags | Notification.FLAG_ONGOING_EVENT;
			 FileAccessActivity.notification.setLatestEventInfo(ctx, "Downloading "+item.name, "", 
			 PendingIntent.getActivity(ctx, utils.genRandomNum(100000),new Intent(ctx,ProgressActivity.class),Intent.FLAG_ACTIVITY_CLEAR_TOP));
			 FileAccessActivity.mNotifyManager.notify(notify_id,FileAccessActivity.notification);
		   io.getTotalSize(item.isFolder, item.id, Integer.parseInt(item.size));
		   ProgressActivity.progress.setIndeterminate(false);
	 }
	 private void storeItem(int position, String op) {
		Services.Item item = items.get(position);
		Editor edit = this.getSharedPreferences("com.x.cloudhub.clipboard-1", Context.MODE_PRIVATE).edit();
		edit.clear();
		edit.putBoolean("isFolder",item.isFolder);
		edit.putString("name", item.name);
		edit.putString("id", item.id);
		edit.putString("parent_id", current_id);
		edit.putString("operation", op);
		edit.putBoolean("hasNext", false);
		edit.apply();
		deselect(null);
		findViewById(R.id.paste).setVisibility(View.VISIBLE);
	}
	 public void renameItem(final int position){
	    	final Services.Item item = items.get(position);
	        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Rename " +item.name+" to:");
	        final EditText input = new EditText(this);
	        input.setText(item.name);
	        input.setInputType(InputType.TYPE_CLASS_TEXT);
	        builder.setView(input);
	        input.selectAll();
	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	String name = input.getText().toString();
	            	 io.rename(item.isFolder,item.id, item.name, name);
	            	 items.set(position,item.rename(name));
   		    		 item_adapter.notifyDataSetChanged();
		         	  }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
	        builder.show();
	     }   
	 public void viewDetails(int position){
	       String info = utils.getItemDetails(items, position);
	        new AlertDialog.Builder(this)
	        .setTitle(items.get(position).name)
	        .setMessage(info)
	        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which){}})
	        .setIcon(android.R.drawable.ic_dialog_info)
	        .show();
	     }  
	 public void deleteItem(final int position, final boolean permanent){  
	        new AlertDialog.Builder(this)
	        .setTitle("Delete").setMessage("Are you sure you want to delete this item?")
	        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) { 
	   		     Services.Item item= items.get(position);
	   		     String status = io.delete(item.isFolder,item.id,permanent);
	   		    String info;
   		    	 if(status.contains("404")||status.contains("400")){
   		    		info="Item could not be deleted. Please try again later";
   		    	 }else{
   		    		 info = "Item was successfully deleted";
   		    		 items.remove(position);
   		    		 item_adapter.notifyDataSetChanged();
   		    	 }
   		    	new AlertDialog.Builder(ctx)
		        .setTitle("Status").setMessage(info)
		        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){}})
		        .setIcon(android.R.drawable.ic_dialog_info)
		        .show();
	   		     } 
	         })
	        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {}})
	        .setIcon(android.R.drawable.ic_dialog_alert)
	         .show();
	        
	    }

	 public boolean onCreateOptionsMenu(Menu menu) {
	        getMenuInflater().inflate(R.menu.access, menu);
	        return true;
	    }
	 public boolean onOptionsItemSelected(MenuItem item) {
	    	switch (item.getItemId()) {
	        	case R.id.home:startActivity(new Intent(this, MainActivity.class)); return true;
	        	case R.id.debug:startActivity(new Intent(this, DebugActivity.class).putExtras(extras)); return true;
	        	case R.id.downloads:startActivity(new Intent(this, DownloadsActivity.class).putExtra("a", "downloads")); return true;
	        	
	        	//case R.id.options:
	        		case R.id.details:
	        			String info = service.getDetails(baseurl,email,credentials);
	        	        new AlertDialog.Builder(this)
	        	        .setTitle("View details").setMessage(info)
	        	        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){}})
	        	        .setIcon(android.R.drawable.ic_dialog_info)
	        	        .show();
	        	        return true;
	        		case R.id.rename:
	        		       final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        		       builder.setTitle("Rename "+name+" to:");
	        		       final EditText input = new EditText(this);
	           		       input.setInputType(InputType.TYPE_CLASS_TEXT);
	           		       input.setText(name);
	        		       builder.setView(input);
	        		       builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	        		           @Override
	        		           public void onClick(DialogInterface dialog, int which) {
	        		              String new_name = s.new ConfigureAccount().changeAccountName(name, input.getText().toString());
	        		              setTitle(new_name);
	        		              if(name.equals(new_name)){builder.setMessage("Cannot change name as a unique name must be used.");
	        		              }else{name=new_name;}
	        		           }
	        		       });
	        		       builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) { dialog.cancel();}});
	        		       builder.show();
	        		     return true;
	        		case R.id.disconnect:
	        			 new AlertDialog.Builder(this)
	        		        .setTitle("Disconnect service").setMessage("Are you sure you want to disconnect this service?")
	        		        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	        		            public void onClick(DialogInterface dialog, int which) { 
	        		            	s.new ConfigureAccount().disconnectAccount(name);startActivity(new Intent(ctx, MainActivity.class)); return;
	        		            }
	        		         })
	        		        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {dialog.cancel();}})
	        		        .setIcon(android.R.drawable.ic_dialog_alert)
	        		         .show();
	        			 return true;
	  	        	default: return super.onOptionsItemSelected(item);
	    	}
	    }

	 public void back(View view){
		 current_id = previousIds.pop();
		 if(previousIds.size()>0){
    		 findViewById(R.id.back).setVisibility(View.VISIBLE);
    	 }else{
    		 findViewById(R.id.back).setVisibility(View.GONE);
    	 }
		 showFiles(current_id,"List", null);
	 }
	 public void selectAll(View view){
		 for(int i=0; i<items.size(); i++){
			 items.get(i).selected = true;
		 }
		 findViewById(R.id.actions).setVisibility(View.VISIBLE);
		 findViewById(R.id.default1).setVisibility(View.GONE);
		 item_adapter.notifyDataSetChanged();
	 }
	 public void toggleView(View view){
		 if(findViewById(R.id.curitems).getVisibility()==View.VISIBLE){
			 findViewById(R.id.curitems).setVisibility(View.GONE);
			 findViewById(R.id.curitems2).setVisibility(View.VISIBLE);
			 item_adapter.internal_view=R.layout.grid_items_view;
			 findViewById(R.id.grid_list).setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_view_list_white_48dp));
		 }else{
			 findViewById(R.id.curitems).setVisibility(View.VISIBLE);
			 findViewById(R.id.curitems2).setVisibility(View.GONE);
			 item_adapter.internal_view=R.layout.list_items_view;
			 findViewById(R.id.grid_list).setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_view_module_white_48dp));
		 }
	 }
	 public void showAddDialog(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 String[] options = {"New folder", "Take photo", "Choose from gallery","Choose file/folder", "Choose from app"};
		 builder.setTitle("Add options:");
		 builder.setItems(options, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, final int option) {
		        	switch(option){
		        		case 0:
		        			 final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		        		        builder.setTitle("Enter a name for the new folder: ");
		        		        final EditText input = new EditText(ctx);
		        		        input.setInputType(InputType.TYPE_CLASS_TEXT);
		        		        builder.setView(input);
		        		        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
		        		            @Override
		        		            public void onClick(DialogInterface dialog, int which) {
		        		         	  String name = input.getText().toString();
		        		         	  String status = io.mkdir(current_id, name);
		        		         	 showFiles(current_id,"List", null);
		        		         	 Toast.makeText(ctx, status.split("\r\n")[0], 2000).show();
		        		         	  }
		        		        });
		        		        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		        		            @Override
		        		            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
		        		        });
		        		        builder.show();
		        			break;
		        		case 1: 
		        			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		        		    startActivityForResult(intent, 0);
		        			break;
		        		case 2: 
		        			Intent galleryIntent = new Intent(Intent.ACTION_PICK,
		        			        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		        			startActivityForResult(Intent.createChooser(galleryIntent, "Select File"), 1);
		        			break;
		        		
		        		case 3: 
		        			String state = Environment.getExternalStorageState();
		        		    if (Environment.MEDIA_MOUNTED.equals(state)) {
		        		    	File local_path = Environment.getExternalStorageDirectory();
		        		    	s.new FileChooser("upload", method, service_id, baseurl, credentials, current_id).chooseFile(local_path.toURI());
		        		    }
		        			break;
		        		case 4:
		        			Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);
		        			intent2.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
		        			startActivityForResult(intent2, 2);
		        		break;
		        	}
		        }	
		    });
		 final AlertDialog alert = builder.create();
		 alert.show();
	 }
	 public void showSortDialog(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 builder.setTitle("Sort options:");
		 builder.setView(LayoutInflater.from(this).inflate(R.layout.sort_options, null));
		 builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	String type = ((Spinner) ((AlertDialog) dialog).findViewById(R.id.sortType)).getSelectedItem().toString();	
	            	boolean ascending = ((RadioButton) ((AlertDialog) dialog).findViewById(R.id.ascend)).isChecked();
	            	if(!type.equals("No sort")){
	            		previousIds.add(current_id);
	            		showFiles(current_id, "Sort",new String[]{type, String.valueOf(ascending)});
	            	}
	         	}
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
		 final AlertDialog alertDialog = builder.create();
		 alertDialog.show();
		 
		 
	 }
	 public void showFilterDialog(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 builder.setTitle("Add filter:");
		 final View dialogView = LayoutInflater.from(this).inflate(R.layout.filter_options, null);
		 builder.setView(dialogView);
		 RadioGroup rg = (RadioGroup) dialogView.findViewById(R.id.radioGroup1);
		 rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
	            @Override
	            public void onCheckedChanged(RadioGroup group, int checkedId) {
	                 int option = group.getCheckedRadioButtonId();
	                 switch(option){
	                 	case R.id.radio1:
	                 		dialogView.findViewById(R.id.date).setVisibility(View.VISIBLE);
	                 		dialogView.findViewById(R.id.size).setVisibility(View.GONE);
	                 		break;
	                 	case R.id.radio2:
	                 		dialogView.findViewById(R.id.date).setVisibility(View.GONE);
	                 		dialogView.findViewById(R.id.size).setVisibility(View.VISIBLE);
	                 		break;
	                 	default: 	                 		
	                 		dialogView.findViewById(R.id.date).setVisibility(View.GONE);
	                 		dialogView.findViewById(R.id.size).setVisibility(View.GONE);
	                 		break;
	                 }
	            }
	        });
		 builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	previousIds.add(current_id);
	            	int option = ((RadioGroup)((AlertDialog) dialog).findViewById(R.id.radioGroup1)).getCheckedRadioButtonId();
	                 switch(option){
	                 	case R.id.radio1:
	                 		DatePicker dp1 = (DatePicker)((AlertDialog) dialog).findViewById(R.id.dateBegin);
	                 		String date1 = String.valueOf(dp1.getYear()) +"/"+ String.valueOf(dp1.getMonth())+"/"+String.valueOf(dp1.getDayOfMonth());
	                 		DatePicker dp2 = (DatePicker)((AlertDialog) dialog).findViewById(R.id.dateEnd);
	                 		String date2 = String.valueOf(dp2.getYear()) +"/"+ String.valueOf(dp2.getMonth())+"/"+String.valueOf(dp2.getDayOfMonth());
	                 		showFiles(current_id, "Filter", new String[]{"Date", date1, date2});
	                 		break;
	                 	case R.id.radio2:
	                 		String num1 = ((EditText)((AlertDialog) dialog).findViewById(R.id.numBegin)).getText().toString();
	                 		String mag1 = ((Spinner)((AlertDialog) dialog).findViewById(R.id.magBegin)).getSelectedItem().toString();
	                 		String num2 = ((EditText)((AlertDialog) dialog).findViewById(R.id.numEnd)).getText().toString();
	                 		String mag2 = ((Spinner)((AlertDialog) dialog).findViewById(R.id.magEnd)).getSelectedItem().toString();
	                 		showFiles(current_id, "Filter", new String[]{"Size", num1+" "+mag1, num2+" "+mag2});
	                 		break;
	                 	default:
	                 		showFiles(current_id, "List", null);
	                 		break;
	                 }
	            	
	         	}

				
	        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
		 final AlertDialog alertDialog = builder.create();
		 alertDialog.show();
		  
		 
	 }
	 public void showBookmarks(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 final String[][] bookmarks = getBookmarksFromDB();
		 boolean exists = false;
		 for(int i=0; i<bookmarks[0].length; i++){
			 if(bookmarks[1][i].equals(current_id)){exists = true;}
		 }
		 String[] options = {"Root folder", "Trash folder", "Shared folder","Existing bookmarks", "Bookmark this folder"};
		 if(exists){
			 options[4] = "Remove bookmark";
		 }
		 final boolean exists2 = exists;
		 		 builder.setItems(options, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, final int option) {
		        	switch(option){
		        		case 0: 
		        			previousIds.add(current_id);
		        			current_id = service.getPaths(baseurl).root;
		        			showFiles(current_id,"List",null);
		        			break;
		        		case 1: 
		        			previousIds.add(current_id);
		        			current_id = "#trash";
		        			showFiles(current_id, "List", null);
		        			break;
		        		case 2: 
		        			previousIds.add(current_id);
		        			current_id = "#shared";
		        			showFiles(current_id, "List", null);
		        			break;
		        		case 3: 
		        			AlertDialog.Builder builder2 = new AlertDialog.Builder(ctx);
		        			
		        			if(bookmarks[0].length>0){
			        			builder2.setTitle("Choose a bookmark:");
			        			builder2.setItems(bookmarks[0], new DialogInterface.OnClickListener() {
			        		        public void onClick(DialogInterface dialog, final int option) {
			        		        	previousIds.add(current_id);
			        		        	current_id=bookmarks[1][option];
			        		        	showFiles(current_id,"List", null);
			        		        }
			        		    });
		        			
		        			final AlertDialog alert2 = builder2.create();
		        			alert2.show();
		        			}else{
		        				Toast.makeText(ctx, "You have no bookmarks yet!", 1000).show();
		        			}
		        			break;
		        		case 4:
		        			final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		        			final EditText input;
		        			if(!exists2){
		        				builder.setTitle("Enter a name for the new bookmark: ");
		        				input = new EditText(ctx);
		        				input.setInputType(InputType.TYPE_CLASS_TEXT);
		        				builder.setView(input);
		        			}else{
		        				input = null;
		        				builder.setTitle("Delete bookmark");
		        				builder.setMessage("Are you sure you want to delete this bookmark?");
		        			}
		        	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
		        	            @Override
		        	            public void onClick(DialogInterface dialog, int which) {
		        	            	SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
		        	            	if(exists2){
			        	            	myDB.delete("BOOKMARKS", "ID=?", new String[]{current_id});
			        	            	Toast.makeText(ctx, "Bookmark deleted", 2000).show();
		        	            	}else{
			        	            	myDB.execSQL("CREATE TABLE IF NOT EXISTS BOOKMARKS (SERVICE_NAME TEXT, NAME TEXT, ID TEXT)");
			        	            	ContentValues values = new ContentValues();
			        			    		values.put("SERVICE_NAME", name);
			        			    		values.put("NAME", input.getText().toString());
			        			    		values.put("ID", current_id);
			        			    	myDB.insert("BOOKMARKS",null,values);
			        			    	
			        	            	Toast.makeText(ctx, "Bookmark added", 2000).show();
			        	            }
		        	            	myDB.close();
		        			    	
		        	         	  }
		        	        });
		        	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		        	            @Override
		        	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
		        	        });
		        	        builder.show();
		        			break;
		        	}
		        }

		 });		
		 final AlertDialog alert = builder.create();
		 alert.show();	 
	 }
		 
 	 public String[][] getBookmarksFromDB() {
				SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
				ArrayList<String> names = new ArrayList<String>(); 
				ArrayList<String> ids = new ArrayList<String>();
				Cursor selection = myDB.rawQuery("SELECT NAME, ID FROM BOOKMARKS WHERE SERVICE_NAME = ?", new String[]{name});
				selection.moveToFirst();
				while (!selection.isAfterLast()) {  
				    names.add(selection.getString(0));
				    ids.add(selection.getString(1));
				    selection.moveToNext();  
				}
				String[][] data = new String[2][];
				data[0] = names.toArray(new String[0]);
				data[1] = ids.toArray(new String[0]);
				myDB.close();
				return data;
	    }
	 public void showSearchDialog(View v){
		 final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Enter a search query: ");
	        final EditText input = new EditText(this);
	        input.setInputType(InputType.TYPE_CLASS_TEXT);
	        builder.setView(input);
	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	         	  String keyword = input.getText().toString();
	         	  	previousIds.add(current_id);
	         	  	showFiles(current_id,"Search", new String[]{keyword});
	         	  }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
	        builder.show(); 
	 }

	 public void paste(View v){
		 boolean hasNext = true;
		 int count = 0;
		 final int notify_id = utils.genRandomNum(10000);
		 final ArrayList <String> ids = new ArrayList<String>();
		 final ArrayList <Boolean> isFolders = new ArrayList<Boolean>();
		 final ArrayList <String> parentIds = new ArrayList<String>();
		 final ArrayList <String> item_names = new ArrayList<String>();
		 final ArrayList <String> ops = new ArrayList<String>();
		 ProgressActivity.progress=new ProgressDialog(ctx);
		 
		 ProgressActivity.progress.setButton(ProgressDialog.BUTTON_POSITIVE, "Do in background", new DialogInterface.OnClickListener() {
	 	        @Override
	 	        public void onClick(DialogInterface dialog, int which) {ProgressActivity.progress.dismiss();}
	 	    });
		 ProgressActivity.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		
		 while(hasNext){
			 count++;
			 SharedPreferences sprefs =  this.getSharedPreferences("com.x.cloudhub.clipboard-"+count, Context.MODE_PRIVATE);
			 isFolders.add(sprefs.getBoolean("isFolder",false));
			 ids.add(sprefs.getString("id", ""));
			 parentIds.add(sprefs.getString("parent_id", ""));
			 item_names.add(sprefs.getString("name", ""));
			 ops.add(sprefs.getString("operation",""));
			 hasNext = sprefs.getBoolean("hasNext", false);
			 sprefs.edit().clear();
		 }
		 String title = "Pasting "+count+" items";
		 if(count==1){title = "Pasting item";}
		 final String title2 = title.replace("Pasting", "Pasted");
		 ProgressActivity.progress.setTitle(title);
		 ProgressActivity.progress.setMessage("Getting files...");
		 ProgressActivity.progress.setMax(count);
			 Thread mThread = new Thread() {
				    @Override
				    public void run() {
				    	for(int i=0; i<ids.size(); i++){
				    		final String op = ops.get(i);
				    		final boolean isFolder = isFolders.get(i);
				    		final String id = ids.get(i);
				    		final String item_name = item_names.get(i);
				    		final String parent_id = parentIds.get(i);
				    		
							 String action = "";
							if(op.equals("move")){action = "Moving";}
							else if(op.equals("copy")){action= "Copying";}
							final String action2 = action;
							final int index = i;
							runOnUiThread(new Runnable(){public void run(){
								ProgressActivity.progress.setTitle(action2+" "+item_names.get(index)+". Please wait...");
							}});
							 if(op.equals("move")){
								  io.move(isFolder, id, item_name,parent_id,current_id);
							 }else if(op.equals("copy")){
								 io.copy(isFolder, id, item_name, current_id);
							 }
							 runOnUiThread(new Runnable(){public void run(){
								 ProgressActivity.progress.incrementProgressBy(1);
							 }});
				    	}
			 runOnUiThread(new Runnable(){public void run(){
				 ProgressActivity.progress.dismiss();
			      FileAccessActivity.mNotifyManager.cancel(notify_id); 
			      Notification copy_done=new Notification(android.R.drawable.ic_menu_save, title2, System.currentTimeMillis());
			      		copy_done.setLatestEventInfo(ctx, title2,"",
			    	PendingIntent.getActivity(ctx, 0, new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
			    	copy_done.flags |= Notification.FLAG_AUTO_CANCEL; 
			    	
			    	FileAccessActivity.mNotifyManager.notify(title2, 0, copy_done);
			    	showFiles(current_id,"List", null);
			    	findViewById(R.id.paste).setVisibility(View.INVISIBLE);
				 }});
			}
			};
			mThread.start();
			FileAccessActivity.mNotifyManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			 FileAccessActivity.notification=new Notification(android.R.drawable.ic_menu_save, title, System.currentTimeMillis());
			 FileAccessActivity.notification.flags = FileAccessActivity.notification.flags | Notification.FLAG_ONGOING_EVENT;
			 FileAccessActivity.notification.setLatestEventInfo(ctx, title, "", PendingIntent.getActivity(ctx, utils.genRandomNum(100000),new Intent(ctx,ProgressActivity.class),Intent.FLAG_ACTIVITY_CLEAR_TOP));
			 FileAccessActivity.mNotifyManager.notify(notify_id,FileAccessActivity.notification);
			ProgressActivity.progress.show();
			
			 
		 }
	 public void upload(View v){
		 	notify_id = utils.genRandomNum(10000); 
			SharedPreferences sprefs = this.getSharedPreferences("com.x.cloudhub.upload", Context.MODE_PRIVATE);
			 final boolean isFolder = sprefs.getBoolean("isFolder",false);
			 final String item_name = sprefs.getString("name", "");
			 final String path = sprefs.getString("path", "");
			 ProgressActivity.progress=new ProgressDialog(this);
			 ProgressActivity.progress.setButton(ProgressDialog.BUTTON_POSITIVE, "Do in background", 
					 new DialogInterface.OnClickListener() {
		 	        	@Override
		 	        		public void onClick(DialogInterface dialog, int which) {ProgressActivity.progress.dismiss();}
		 	    });
			 ProgressActivity.progress.setTitle("Uploading "+item_name+". Please wait...");
			 ProgressActivity.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			 ProgressActivity.progress.setIndeterminate(true);
			 ProgressActivity.progress.setMax(0);
			 if(isFolder){
				 ProgressActivity.progress.setMessage("Getting total size");
				 }
			 FileAccessActivity.mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			 FileAccessActivity.notification=new Notification(android.R.drawable.stat_sys_upload, "Uploading "+item_name, System.currentTimeMillis());
			 FileAccessActivity.notification.flags = FileAccessActivity.notification.flags | Notification.FLAG_ONGOING_EVENT;
			 FileAccessActivity.notification.setLatestEventInfo(ctx, "Uploading "+item_name, "", 
					 PendingIntent.getActivity(ctx, utils.genRandomNum(100000),new Intent(ctx,ProgressActivity.class),Intent.FLAG_ACTIVITY_CLEAR_TOP));
			 FileAccessActivity.mNotifyManager.notify(notify_id,FileAccessActivity.notification);
		      
			Thread mThread = new Thread() {
				    @Override
				    public void run() {
				    	try{
				    	File file = new File(new URI(path+Uri.encode(item_name)));
				    	utils.getTotalLocalSize(file);
				    	ProgressActivity.progress.setIndeterminate(false);
				        final String a = io.upload(current_id,file,false, notify_id);
				        ProgressActivity.progress.dismiss();
				    	FileAccessActivity.mNotifyManager.cancel(notify_id);
				    	
				    	String title ="Uploaded "+item_name; 
				    	Notification upload_done=new Notification(android.R.drawable.stat_sys_upload_done, title, System.currentTimeMillis());
				    	upload_done.setLatestEventInfo(ctx, title,"",
				    	PendingIntent.getActivity(ctx, 0, new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
				    	upload_done.flags |= Notification.FLAG_AUTO_CANCEL; 
				    	
				    	FileAccessActivity.mNotifyManager.notify(title, 0, upload_done);
				    	
				    	runOnUiThread(new Runnable(){public void run(){
				    		showFiles(current_id,"List", null);
				    		 Toast.makeText(ctx, a.split("\r\n")[0], 2000).show();
				    	}});
				    	}catch(Exception e){
				    		e.printStackTrace();
				    	}
				    }
			};
			mThread.start();
			item_adapter.notifyDataSetChanged();
			sprefs.edit().clear();
			findViewById(R.id.upload).setVisibility(View.GONE);
			findViewById(R.id.addFile).setVisibility(View.VISIBLE);
		 }	 

	 public void onBackPressed() {
		 startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY)); finish(); 
	 }
	 public int getSelectedCount(ArrayList<Services.Item> items){
		 int i=0;
		 for(Services.Item item:items){
			 if(item.selected) i++;
		 }
		 return i;
	 }
	 @Override
	 public void onActivityResult(int requestCode, int resultCode, Intent data){
		 super.onActivityResult(requestCode, resultCode, data);
		 if(resultCode==RESULT_OK){
		 if(requestCode == 0){   
			 String imageFileName = Integer.toString(utils.genRandomNum(100000));
			 File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
			 try {
				File image = File.createTempFile(imageFileName, ".jpg",storageDir);
				FileOutputStream fos = new FileOutputStream(image);
				((Bitmap)data.getExtras().get("data")).compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.close();
				uploadFile(image);
			}catch(IOException e) {
				e.printStackTrace();
			}
		 }else if(requestCode==1){
			 Uri uri = data.getData();
		        android.database.Cursor cursor = getContentResolver().query(uri,new String[]{MediaStore.Images.Media.DATA}, null, null, null);
		        cursor.moveToFirst();
		        int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
		        String picturePath = cursor.getString(columnIndex);
		        cursor.close();
	 	 		uploadFile(new File(picturePath));
		 }else{
			 Uri uri = data.getData();		       
			 try{
				 String[] proj = { MediaStore.MediaColumns.DATA, };
			     Cursor cursor = ctx.getContentResolver().query(uri,  proj, null, null, null);
			     int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
			     cursor.moveToFirst();
				 String path = cursor.getString(column_index);
				 cursor.close();
				 uploadFile(new File(path));
			 }catch(Exception err){
				 String path = uri.getPath();
				 uploadFile(new File(path));
			 }
			 
		 }
	 }
	}

	public void uploadFile(File file){
		 Editor edit = ctx.getSharedPreferences("com.x.cloudhub.upload", Context.MODE_PRIVATE).edit();
	 		edit.clear();
	 		edit.putBoolean("isFolder",false);
	 		edit.putString("name", file.getName());
	 		edit.putString("path", Uri.fromFile(file.getParentFile()).toString()+"/");
	 		edit.apply();
	 		upload(null);
	}
}
