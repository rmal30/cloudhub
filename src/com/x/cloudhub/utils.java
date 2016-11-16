package com.x.cloudhub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.x.cloudhub.Services.Item;

public class utils {
	static public int genRandomNum(int limit){
		 return (int) Math.floor(Math.random()*limit);
	 }
	static public String bytes_in_h_format(String string){
		if(string==null){return "null";}
		try{return bytes_in_h_format(Long.valueOf(string));}catch(Exception e){return "";}
	}
	static public String bytes_in_h_format(Long string){
			String binbytes =Long.toString(string,2);
			int lenbytes = binbytes.length();
			int category=Integer.toString(lenbytes-1).toCharArray()[0]-'0';
			if(lenbytes-1<10){category=0;}
			String[] SI ={"","K","M","G","T","P"};
			return Integer.toString(Integer.valueOf(binbytes.substring(0,lenbytes-10*category),2))+" "+SI[category]+"B";
		}	
	static public boolean tokenExpired(Long expires){
		return System.currentTimeMillis()+10000>expires && expires!=0;
	}
	static public byte[] strToBytes(String str){
		try{
		return str.getBytes("UTF-8");
		}catch(Exception e){
			e.printStackTrace();
			return new byte[0];
		}
	}
	static public int getTotalLocalSize(File file){
		if(file.isDirectory()){
			int cur = 0;
			File[] children = file.listFiles();
			for(File child:children){
				cur+=getTotalLocalSize(child);
			}	
			return cur;
		}else{
			ProgressActivity.progress.setMax(ProgressActivity.progress.getMax()+(int)file.length());
			return (int) file.length();
		}
	}
	static public void open(File file, Context ctx){
		if(file.isDirectory()){
			 new Services(ctx).new FileChooser("open").chooseFile(file.toURI());
		 }else{
			 try{
				 Intent i = new Intent(Intent.ACTION_VIEW);
				 String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension( MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString()));
				 i.setDataAndType(Uri.fromFile(file),mimeType);
				 ctx.startActivity(i);
				 }catch(Exception e){
					e.printStackTrace();
					 Toast.makeText(ctx, "File not recognised", 2000).show();
				 }
		 }
	}
	static public String findPassportUrl(String credentials, Context ctx){
		SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
		Cursor select = myDB.query("ACCOUNTS",new String[]{"URL"},"CREDENTIALS='"+credentials.replaceAll("'","''")+"'",null,null,null,null);
		select.moveToPosition(0);
		return select.getString(0);
	}
	
	static public String getHeaderValue(ArrayList<String> headers, String header_name){
 		for(String header:headers){
 			if(header.contains(header_name)){
 				return header.split(header_name+": ")[1].split("\n")[0];
 			}
 		}
 		return "0";
 	}
	static public String Header(String header, String value){
		return header+": "+value;
	}
	static public String getItemDetails(ArrayList<Item> items,int position){
	 String info="";
     info = info.concat("ID:"+items.get(position).id+"\n");
     info = info.concat("Type: "+items.get(position).type+"\n");
     if(!items.get(position).isFolder){
  	   info = info.concat("Size: "+utils.bytes_in_h_format(items.get(position).size)+"\n");
     }
     info = info.concat("Date created: "+items.get(position).created+"\n");
     info = info.concat("Date modified: "+items.get(position).modified+"\n");
	return info;
	}
	
	static public int getProtoLayout(int service_id){
		switch(service_id){
			case R.string.webdav: return R.layout.protocol_webdav;
			case R.string.sftp: return R.layout.protocol_sftp;
			case R.string.smb: return R.layout.protocol_smb;
			case R.string.nfs: return R.layout.protocol_nfs;
			case R.string.ftp: return R.layout.protocol_ftp;
			
			case R.string.microsoft: return R.layout.webview;
			case R.string.box: return R.layout.protocol_webdav;
		}
		return 0;
	}
	static public int getStrId(String service, Context ctx) {		
		int[] service_ids = {R.string.google, R.string.microsoft, R.string.dropbox, R.string.box, R.string.ftp, R.string.webdav,R.string.smb,R.string.nfs,R.string.sftp};
		for(int i=0; i<service_ids.length; i++){
			if(service.equals(ctx.getResources().getString(service_ids[i]))){
				return service_ids[i];
			}
		}
		return 0;
	}
	
	static public void clearWebViewDatabases(Context ctx){
		 ctx.deleteDatabase("webview.db");ctx.deleteDatabase("webview.db-wal");ctx.deleteDatabase("webview.db-shm");
	     ctx.deleteDatabase("webviewCache.db");ctx.deleteDatabase("webviewCache.db-wal");ctx.deleteDatabase("webviewCache.db-shm");	
	}
	static public String getProperty(String method,String content, String property){
		if(method.equals("OAuth2")){
			
			if(content==null || content.equals("-")){return "-";}
			try {
				return new JSONObject(content).optString(property, "-");
			} catch (JSONException e) {
				e.printStackTrace();
				return "-";
			}
			
		}else if(method.equals("WebDAV")){
			try{
			int end;
			 content = content.replaceAll("D:"+property, "d:"+property);
			 String begbeg = "<d:"+property; String endbeg = ">";
			 if(content.indexOf(begbeg)==-1){return null;}
			 String bcontent = content.substring(content.indexOf(begbeg));
			 String marker = bcontent.substring(0,bcontent.indexOf(endbeg)+1);
			 end = bcontent.indexOf("</d:"+property+">");	
			 if(end==-1){return null;}
		     return bcontent.substring(marker.length(), end);
			}catch(Exception e){
				return "";
			}
		}
		else{return "0";}
	}
	
	static public byte[] newPart(String separator,ArrayList<String> headers, byte[] content){
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try {
			baos.write(("--"+separator+"\r\n").getBytes("UTF-8"));
			for(int i=0; i<headers.size(); i++){baos.write((headers.get(i)+"\r\n").getBytes("UTF-8"));}
			baos.write(("\r\n").getBytes("UTF-8"));
			baos.write(content);
			baos.write(("\r\n").getBytes("UTF-8"));
			return baos.toByteArray();
	} catch (Exception e){
		e.printStackTrace();
	}
		return content;
}
	static public String[] getServices(Context ctx){
		 SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
	        myDB.execSQL("CREATE TABLE IF NOT EXISTS ACCOUNTS (NAME TEXT,METHOD VARCHAR(15),SERVICE VARCHAR(30),URL TEXT,EMAIL TEXT,CREDENTIALS TEXT, EXPIRES INTEGER)");
	        int num_rows = myDB.rawQuery("SELECT * FROM ACCOUNTS",null).getCount();
	        String[] services = new String[num_rows]; 
	        Cursor selection = myDB.query("ACCOUNTS",new String[]{"NAME"},null,null,null,null,null);
	        for(int i=0; i<num_rows; i++){
	        	selection.moveToPosition(i);
	        	services[i] = selection.getString(0);
	        }
	        myDB.close();
	        return services;
	}
}
