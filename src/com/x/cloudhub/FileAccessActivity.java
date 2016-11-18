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
import android.content.IntentFilter;
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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
public class FileAccessActivity extends Activity{
	 Services s=new Services(this);
	 int notify_id;
  	 int service_id; String name,credentials,baseurl="",method="",email; 
	 Stack<String> previousIds;
	 Socket ftp_socket; 
	 String current_id; 
	 static Long expires;
	 ArrayList<Services.Item> items;static ItemAdapter item_adapter;TextView URLText;Bundle extras;
	 Services.Service service; Services.FileIO io;Services.FTPSocket ftpsock;ArrayList<String> children;
	 static boolean multiSelect = false;
	 static Notification notification;
	 static NotificationManager mNotifyManager;
	 IntentFilter filter = new IntentFilter();
	 public class ItemAdapter extends BaseAdapter {
		private ArrayList<Services.Item> internal_items;
	    public ItemAdapter(Context context, ArrayList<Services.Item> items) {
	    	this.internal_items = items;
	    }
		@Override
		public Services.Item getItem(int arg0) {
			return this.internal_items.get(arg0);
		}
		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}
	    public View getView(final int position, View convertView, ViewGroup parent) {
	    	Services.Item item = getItem(position);    
	       if (convertView == null) {convertView = LayoutInflater.from(FileAccessActivity.this).inflate(R.layout.list_items_view, parent, false);}
	       TextView txtName = (TextView) convertView.findViewById(R.id.name);
	       TextView txtId = (TextView) convertView.findViewById(R.id.id);
	       TextView txtQuota = (TextView) convertView.findViewById(R.id.quota);
	       ImageView service = (ImageView) convertView.findViewById(R.id.service);
	       CheckBox select = (CheckBox) convertView.findViewById(R.id.checkBox1);
	       txtName.setText(item.name); txtId.setText(item.type); 
	       txtQuota.setText(utils.bytes_in_h_format(item.size));
	       select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				getItem(position).selected = isChecked;
				if(anySelected(items)){
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
		public int getCount() {
			// TODO Auto-generated method stub
			return items.size();
		}
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
			  URLText = (TextView) findViewById(R.id.URLText);
			  setTitle(name);
			  service = s.new Service(method, service_id); io = s.new FileIO(method, service_id, baseurl, credentials);
			  /*
	        if(service_id==R.string.ftp){
	        	 URL u = s.new URL(baseurl);
	        	ftpsock =  s.new FTPSocket(u.host,8000,username,password,usrInfoText);
	        }
	       */		  
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
	        
	        curitems.setAdapter(item_adapter);
	        curitems2.setAdapter(item_adapter);
	        current_id = service.getPaths(baseurl).root;
			listDirectory(current_id,true);
			
	    }
	 public void back(View view){
		 current_id = previousIds.pop();
		 listDirectory(current_id,true);
		 if(previousIds.size()==0){
			 findViewById(R.id.back).setVisibility(View.GONE);
		 }
	 }
	 public void itemClicked(final int position){
		 if(!anySelected(items)){
		  String chosen_id = items.get(position).id;
		 	if(items.get(position).isFolder){
		 		previousIds.add(current_id);
				 current_id = chosen_id; listDirectory(current_id,true);
				 findViewById(R.id.back).setVisibility(View.VISIBLE);
			 }else{
				 String info = utils.getItemDetails(items, position);
			        new AlertDialog.Builder(FileAccessActivity.this)
			        .setTitle(items.get(position).name).setMessage(info)
			        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which){}})
			        .setNegativeButton("Download File", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which){
			            	downloadItem(position);
			            }})
			        .setIcon(android.R.drawable.ic_dialog_info)
			        .show();
			 }
		 }
	}
	 
	 public void deselect(View view){
		 multiSelect=false;
		 for(int i=0; i<items.size(); i++){
			 items.get(i).selected = multiSelect;
		 }
		 findViewById(R.id.actions).setVisibility(View.GONE);
		 findViewById(R.id.default1).setVisibility(View.VISIBLE);
		 item_adapter.notifyDataSetChanged();
	 }
	 public void btnReload(View view){
		listDirectory(current_id,true);
	 }
	 public void listDirectory(final String dir_id, final boolean includeFiles){
		 items.clear();
		 URLText.setText(service.getChildUrl(baseurl, dir_id));
		 findViewById(R.id.linearLayout4).setVisibility(View.VISIBLE);
		 Thread t = new Thread(){
			 public void run(){
			 children = s.new FileIO(method, service_id,baseurl,credentials).list_children(dir_id);
			 runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                	 for(int i=0; i<children.size(); i++){
        				 Services.Item item=s.new Item(method, service_id,children.get(i));
        				 if(includeFiles || item.isFolder){items.add(item);}
        			 }
                	 findViewById(R.id.linearLayout4).setVisibility(View.GONE);
                	 item_adapter.notifyDataSetChanged();
                 }});
			 
			 }
		 };
		 t.start();
		 
		
		 
	 }
	 
	 public void searchChildren(final String dir_id, final String keyword){
		 items.clear();
		 URLText.setText(service.getChildUrl(baseurl, dir_id));
		 findViewById(R.id.linearLayout4).setVisibility(View.VISIBLE);
		 Thread t = new Thread(){
			 public void run(){
		 children = s.new FileIO(method, service_id,baseurl,credentials).search_children(dir_id, keyword);
		 runOnUiThread(new Runnable() {
             @Override
             public void run() {
            	 for(int i=0; i<children.size(); i++){
    				 Services.Item item=s.new Item(method, service_id,children.get(i));
    				items.add(item);
    			 }
            	 findViewById(R.id.linearLayout4).setVisibility(View.GONE);
            	 item_adapter.notifyDataSetChanged();
             }});
		 
		 }
	 };
	 t.start();
	 }
	 
	 public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {  
	    	super.onCreateContextMenu(menu, v, menuInfo);  
	    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	     menu.setHeaderTitle("Options for " +items.get(info.position).name);
	     menu.add(0, v.getId(), 0, "Details");
	     menu.add(0, v.getId(), 0,"Download");
	     menu.add(0, v.getId(), 0, "Rename");
	     menu.add(0, v.getId(), 0, "Cut");
	     menu.add(0, v.getId(), 0, "Copy");
	    
	     if(method.equals("OAuth2")){
	    	 menu.add(0,v.getId(),0,"Edit");
	    	 menu.add(0, v.getId(), 0, "Send to Trash");
	     }
	     
	     if(service_id==R.string.google || service_id==R.string.box || method.equals("WebDAV")){
	    	 menu.add(0, v.getId(), 0, "Delete permanently");
	     }
	 }  
	 public boolean onContextItemSelected(MenuItem item) { 
	    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	        if(item.getTitle()=="Details"){viewDetails(info.position);} 
	        if(item.getTitle()=="Download"){downloadItem(info.position);} 
	        if(item.getTitle()=="Rename"){renameItem(info.position);}
	        if(item.getTitle()=="Cut"){storeItem(info.position, "move");}
	        if(item.getTitle()=="Copy"){storeItem(info.position,"copy");}
	        if(item.getTitle()=="Edit"){editItem(info.position);}
	        if(item.getTitle()=="Delete permanently"){deleteItem(info.position,true);}  
	        if(item.getTitle()=="Send to Trash"){deleteItem(info.position,false);}  
	        return true;  
	    }  
	 private void editItem(int position) {
		String content = children.get(position);
		String edit_url = utils.getProperty(method, content, "alternateLink");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Edit");
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
	       ProgressActivity.progress.setCancelable(true);
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
	 private void storeItem(int position, String string) {
		Services.Item item = items.get(position);
		Editor edit = this.getSharedPreferences("com.x.cloudhub.clipboard", Context.MODE_PRIVATE).edit();
		edit.clear();
		edit.putBoolean("isFolder",item.isFolder);
		edit.putString("name", item.name);
		edit.putString("id", item.id);
		edit.putString("parent_id", current_id);
		edit.putString("operation", string);
		edit.apply();
		findViewById(R.id.button1).setVisibility(View.VISIBLE);
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
   		    	new AlertDialog.Builder(FileAccessActivity.this)
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
	        		            	s.new ConfigureAccount().disconnectAccount(name);startActivity(new Intent(FileAccessActivity.this, MainActivity.class)); return;
	        		            }
	        		         })
	        		        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {dialog.cancel();}})
	        		        .setIcon(android.R.drawable.ic_dialog_alert)
	        		         .show();
	        			 return true;
	  	        	default: return super.onOptionsItemSelected(item);
	    	}
	    }
	
	 public void selectAll(View view){
		 multiSelect=true;
		 for(int i=0; i<items.size(); i++){
			 items.get(i).selected = multiSelect;
		 }
		 findViewById(R.id.actions).setVisibility(View.VISIBLE);
		 findViewById(R.id.default1).setVisibility(View.GONE);
		 item_adapter.notifyDataSetChanged();
	 }
	 public void toggleView(View view){
		 if(findViewById(R.id.curitems).getVisibility()==View.VISIBLE){
			 findViewById(R.id.curitems).setVisibility(View.GONE);
			 findViewById(R.id.curitems2).setVisibility(View.VISIBLE);
			 findViewById(R.id.grid_list).setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_view_list_white_48dp));
		 }else{
			 findViewById(R.id.curitems).setVisibility(View.VISIBLE);
			 findViewById(R.id.curitems2).setVisibility(View.GONE);
			 findViewById(R.id.grid_list).setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_view_module_white_48dp));
		 }
	 }
	 public void addFolder(View v){
		 final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Enter a name for the new folder: ");
	        final EditText input = new EditText(this);
	        input.setInputType(InputType.TYPE_CLASS_TEXT);
	        builder.setView(input);
	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	         	  String name = input.getText().toString();
	         	  String status =io.mkdir(current_id, name);
	         	 listDirectory(current_id,true);
	         	 Toast.makeText(FileAccessActivity.this, status.split("\r\n")[0], 2000).show();
	         	  }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
	        builder.show();
	 }	 
	 public void showUploadDialog(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 String[] options = {"Take photo", "Choose from gallery","Choose file/folder", "Choose from app"};
		 builder.setTitle("Upload options:");
		 builder.setItems(options, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, final int option) {
		        	switch(option){
		        		case 0: 
		        			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		        		    startActivityForResult(intent, 0);
		        			break;
		        		case 1: 
		        			Intent galleryIntent = new Intent(Intent.ACTION_PICK,
		        			        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		        			startActivityForResult(Intent.createChooser(galleryIntent, "Select File"), 1);
		        			break;
		        		
		        		case 2: 
		        			String state = Environment.getExternalStorageState();
		        		    if (Environment.MEDIA_MOUNTED.equals(state)) {
		        		    	File local_path = Environment.getExternalStorageDirectory();
		        		    	s.new FileChooser("upload", method, service_id, baseurl, credentials, current_id).chooseFile(local_path.toURI());
		        		    }
		        			break;
		        		case 3:
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
	            	sort(current_id, type, ascending);
	         	}

				
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
		 final AlertDialog alertDialog = builder.create();
		;
		 alertDialog.show();
		 
		 
	 }
	 public void showFilterDialog(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 builder.setTitle("Filter options:");
		 builder.setView(LayoutInflater.from(this).inflate(R.layout.filter_options, null));
		 builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	//String type = ((Spinner) ((AlertDialog) dialog).findViewById(R.id.sortType)).getSelectedItem().toString();	
	            	//boolean ascending = ((RadioButton) ((AlertDialog) dialog).findViewById(R.id.ascend)).isChecked();
	            	//sort(current_id, type, ascending);
	         	}

				
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
		 final AlertDialog alertDialog = builder.create();
		;
		 alertDialog.show();
		  
		 
	 }
	 public void bookmark(View v){
		 final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Enter a name for the new bookmark: ");
	        final EditText input = new EditText(this);
	        input.setInputType(InputType.TYPE_CLASS_TEXT);
	        builder.setView(input);
	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	SQLiteDatabase myDB = FileAccessActivity.this.openOrCreateDatabase("services.db", MODE_PRIVATE, null);
	            	myDB.execSQL("CREATE TABLE IF NOT EXISTS BOOKMARKS (SERVICE_NAME TEXT, NAME TEXT, ID TEXT)");
			    	ContentValues values = new ContentValues();
			    		values.put("SERVICE_NAME", name);
			    		values.put("NAME", input.getText().toString());
			    		values.put("ID", current_id);
			    	myDB.insert("BOOKMARKS",null,values);
			    	myDB.close();
	            	Toast.makeText(FileAccessActivity.this, "Bookmark added", 2000).show();
	         	  }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
	        builder.show();
	 }
	 public void showBookmarks(View v){
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 String[] options = {"Root folder", "Trash folder", "Shared folder","Bookmarks"};
		 builder.setItems(options, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, final int option) {
		        	switch(option){
		        		case 0: 
		        			previousIds.add(current_id);
		        			current_id = service.getPaths(baseurl).root;
		        			listDirectory(current_id,true);
		        			break;
		        		case 1: 
		        			previousIds.add(current_id);
		        			current_id = "#trash";
		        			listDirectory(current_id, true);
		        			break;
		        		case 2: 
		        			previousIds.add(current_id);
		        			current_id = "#shared";
		        			listDirectory(current_id, true);
		        			break;
		        		case 3: 
		        			AlertDialog.Builder builder2 = new AlertDialog.Builder(FileAccessActivity.this);
		        			final String[][] bookmarks = getBookmarksFromDB();
		        			if(bookmarks[0].length>0){
			        			builder2.setTitle("Choose a bookmark:");
			        			builder2.setItems(bookmarks[0], new DialogInterface.OnClickListener() {
			        		        public void onClick(DialogInterface dialog, final int option) {
			        		        	previousIds.add(current_id);
			        		        	current_id=bookmarks[1][option];
			        		        	listDirectory(current_id, true);
			        		        }
			        		    });
		        			
		        			final AlertDialog alert2 = builder2.create();
		        			alert2.show();
		        			}else{
		        				Toast.makeText(FileAccessActivity.this, "You have no bookmarks yet!", 1000).show();
		        			}
		        			break;
		        	}
		        }

				private String[][] getBookmarksFromDB() {
					SQLiteDatabase myDB = FileAccessActivity.this.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
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
					return data;
				}	
		    });
		 final AlertDialog alert = builder.create();
		 alert.show();
		 
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
	         	  String name = input.getText().toString();
	         	  searchChildren(current_id,name);
	         	  }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) { dialog.cancel();}
	        });
	        builder.show(); 
	 }

	 public void paste(View v){
		SharedPreferences sprefs =  this.getSharedPreferences("com.x.cloudhub.clipboard", Context.MODE_PRIVATE);
		 final boolean isFolder = sprefs.getBoolean("isFolder",false);
		 final String id = sprefs.getString("id", "");
		 final String parent_id = sprefs.getString("parent_id", "");
		 final String item_name = sprefs.getString("name", "");
		 final String op = sprefs.getString("operation","");
		 String action = "";
		if(op.equals("move")){action = "Moving"; }
		
		if(op.equals("copy")){action= "Copying"; ProgressActivity.progress=new ProgressDialog(this);}
		final ProgressDialog dialog = ProgressDialog.show(FileAccessActivity.this, "", action+". Please wait...", true);
		if(isFolder && op.equals("copy")){
			ProgressActivity.progress=new ProgressDialog(this);
			ProgressActivity.progress.setButton(ProgressDialog.BUTTON_POSITIVE, "Do in background", new DialogInterface.OnClickListener() {
	 	        @Override
	 	        public void onClick(DialogInterface dialog, int which) {ProgressActivity.progress.dismiss();}
	 	    });
			ProgressActivity.progress.setTitle("Copying "+item_name+". Please wait...");
			ProgressActivity.progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			ProgressActivity.progress.setMessage("Getting number of files");
			ProgressActivity.progress.setMax(0);
			ProgressActivity.progress.setIndeterminate(true);
			
		 
			FileAccessActivity.mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			FileAccessActivity.notification=new Notification(android.R.drawable.ic_menu_save, "Copying "+item_name, System.currentTimeMillis());
			FileAccessActivity.notification.flags = FileAccessActivity.notification.flags | Notification.FLAG_ONGOING_EVENT;
			FileAccessActivity.notification.setLatestEventInfo(FileAccessActivity.this, "Copying "+item_name+"...", "", 
			PendingIntent.getActivity(FileAccessActivity.this, utils.genRandomNum(100000),new Intent(FileAccessActivity.this,ProgressActivity.class),Intent.FLAG_ACTIVITY_CLEAR_TOP));
			FileAccessActivity.mNotifyManager.notify(notify_id,FileAccessActivity.notification);
			dialog.dismiss();
		}
	
		 Thread mThread = new Thread() {
			    @Override
			    public void run() {
		 if(op.equals("move")){
			  io.move(isFolder, id, item_name,parent_id,current_id);
		 }else if(op.equals("copy")){
			 if(isFolder){
				 ProgressActivity.progress.setMax(0);
				 io.getNumOfFiles(isFolder, id);
			 }
			 ProgressActivity.progress.setIndeterminate(false);
			  io.copy(isFolder, id, item_name, current_id);
			  ProgressActivity.progress.dismiss();
		      FileAccessActivity.mNotifyManager.cancel(notify_id);
		      String title ="Copied "+item_name; 
		    	Notification copy_done=new Notification(android.R.drawable.ic_menu_save, title, System.currentTimeMillis());
		    	copy_done.setLatestEventInfo(FileAccessActivity.this, title,"",
		    	PendingIntent.getActivity(FileAccessActivity.this, 0, new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
		    	copy_done.flags |= Notification.FLAG_AUTO_CANCEL; 
		    	
		    	FileAccessActivity.mNotifyManager.notify(title, 0, copy_done);
		 }
		 dialog.dismiss();
		 ProgressActivity.progress.dismiss();
		 runOnUiThread(new Runnable(){public void run(){listDirectory(current_id,true);}});
		}
		};
		mThread.start();
		
		 item_adapter.notifyDataSetChanged();
		 sprefs.edit().clear();
		 findViewById(R.id.button1).setVisibility(View.INVISIBLE);
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
			 FileAccessActivity.notification.setLatestEventInfo(FileAccessActivity.this, "Uploading "+item_name, "", 
					 PendingIntent.getActivity(FileAccessActivity.this, utils.genRandomNum(100000),new Intent(FileAccessActivity.this,ProgressActivity.class),Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
				    	upload_done.setLatestEventInfo(FileAccessActivity.this, title,"",
				    	PendingIntent.getActivity(FileAccessActivity.this, 0, new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
				    	upload_done.flags |= Notification.FLAG_AUTO_CANCEL; 
				    	
				    	FileAccessActivity.mNotifyManager.notify(title, 0, upload_done);
				    	
				    	runOnUiThread(new Runnable(){public void run(){
				    		listDirectory(current_id,true);
				    		 Toast.makeText(FileAccessActivity.this, a.split("\r\n")[0], 2000).show();
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
	 public void move(View v){
		 
	 }
	 
	 public void sort(String currentId, String type, boolean ascending) {
		Toast.makeText(this, currentId+" "+type+" "+Boolean.toString(ascending), 1000).show();	
	 }
	 
	 public void onBackPressed() {
		 startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY)); finish(); 
	 }
	 public boolean anySelected(ArrayList<Services.Item> items){
		 for(Services.Item item:items){
			 if(item.selected) return true;
		 }
		 return false;
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
 		 upload(null);
		 }else if(requestCode==1){
			 Uri uri = data.getData();
		        android.database.Cursor cursor = getContentResolver().query(uri,new String[]{MediaStore.Images.Media.DATA}, null, null, null);
		        cursor.moveToFirst();
		        int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
		        String picturePath = cursor.getString(columnIndex);
		        cursor.close();
		        System.out.println(picturePath);
	 	 		uploadFile(new File(picturePath));
		 }else{
			 Uri uri = data.getData();		       
			 try{
				 String[] proj = { MediaStore.MediaColumns.DATA, };
			     Cursor cursor = FileAccessActivity.this.getContentResolver().query(uri,  proj, null, null, null);
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
		 Editor edit = FileAccessActivity.this.getSharedPreferences("com.x.cloudhub.upload", Context.MODE_PRIVATE).edit();
	 		edit.clear();
	 		edit.putBoolean("isFolder",false);
	 		edit.putString("name", file.getName());
	 		edit.putString("path", Uri.fromFile(file.getParentFile()).toString()+"/");
	 		edit.apply();
	 		upload(null);
	 		
	}
}
/*
Empty google trash: 	DELETE  /files/trash	
WebDAV
PUT - Upload/Modify file
 */