package com.x.cloudhub;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Base64;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
public class Services extends Activity{
	private Context ctx;
	public Services(Context context){this.ctx=context;}
	public class OAuth2{
	int service_id;
		public OAuth2(int service_id){super();this.service_id = service_id;}
		public String getSetupParams(){
		String google_oauth_params="&access_type=offline&prompt=consent";
		switch(this.service_id){
			case R.string.google: return google_oauth_params;
			default: return "";
		}
	}
		public int getScope(){
		switch(this.service_id){
			case R.string.google: return R.string.google_scopes;
			case R.string.microsoft: return R.string.ms_scopes;
			case R.string.dropbox: return R.string.dropbox_scopes;
			case R.string.box: return R.string.box_scopes;
			case R.string.amazon: return R.string.amazon_scopes;
		}
		return 0;
	}
		public String getClientId(){
		String google_client_id = "752718244781-bqhve4dh6v8rqlu9d5kct95jia72egno.apps.googleusercontent.com";
		String ms_client_id = "000000004C179037";
		String amazon_client_id="amzn1.application-oa2-client.209f7fba149f4e1ebe4c0d1a6ef3994c";
		String dropbox_client_id="81plkz4moujp39b";
		String box_client_id="l3rs1cd179pez1ihkkxi8u74sta9lxu6";
		switch(this.service_id){
			case R.string.google: return google_client_id;
			case R.string.microsoft: return ms_client_id;
			case R.string.dropbox: return dropbox_client_id;
			case R.string.box: return box_client_id;
			case R.string.amazon: return amazon_client_id;
		}
		return "";
		
	}
		public String getClientSecret() {
 		char[] ms_secret = "gFIsIktzB2dLdAcRisLX4-DjY0ft21-v".toCharArray();
 		char[] dropbox_secret="beootlvbomv3amm".toCharArray();
 		char[] box_secret="ibifd462eza4JrFiSzsF3eGCMFbnOZoN".toCharArray();
 		//char[] amazon_secret = "4330dbeb2ec51f117ecb9e9bb63c290173292c52b405de60a5c080e09e4cbdfc".toCharArray();
 		char[] google_secret ="V71B8y5PuF6QXaUEE1GKtw7r".toCharArray();
		switch(this.service_id){
			case R.string.google: return new String(google_secret);
			case R.string.microsoft: return new String(ms_secret);
			case R.string.dropbox: return new String(dropbox_secret);
			case R.string.box: return new String(box_secret);
			//case R.string.amazon: return new String(amazon_secret);		
		}
		return "";
	}
		public int getUrl(int stage){
		switch(this.service_id*10+stage){
			case R.string.google*10+1: return R.string.url_oauth_google_1;
			case R.string.google*10+2:return R.string.url_oauth_google_2;
			case R.string.google*10+3:return R.string.url_rest_google;
			case R.string.microsoft*10+1: return R.string.url_oauth_ms_1;
			case R.string.microsoft*10+2:return R.string.url_oauth_ms_2;
			case R.string.microsoft*10+3:return R.string.url_rest_ms;
			case R.string.dropbox*10+1:return R.string.url_oauth_dropbox_1;
			case R.string.dropbox*10+2:return R.string.url_oauth_dropbox_2;
			case R.string.dropbox*10+3:return R.string.url_rest_dropbox;
			case R.string.box*10+1:return R.string.url_oauth_box_1;
			case R.string.box*10+2:return R.string.url_oauth_box_2;
			case R.string.box*10+3:return R.string.url_rest_box;
			//case R.string.amazon*10+1:return R.string.url_oauth_amazon_1;
			//case R.string.amazon*10+2:return R.string.url_oauth_amazon_2;
			//case R.string.amazon*10+3:return R.string.url_rest_amazon;
		}
		return 0;
	}
		public String getUserInfoPath(){
		switch(service_id){
			case R.string.google: return "about";
			case R.string.microsoft: return "";
			case R.string.dropbox: return "account/info";
			case R.string.box: return "users/me";
			//case R.string.amazon: return "account/endpoint";
		}
		return "";
	}
		public boolean tokenExpired(String access_token){
			
 		if(service_id==R.string.dropbox){return false;}
 		String url = ctx.getResources().getString(this.getUrl(3))+this.getUserInfoPath();
		 ArrayList<String> headers=new ArrayList<String>();
		 headers.add("Authorization: Bearer "+access_token);
		 if(new HTTPRequest("GET",url, headers,new byte[0]).getStatus().contains("401")){return true;}
		 return false;
	}
		public String getNewTokens(String credentials){
		if(service_id==R.string.dropbox){return credentials.split("&&")[1];}
		String grant_type="refresh_token";
		HttpClient client = new DefaultHttpClient();
	    HttpPost post = new HttpPost(ctx.getResources().getString(this.getUrl(2)));
	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("grant_type", grant_type));
        params.add(new BasicNameValuePair("client_id", this.getClientId()));
        params.add(new BasicNameValuePair("client_secret", this.getClientSecret()));
        params.add(new BasicNameValuePair("refresh_token", credentials.split("&&")[1]));
        UrlEncodedFormEntity ent;
		try {
			ent = new UrlEncodedFormEntity(params,HTTP.UTF_8);
			 post.setEntity(ent);
			 HttpEntity resEntity = client.execute(post).getEntity();
			 String json_str = EntityUtils.toString(resEntity);
			 String json_str2 = json_str.substring(json_str.indexOf("{"), json_str.lastIndexOf("}") + 1);
			 System.out.println(json_str2);
			 JSONObject response = new JSONObject(new String(json_str2));
			 String access_token = response.getString("access_token");
			 String refresh_token = credentials.split("&&")[1];
			 if(service_id==R.string.box){refresh_token = response.getString("refresh_token");}
			 SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
			 Long expires = System.currentTimeMillis()+3600*1000;
			 myDB.execSQL("UPDATE ACCOUNTS SET CREDENTIALS='"+access_token+"&&"+refresh_token+"' WHERE CREDENTIALS='"+credentials+"'");
			 myDB.execSQL("UPDATE ACCOUNTS SET EXPIRES='"+expires+"' WHERE CREDENTIALS='"+credentials+"'");
			 myDB.close();
			 FileAccessActivity.expires = expires;
			 return access_token+"&&"+refresh_token;
		} catch (Exception e) {e.printStackTrace();return e.toString();}  
	}
		public String getMsEmail(String credentials){
 		ArrayList<String> headers=new ArrayList<String>(Arrays.asList(new Service("OAuth2",service_id).useAuthHeader(credentials)));
 		
		try {
			String response=new HTTPRequest("GET","https://apis.live.net:443/v5.0/me", headers,new byte[0]).getContent();
			String str_live_details = response;
	       	JSONObject details;
			details = new JSONObject(new String(str_live_details));
			return details.getJSONObject("emails").getString("account");
		} catch (Exception e) {
			e.printStackTrace();
			return "Error";
		}
 	}
	}
	public class Item{
		String name,id,type,size,modified,created; Boolean isFolder;
		public Item(){super();}
		public Item(String name, String id, String type, String size, String modified, String created, Boolean isFolder){
			super();this.name=name; this.id=id; this.type=type; this.size=size; this.modified = modified; this.created=created; this.isFolder =isFolder;
		}
		public Item(String method, int service_id,String content){
			super();
			Service service = new Service(method, service_id);
			 this.isFolder = service.is_folder(content);
			 Services.FilePropertySet pns=service.getNameSet();
			 if(method.equals("OAuth2")){
				 String[] levels = utils.getProperty(method,content,pns.name).split("/");
				 this.id = utils.getProperty(method,content,pns.id);
				 String type = utils.getProperty(method,content,pns.type);
				 if(service_id==R.string.microsoft){
					 type = utils.getProperty(method,utils.getProperty(method,content,"file"),pns.type);
				 }
				 if(type.equals("-")){type="folder";}
				 this.name=levels[levels.length-1];
				 this.type=type;
			 }else if(method.equals("WebDAV")){
				 if(!this.isFolder){this.type= utils.getProperty(method,content,pns.type);	
				 }else{this.type="folder";}
				 String[] levels = new URL(utils.getProperty(method,content,pns.name)).path.split("/");
				 this.name=URLDecoder.decode(levels[levels.length-1]);
				 this.id = new URL(utils.getProperty(method,content,pns.name)).path;
			 }
			 this.isFolder = service.is_folder(content);
			 this.created = utils.getProperty(method,content, pns.created);
			 this.modified = utils.getProperty(method,content, pns.modified);
			 if(!this.isFolder){
				 this.size = utils.getProperty(method,content, pns.size);
			 }else{this.size="0";}
		}
		public Item rename(String new_name){
			this.name=new_name;
			return this;
		}
	}
 	public class FTPSocket{	
	Socket sock;PrintWriter pw; BufferedReader input;
	public FTPSocket(String host, int port, String username, String password, TextView usrInfoText){	  
	 try{
		 this.sock = new Socket(host,port);
		 this.pw = new PrintWriter(this.sock.getOutputStream());
		 this.input = new BufferedReader(new InputStreamReader(this.sock.getInputStream())); 
		 this.pw.print("USER "+username+"\r\n");
		 this.pw.flush();
		 usrInfoText.append(this.input.readLine()+"\n");
		 this.pw.print("PASS "+password+"\r\n");
		 this.pw.flush();
		 usrInfoText.append(this.input.readLine()+"\n");
		 usrInfoText.append(this.input.readLine()+"\n");
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	}
	public void runFTPcommand(String command, TextView usrInfoText){
		 try{
			this.pw.print(command+"\r\n");
			this.pw.flush();
			String line = this.input.readLine();
			usrInfoText.append(line+"\n");
			if(line.charAt(3)=='-'){
				String code=line.substring(0,2);
				 while((line = this.input.readLine())!=null && !("#"+line).contains("#"+code+" ")){
					 usrInfoText.append(line+"\n");
				 }
			}
		    }catch(Exception e){e.printStackTrace();}
	}
}	
 	public class HTTPRequest{
 		int size = 131072; BufferedInputStream in; byte[] buffer = new byte[size]; int c;
 		long before=System.currentTimeMillis(); OutputStream query;
		Socket sock; SSLSocket sslsock;SSLSocketFactory sslfactory;SocketFactory factory;
		ArrayList<String> res_headers;
		String res_text; String res_status;
		int error = 0;
		byte[] res_file;		
 		public HTTPRequest(String HTTPVerb,String url, ArrayList<String> headers, byte[] body){
 			super();
 			String enc = "UTF-8";
 			String crlf = "\r\n";
 			URL u = new URL(url);
 			 if (android.os.Build.VERSION.SDK_INT > 9) {
 				 StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
 				 StrictMode.setThreadPolicy(policy); 
 				 }
 			if(u.port.equals(0)){u.port=new Integer(443);}
 		try{	
 			if(u.protocol.equals("https")){
				this.sslfactory=(SSLSocketFactory) SSLSocketFactory.getDefault();
				this.sslsock=(SSLSocket) this.sslfactory.createSocket(InetAddress.getByName(u.host), u.port);
				this.query = new BufferedOutputStream(this.sslsock.getOutputStream());
				this.sslsock.setTcpNoDelay(true);
				this.in =  new BufferedInputStream(this.sslsock.getInputStream(),size);
			}else{
				this.factory = (SocketFactory)SocketFactory.getDefault();
				this.sock = (Socket) this.factory.createSocket(InetAddress.getByName(u.host),u.port);
				this.query = new BufferedOutputStream(this.sock.getOutputStream());
				this.sock.setTcpNoDelay(true);
				this.in =  new BufferedInputStream(this.sock.getInputStream(),size);
			}
 			
 			this.query.write((HTTPVerb+" "+u.path+"?"+u.params+" "+"HTTP/1.1"+crlf).getBytes(enc));
 			this.query.write(("Host: "+u.host+crlf).getBytes(enc));
 			for(int i=0; i<headers.size(); i++){
 				this.query.write((headers.get(i)+crlf).getBytes(enc));
 				}
 		
 			this.query.write(("Content-Length: "+String.valueOf(body.length)+crlf).getBytes(enc));
 			this.query.write(("Connection: close"+crlf).getBytes(enc));
 			this.query.write(crlf.getBytes(enc));
 			if(body.length>0){
 				this.query.write(body);
 			}
 			this.query.write(crlf.getBytes(enc));
 			this.query.flush();
 			
 		} catch (Exception e) {
 		 	this.error = 1;
 		}
 		}
 		public HTTPRequest(String HTTPVerb,String url, ArrayList<String> headers){
 			super();
 			String enc = "UTF-8";
 			String crlf = "\r\n";
 			URL u = new URL(url);
 			 if (android.os.Build.VERSION.SDK_INT > 9) {
 				 StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
 				 StrictMode.setThreadPolicy(policy); 
 				 }
 			if(u.port.equals(0)){u.port=new Integer(443);}
 		try{	
 			if(u.protocol.equals("https")){
				this.sslfactory=(SSLSocketFactory) SSLSocketFactory.getDefault();
				this.sslsock=(SSLSocket) this.sslfactory.createSocket(InetAddress.getByName(u.host), u.port);
				this.query = new BufferedOutputStream(this.sslsock.getOutputStream());
				this.sslsock.setTcpNoDelay(true);
				this.in =  new BufferedInputStream(this.sslsock.getInputStream(),size);
			}else{
				this.factory = (SocketFactory)SocketFactory.getDefault();
				this.sock = (Socket) this.factory.createSocket(InetAddress.getByName(u.host),u.port);
				this.query = new BufferedOutputStream(this.sock.getOutputStream());
				this.sock.setTcpNoDelay(true);
				this.in =  new BufferedInputStream(this.sock.getInputStream(),size);
			}
 			
 			this.query.write((HTTPVerb+" "+u.path+"?"+u.params+" "+"HTTP/1.1"+crlf).getBytes(enc));
 			this.query.write(("Host: "+u.host+crlf).getBytes(enc));
 			for(int i=0; i<headers.size(); i++){
 				this.query.write((headers.get(i)+crlf).getBytes(enc));
 				}
 			this.query.write(("Connection: close"+crlf).getBytes(enc));
 			this.query.write(crlf.getBytes(enc));
 		} catch (Exception e) {
 		 	this.error = 1;
 		}
 		}
 		public HTTPRequest putChunk(byte[] chunk){
 			try {
 				int remaining = chunk.length;	
 				while(remaining>0){
 					if(remaining>size){
 						System.arraycopy(chunk, chunk.length - remaining, buffer, 0, size);
 						this.query.write(buffer);
 						ProgressActivity.progress.incrementProgressBy(size);
 						remaining-=size;
 					}else{
 						byte[] remainder  = new byte[remaining];
 						System.arraycopy(chunk, chunk.length - remaining, remainder, 0, remaining);
						this.query.write(remainder);
						ProgressActivity.progress.incrementProgressBy(remaining);
						remaining=0;
 					}	
 				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return this;
 		}
 		public HTTPRequest send(){
 			try {
				this.query.write("\r\n".getBytes("UTF-8"));
				this.query.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return this;
 		}
 		public String getStatus(){	
 			if(this.res_status==null){
 					ByteArrayOutputStream baos= new ByteArrayOutputStream(50); Byte[] key = {13,10};int count=0;
 	 				try{
 	 					while ((c = this.in.read()) != -1 && count<2) {baos.write(c);if(key[count]==c){count++;}else{count=0;}}
 	 					this.res_status = baos.toString("UTF-8");
 	 					baos.close();  
 				}catch (Exception e) {e.printStackTrace();this.res_status="";}
 			}
 			return this.res_status;
 		}
 		public ArrayList<String> getHeaders(){
 			if(this.res_status==null){this.res_status = this.getStatus();}
 			if(this.res_headers==null){
 					ByteArrayOutputStream baos= new ByteArrayOutputStream(500);
 					int count=0; Byte[] key = {13,10,13,10};
 	 				try{
 	 					while ((c = this.in.read()) != -1) {
 	 						baos.write(c); if(key[count]==c){count++;if(count==key.length){break;}}else{count=0;}
 	 					}
 	 					this.res_headers = new ArrayList<String>(Arrays.asList(baos.toString("UTF-8").split("\r\n")));
 	 					baos.close();  
 				}catch (Exception e) {e.printStackTrace();this.res_headers=new ArrayList<String>();}
 			}
 			return this.res_headers;
 		}
 		public String getContent(){
 			if(this.res_headers==null){this.res_headers = this.getHeaders();}
 			if(this.res_text==null){
 				ByteArrayOutputStream baos= new ByteArrayOutputStream(size*4); int count=0;
 				try{
 					while ((count = this.in.read(buffer)) != -1) {baos.write(buffer, 0, count);}
 					this.res_text = baos.toString("UTF-8");
 					baos.close();   
 				}catch(Exception e){
 					e.printStackTrace();
 				}
 			}
 			return this.res_text;
 		}
 		public byte[] getFileChunk(int chunk_size, int remaining, int done, int total, int notify_id){
 			if(this.res_headers==null){this.res_headers = this.getHeaders();}
 			byte[] chunk;
 			int min;
 			if(remaining<chunk_size){
 				min = remaining;
 			}else{
 				min = chunk_size;
 			}
 			int min_remaining = min;
 			chunk = new byte[min];
 				try{
 					int count; int big_count=0; byte[] mini_buffer = new byte[size];
					while (min_remaining>size && (count = this.in.read(mini_buffer)) != -1){
						System.arraycopy(mini_buffer, 0, chunk, min-min_remaining, count);
 						min_remaining-=count;
 						ProgressActivity.progress.incrementProgressBy(count);
 						done+=count;
 						big_count+=count;
 						if(big_count>total/10){
 							//FileAccessActivity.notification.contentView.setProgressBar(R.id.progBarStatus, total, done, false);
 							//FileAccessActivity.mNotifyManager.notify(notify_id, FileAccessActivity.notification);
 							big_count=0;
 						}
 					}
					big_count=0;
					byte[] remainder_bytes = new byte[min_remaining];
					while (min_remaining>0 && (count = this.in.read(remainder_bytes)) != -1){
						System.arraycopy(remainder_bytes, 0, chunk, min-min_remaining, count);
 						min_remaining-=count;
 						remainder_bytes = new byte[min_remaining];
 						ProgressActivity.progress.incrementProgressBy(count);
 						done+=count; big_count+=count;
 						
 						if(big_count>total/10){
 							//FileAccessActivity.notification.contentView.setProgressBar(R.id.progBarStatus, total, done, false);
 							//FileAccessActivity.mNotifyManager.notify(notify_id, FileAccessActivity.notification);
 							big_count=0;
 						}
 					}
					//FileAccessActivity.mNotifyManager.notify(notify_id, FileAccessActivity.notification);
 					return chunk;
 				}catch(IOException e){
 					e.printStackTrace();
 					return new byte[0];
 				}
 			
 		}
 	
 	}
	public class doWebDAVRequest{
		private ArrayList<String> headers;
		public doWebDAVRequest(String protocol,String host_str, int port, String auth_header){
		super();
		this.headers= new ArrayList<String>(Arrays.asList(auth_header));
	}
		public ArrayList<String> copy(String url, String dest, boolean overwrite){
		String overwrite_flag;
		if(overwrite){overwrite_flag="T";}
		else{overwrite_flag="F";}
		headers.add("Destination: "+ dest);
		headers.add("Overwrite: "+overwrite_flag);
		return new HTTPRequest("COPY",url, headers,new byte[0]).getHeaders();
	}
		public ArrayList<String> upload(String url, String type, int length){
		headers.add("Content-Type: "+type);
		headers.add("Content-Length: "+length);
		return new HTTPRequest("PUT",url,headers,new byte[0]).getHeaders();
	}
	}
	public class URL{
		public Integer port=0; public String protocol, host, path, params="";
		public URL(String protocol, String host, Integer port, String path, String params){
			super();this.protocol = protocol;this.host = host;this.port = port;this.path = path;this.params = params;
		}
		public URL(String url){
			super();
			if(url.endsWith("/")){url = url.substring(0,url.length()-1);}
			if(url.contains("://")){
				this.protocol = url.split("://")[0];
				this.host = url.split("://")[1].split("/")[0].split(":")[0];
				if(url.split("://")[1].split("/")[0].contains(":")){
					this.port = Integer.parseInt(url.split("://")[1].split("/")[0].split(":")[1]);
					this.path = url.replace(this.protocol+"://"+this.host+":"+this.port, "").split("\\?")[0];
				}else{
					this.port=443;
					this.path = url.replace(this.protocol+"://"+this.host, "").split("\\?")[0];
				}
			}else{
				this.host = url.split("/")[0].split(":")[0];
				if(url.split("/")[0].contains(":")){
					this.port = Integer.parseInt(url.split("/")[0].split(":")[1]);
					this.path = url.replace(this.host+":"+this.port, "").split("\\?")[0];
				}else{
					this.port=443;
					this.path = url.replace(this.host, "").split("\\?")[0];
				}
			}
			if(url.startsWith("/")){this.path = url.split("\\?")[0];}
			if(url.contains("?")){
				this.params = url.split("\\?")[1];
			}
		}
		public String getURLString(){
		String[] marker = {"://",":","?"};
		String strport=String.valueOf((Object) this.port);
		if(this.protocol.equals("")){marker[0]="";}
		if(this.port.equals(0)){marker[1]=""; strport="";}
		if(this.params.equals("")){marker[2]="";}
		return this.protocol+marker[0]+this.host+marker[1]+strport+this.path+marker[2]+this.params;
	}
}	
	public class AccountDetails{
		String service,id,url,email,quota_used, quota_total,person_name;
		public AccountDetails(String service, String id,String url,String person_name,String email,String quota_used, String quota_total){
			super();this.service = service; this.id=id; this.url=url; this.email=email; this.quota_used=quota_used; this.quota_total=quota_total;this.person_name=person_name; 
		}
 		public AccountDetails(String method, int service_id, String url, String credentials){
			super();
			ArrayList<String> headers;
			 headers=new ArrayList<String>();
			 if(method.equals("WebDAV") && service_id==R.string.microsoft){
				 headers.add(new Service(method,service_id).useAuthHeader(credentials,url));
			 }else{
				 headers.add(new Service(method,service_id).useAuthHeader(credentials));
			 }
			 
			 this.service = ctx.getResources().getString(service_id);
			 this.url=url;
			if(method.equals("WebDAV")){
				this.id = url;this.email="";this.person_name="";
				HTTPRequest request=new HTTPRequest("PROPFIND",url,headers,new byte[0]);
				this.quota_used = utils.getProperty(method,request.getContent(),"quota-used-bytes");
				try{
				this.quota_total = String.valueOf(Long.valueOf(utils.getProperty(method,request.res_text,"quota-available-bytes"))+Long.valueOf(quota_used));
				}catch(Exception e){
					this.quota_total=null;
				}
			}else if(method.equals("OAuth2")){
			    try {
			    	OAuth2 o = new OAuth2(service_id);
					String json_str2 =new HTTPRequest("GET",ctx.getResources().getString(o.getUrl(3))+o.getUserInfoPath(), headers,new byte[0]).getContent(); 
					json_str2 = json_str2.substring(json_str2.indexOf("{"), json_str2.lastIndexOf("}") + 1);
					JSONObject about = new JSONObject(new String(json_str2));
			       switch(service_id){
				       case R.string.google:
				    	    this.id = about.getJSONObject("user").getString("emailAddress");
				       		this.person_name = about.getString("name");
				       		this.quota_used = about.getString("quotaBytesUsed"); 
				       		this.quota_total= about.getString("quotaBytesTotal");
				       		break;
				       case R.string.microsoft:
				    	    this.id = o.getMsEmail(credentials);
				       		this.person_name = about.getJSONObject("owner").getJSONObject("user").getString("displayName");
				       		this.quota_used = about.getJSONObject("quota").getString("used"); 
				       		this.quota_total=about.getJSONObject("quota").getString("total");
				       		break;
				       case R.string.dropbox:
				    	    this.id = about.getString("email");
				       		this.person_name=about.getString("display_name");
							this.quota_used = String.valueOf(about.getJSONObject("quota_info").getLong("normal")); 
							this.quota_total = String.valueOf(about.getJSONObject("quota_info").getLong("quota"));
							break;
				       case R.string.box:
							this.id = about.getString("login");
							this.person_name = about.getString("name");
							this.quota_used = about.getString("space_used"); 
							this.quota_total =about.getString("space_amount");
							break;
			       }
			       this.email=this.id;
			    } catch (Exception e) {e.printStackTrace();}  	   
			}
		}
	}
 	public class FilePropertySet{
		String name,id,type,created,modified,size,folder_type;
		public FilePropertySet(String name, String id, String type, String created, String modified, String size,String folder_type){
			this.name = name;this.id = id;this.type = type;
			this.created = created;this.modified = modified;
			this.size = size;this.folder_type=folder_type;
		}
	}
 	public class Service{
		
		String method; int service_id; 
		public Service(String method, int service_id){
			super(); this.method = method; this.service_id = service_id;
		}
		public FilePropertySet getNameSet(){
			if(this.method.equals("OAuth2")){
				 switch(this.service_id){
				 	case R.string.google: return new FilePropertySet("title","id","mimeType","createdDate","modifiedDate","quotaBytesUsed",""); 
				 	case R.string.microsoft: return new FilePropertySet("name","id","mimeType","createdDateTime","lastModifiedDateTime","size","folder"); 
				 	case R.string.box: return new FilePropertySet("name","id","type","created_at","modified_at","size","");
				 	case R.string.dropbox: return new FilePropertySet("path","path","mime_type","client_mtime","modified","bytes","");
				 }
			}else if(this.method.equals("WebDAV")){
				return new FilePropertySet("href","href","getcontenttype","creationdate","getlastmodified","getcontentlength","resourcetype");
			}
			return null;
		}
		public String getChildUrl(String baseurl,String id){
			Path p = getPaths(baseurl);
			if(method.equals("OAuth2")){
				String params="";
				switch(service_id){
					case R.string.google: return baseurl+p.drive+"?q="+ "'"+id+"'+in+parents+and+trashed=false";
					case R.string.dropbox: return baseurl+"metadata/"+p.drive+id;
					case R.string.box: params = "?fields=type,id,name,created_at,size,modified_at";
				}
				return baseurl+p.drive+"/"+id+"/"+p.child+params;
			}else{
				URL u = new URL(baseurl);
				return new URL(u.protocol, u.host, u.port, "", "").getURLString()+id;
			}
		}	
		public boolean is_folder(String content){
		Services.FilePropertySet pns=getNameSet();
		try {
			if(method.equals("OAuth2")){
			switch(service_id){
		 		case R.string.dropbox: return new JSONObject(new String(content)).optBoolean("is_dir");
		 		case R.string.google: return utils.getProperty(method,content,pns.type).equals("application/vnd.google-apps.folder");
		 		case R.string.box: return utils.getProperty(method,content,pns.type).equals("folder");
		 		case R.string.microsoft: return !utils.getProperty(method, content, "folder").equals("-");
			}
		}else if(method.equals("WebDAV")){
			String collection = utils.getProperty(method,content,pns.folder_type);
			 if(collection==null){return false;
			 }else{return true;}
		}
	 		}catch(Exception e){e.printStackTrace();return false;}
	 		return false;
	}
		public Path getPaths(String baseurl){
			if(method.equals("OAuth2")){
				switch(service_id){
				case R.string.google: return new Path("files","","root");
				case R.string.microsoft: return new Path("items","children","root");
				case R.string.box: return new Path("folders","items","0");
				case R.string.dropbox: return new Path("auto","","");
				default: return new Path("","","");
				}		
			}else{
				return new Path("","",new URL(baseurl).path);
			}
		}
		public String useAuthHeader(String credentials,String passport_url){
			String passport = credentials.split("&&")[0];
			String[] userpass = credentials.split("&&")[1].split(":");
			if(utils.tokenExpired(FileAccessActivity.expires)){
				passport = getNetPassport(passport_url,userpass[0],userpass[1]);
				SQLiteDatabase myDB= ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
				myDB.execSQL("UPDATE ACCOUNTS SET EXPIRES='"+FileAccessActivity.expires+"' WHERE CREDENTIALS='"+credentials.replaceAll("'", "''")+"'");
				myDB.execSQL("UPDATE ACCOUNTS SET CREDENTIALS='"+(passport+"&&"+credentials.split("&&")[1]).replaceAll("'", "''")+"' WHERE CREDENTIALS='"+credentials.replaceAll("'", "''")+"'");
				myDB.close();
			 }
			return "Authorization: Passport1.4 from-PP="+passport;
		}
		public String useAuthHeader(String credentials){
			if(method.equals("OAuth2")){
					OAuth2 o = new OAuth2(service_id);
					 if(utils.tokenExpired(FileAccessActivity.expires)){
						 credentials = o.getNewTokens(credentials);
						 System.out.println(credentials);
						 }
				String[] details = credentials.split("&&");
				return "Authorization: Bearer "+details[0];
			}else{
				if(service_id==R.string.microsoft){
					String passport = credentials.split("&&")[0];
					String[] userpass = credentials.split("&&")[1].split(":");
					if(utils.tokenExpired(FileAccessActivity.expires)){
						String url = utils.findPassportUrl(credentials, ctx);
						passport = getNetPassport(url,userpass[0],userpass[1]);
						SQLiteDatabase myDB= ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
						myDB.execSQL("UPDATE ACCOUNTS SET EXPIRES='"+FileAccessActivity.expires+"' WHERE CREDENTIALS='"+credentials.replaceAll("'", "''")+"'");
						myDB.execSQL("UPDATE ACCOUNTS SET CREDENTIALS='"+(passport+"&&"+credentials.split("&&")[1]).replaceAll("'","''")+"' WHERE CREDENTIALS='"+credentials.replaceAll("'", "''")+"'");
						myDB.close();
					 }
					return "Authorization: Passport1.4 from-PP="+passport;
				}else{
					return "Authorization: Basic "+Base64.encodeToString((credentials).getBytes(),Base64.NO_WRAP);
				}
			}
		}
		public String getDetails(String url, String email,String credentials) {
			AccountDetails d = new AccountDetails(method, service_id, url, credentials);
			String info="";
			info = info.concat("Service: "+d.service+"\n");
			info = info.concat("URL: "+d.url+"\n");
			info = info.concat("Name:"+d.person_name+"\n");
			info = info.concat("Email: "+d.email+"\n");
			info = info.concat("Quota: "+utils.bytes_in_h_format(d.quota_used)+ "/ "+ utils.bytes_in_h_format(d.quota_total));
			return info;
	}
		
 	}	
 	public String getNetPassport(String url, String username, String password){
		try{
		 ArrayList<String> headers = new ArrayList<String>();
		 HTTPRequest req = new HTTPRequest("GET",url, headers, new byte[0]);
    	 if(req.getHeaders().equals("")){return "No internet";}
		 String passporturl = utils.getHeaderValue(req.res_headers,"Location");
		 String authheader_value = utils.getHeaderValue(req.res_headers,"WWW-Authenticate");
		 String authparams = authheader_value.split("Passport1.4 ")[1];
		 String authheader ="Authorization: Passport1.4 sign-in="+username.replace("@", "%40")+","+"pwd="+password+","+"OrgVerb=GET"+","+"OrgUrl="+url+","+authparams; 
		 headers.add(authheader);
		 HTTPRequest req2 =  new HTTPRequest("GET",passporturl.replace("login.srf","login2.srf").split("\\?")[0], headers, new byte[0]);
		 headers.clear();
		 String authInfo =utils.getHeaderValue(req2.getHeaders(), "Authentication-Info");
		 if(authInfo.split("da-status=")[1].split(",")[0].equals("success")){
			 FileAccessActivity.expires=System.currentTimeMillis()+86400*1000;
			 return authInfo.split("from-PP=")[1].split(",")[0];
		 }else{return req2.res_status.split(" ")[1];}
		}catch(Exception e){e.printStackTrace();return "001";}
	}
 	public class FileIO extends Service{
		String credentials; String baseurl;
		public FileIO(String method, int service_id,String baseurl, String credentials){super(method,service_id);this.credentials = credentials; this.baseurl=baseurl;}
		public ArrayList<String> list_children(String dir_id){
			ArrayList<String> children = new ArrayList<String>();
			ArrayList<String> headers=new ArrayList<String>();
			 String url = getChildUrl(baseurl, dir_id);
			if(method.equals("OAuth2")){
				 try{
				headers.add(useAuthHeader(credentials));
				 String str=new HTTPRequest("GET",url, headers,new byte[0]).getContent();	 
				 str = str.substring(str.indexOf("{"), str.lastIndexOf("}") + 1);
				 JSONObject content = new JSONObject(new String(str));
				  String list_property="";
				 switch(service_id){
				 case R.string.google: list_property="items"; break;
				 case R.string.microsoft: list_property = "value"; break;
				 case R.string.box: list_property = "entries"; break;
				 case R.string.dropbox: list_property = "contents"; break;
				 default: 
				 }
				 JSONArray json_items = content.getJSONArray(list_property);
				 int c = json_items.length();
				 for(int i = 0; i < c; i++){children.add(json_items.getJSONObject(i).toString());}
				 }catch(Exception e){e.printStackTrace();}
			 }else if(method.equals("WebDAV")){
				 headers.add("Depth: 1");
				 headers.add(useAuthHeader(credentials));
				 String str = new HTTPRequest("PROPFIND",url, headers,new byte[0]).getContent();
				 int initial = 0; int change;
				 String property = "response";
				 String start= "<d:"+property+">";
				 String finish= "</d:"+property+">";
				 int end;
				 str = str.replaceAll("D:"+property+">", "d:"+property+">");
				 while(initial<str.length() && (change = str.substring(initial).indexOf(start))!=-1){
					 end = str.substring(initial).indexOf(finish);
					 if(end==-1){break;}
					  children.add(str.substring(initial+change+start.length(), initial+end));
					  initial = initial+end+finish.length();
				 }
				 try{
				 children.remove(0);
				 }catch(Exception e){
					 e.printStackTrace();
				 }
			 }
			return children;
		}		
		public void copy(boolean isFolder, String id, final String name, String dest_id){
			if(isFolder){
				ArrayList<String> children = list_children(id);	
				String new_folder_id = mkdir(dest_id, name);
				for(String child:children){
					Item item = new Item(method,service_id, child);
					copy(item.isFolder,item.id,item.name,new_folder_id);
				}
			}else{
				runOnUiThread(new Runnable(){public void run(){
					ProgressActivity.progress.setMessage("Copying "+name);
				}});
				String HTTPVerb="POST";
				 ArrayList<String> headers = new ArrayList<String>();
				 String path = "",json_str = "";
				 JSONObject json = new JSONObject();
				 boolean cont = true;
	        	  Services.URL u = new URL(baseurl);
	        	  String site = new URL(u.protocol, u.host, u.port, "", "").getURLString();
	        	  headers.add(useAuthHeader(credentials));
	        	  if(method.equals("OAuth2")){
	        		  if(service_id!=R.string.dropbox){
	        			  headers.add("Content-Type: application/json");
	        		  }
	        		  try {
				 switch(service_id){
				 	case R.string.google:
				 		path ="/drive/v2/files/"+id+"/copy"; 
						json.put("parents", new JSONArray(Arrays.asList(new JSONObject().put("id",dest_id))));
						json_str=json.toString();
					 break;
				 	case R.string.microsoft:
				 		path = "/drive/items/"+id+"/action.copy";
				 		if(dest_id.equals("root")){
				 			json.put("parentReference",new JSONObject().put("path", "/drive/root"));
				 		}else{
				 			json.put("parentReference",new JSONObject().put("id", dest_id));
				 		}
						json.put("name", name);
						json_str = json.toString();
						break;
				 	case R.string.box:
				 		path = "/2.0/files/"+id;
				 		json.put("parent", new JSONObject().put("id", dest_id));
				 		json_str = json.toString();
				 		break;
				 	case R.string.dropbox:
	         	  		path = "/1/fileops/copy";
	         	  		headers.add("Content-Type: application/x-www-form-urlencoded");
	         	  		json_str = "root=auto&from_path="+id+"&to_path="+dest_id+"/"+name;
	         	  		break;
				 }
	        		  } catch (Exception e) {e.printStackTrace();}
	        		  if(cont){
				 Services.HTTPRequest req = new HTTPRequest(HTTPVerb, site+path, headers, utils.strToBytes(json_str));
				 System.out.println(req.getStatus());
	        		  }
	        	  }

	  	  if(method.equals("WebDAV")){
		         	 String origin = new URL(u.protocol, u.host, u.port, "", "").getURLString()+id;
					 String destination = new URL(u.protocol, u.host, u.port, "", "").getURLString()+dest_id+"/"+name;
	         		 headers.add("Content-Type: application/xml");
	         		 headers.add("Destination: "+ destination);
	         		 Services.HTTPRequest req =  new HTTPRequest("COPY",origin, headers,new byte[0]);
	         		 System.out.println(req.getStatus());
	  	  		}	
	  	ProgressActivity.progress.incrementProgressBy(1);
	  		
			}
		
		}
		public String mkdir(String parent_id, String name){
			String path = ""; String json_str = "";
         	  Services.URL u = new URL(baseurl);
         	  Services.URL v = new URL(u.protocol, u.host, u.port, "", "");
         	  JSONObject json = new JSONObject();
         	  ArrayList<String> headers = new ArrayList<String>();
         	  headers.add(useAuthHeader(credentials));
         	  String HTTPVerb = "POST"; String req_url = "";
         	  if(method.equals("OAuth2")){
         		 try {
         			if(service_id!=R.string.dropbox){headers.add("Content-Type: application/json");}
         	  switch(service_id){
         	  	case R.string.google:	
         	  		path ="/drive/v2/files"; 
					json.put("title",name);
					json.put("parents", new JSONArray(Arrays.asList(new JSONObject().put("id",parent_id))));
         	  		json.put("mimeType","application/vnd.google-apps.folder");
         	  		json_str = json.toString();
         	  		break;
         	  	case R.string.microsoft:	
         	  		path ="/v1.0/drive/items/"+parent_id+"/children";
         	  		json.put("name", name);
         	  		json.put("folder",new JSONObject());
         	  		json.put("@name.conflictBehavior", "rename");
         	  		json_str = json.toString();
         	  		break;
         	  	
         	  	case R.string.box: 
         	  		path = "/2.0/folders"; 
         	  		json.put("name", name);
         	  		json.put("parent",new JSONObject().put("id",parent_id));
         	  		json_str = json.toString();
         	  		break;
         	  	case R.string.dropbox: 
         	  		path = "/1/fileops/create_folder";
         	  		headers.add("Content-Type: application/x-www-form-urlencoded");
         	  		json_str = "root=auto&path="+parent_id+"/"+name;
         	  		break;
         	  }
         	  
         		} catch (JSONException e) {e.printStackTrace();}
         	  req_url = v.getURLString()+path;
         	  }
         	  
         	  if(method.equals("WebDAV")){
         		 headers.add("Content-Type: application/xml");
         		 HTTPVerb = "MKCOL";
         		 req_url = getChildUrl(baseurl, parent_id)+"/"+name;
         	  }
         	 Services.HTTPRequest req = new HTTPRequest(HTTPVerb,req_url, headers, utils.strToBytes(json_str));
         	String content = req.getContent();
         	try{
         	 if(method.equals("OAuth2")){
         		 content = content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1);			
         	}
         	}catch(Exception e){e.printStackTrace();}
         	 Item item = new Item(method,service_id, content);
         	return item.id;
		}		
		public String delete(boolean isFolder, String id, boolean permanent){
			 String deleteurl=""; String body ="";
   		     String HTTPVerb="POST";
   		     if(permanent){HTTPVerb="DELETE";}
   		  ArrayList<String> headers = new ArrayList<String>();
	    	 headers.add(useAuthHeader(credentials));
   		   if(method.equals("OAuth2")){
   		     switch(service_id){
   		     	case R.string.box: 
   		     		HTTPVerb="DELETE";
   		     		if(isFolder){deleteurl = baseurl+"folders/"+id+"?recursive=true";}
   		     		else{deleteurl = baseurl+"files/"+id;}
   		     		break;
   		     	case R.string.google:
   		     		if(permanent){deleteurl = baseurl+"files/"+id;}
   		     		else{deleteurl = baseurl+"files/"+id+"/trash";}
   		     		break;
   		     	case R.string.dropbox:
   		     		deleteurl = baseurl+"fileops/delete";
   		     		headers.add(utils.Header("Content-Type","application/x-www-form-urlencoded"));
   		     		 body = "root=auto&path="+id;
   		     		break;
   		     	case R.string.microsoft:
   		     		HTTPVerb="DELETE";
   		     		deleteurl=baseurl+"items/"+id;
   		     		break;
   		     }
   		  
   		  }else if(method.equals("WebDAV")){
   			Services.URL u = new URL(baseurl);
			deleteurl = new URL(u.protocol, u.host, u.port, "", "").getURLString()+id;
   		  }
   		   
		    	 Services.HTTPRequest req =new HTTPRequest(HTTPVerb,deleteurl,headers,utils.strToBytes(body));
		    	 if(service_id==R.string.box && method.equals("OAuth2")&& permanent){
		    		if(isFolder){deleteurl = baseurl+"folders/"+id+"/trash";}
		     		else{deleteurl = baseurl+"files/"+id+"/trash";}
		    		req = new HTTPRequest(HTTPVerb,deleteurl,headers,new byte[0]);
		    	 }
		    	 System.out.println(deleteurl);
		    	return req.getStatus();	
		}
		public void rename(boolean isFolder,String id, String old_name,String name){
			String path = "",json_str = "";
         	  Services.URL u = new URL(baseurl);
         	  String site = new URL(u.protocol, u.host, u.port, "", "").getURLString();
         	  if(method.equals("OAuth2")){
         		 ArrayList<String> headers = new ArrayList<String>();
	         	  headers.add(useAuthHeader(credentials));
	         	 String HTTPVerb="PATCH";
	         	  try {
	         		 if(service_id!=R.string.dropbox){headers.add("Content-Type: application/json");}
         	  switch(service_id){
         	  	case R.string.google:	
         	  		path ="/drive/v2/files/"+id; 
					json_str = new JSONObject().put("title",name).toString();
         	  		break;
         	  	case R.string.microsoft:	
         	  		path ="/v1.0/drive/items/"+id;
         	  		json_str = new JSONObject().put("name", name).toString();
         	  		break;
         	  	case R.string.box: 
         	  		HTTPVerb = "PUT";
         	  		if(isFolder){path = "/2.0/folders/"+id;}else{path= "/2.0/files/"+id;}
         	  		json_str = new JSONObject().put("name", name).toString();
         	  		break;
         	  	case R.string.dropbox: 
         	  		HTTPVerb="POST";
         	  		path = "/1/fileops/move";
         	  		headers.add("Content-Type: application/x-www-form-urlencoded");
         	  		json_str="root=auto&from_path="+id+"&to_path="+(id+"}").replace(old_name+"}",name+"}").replace("}", "");
         	  		break;
         	  }
         		} catch (JSONException e) {e.printStackTrace();}
         	  
         	  Services.HTTPRequest req = new HTTPRequest(HTTPVerb,site+path, headers, utils.strToBytes(json_str));
         	  System.out.println(req.getStatus());

         	  }
         	  
         	  if(method.equals("WebDAV")){
         		 ArrayList<String> headers = new ArrayList<String>();
         		 
	         	  headers.add(useAuthHeader(credentials));
	         	 String origin = new URL(u.protocol, u.host, u.port, "", "").getURLString()+id;
	         	 String new_id = (id+"}").replace(old_name+"}",name+"}").replace("}", "");
				 String destination = new URL(u.protocol, u.host, u.port, "", "").getURLString()+new_id;
         		 headers.add("Content-Type: application/xml");
         		 headers.add("Destination: "+ destination);
         		 Services.HTTPRequest req =  new HTTPRequest("MOVE",origin, headers,new byte[0]);
         		 System.out.println(req.getStatus());
	         	 
         	  }
		}
		public void move(boolean isFolder,String id, String name, String origin_id,String dest_id){
			 String HTTPVerb="POST";
			 ArrayList<String> headers = new ArrayList<String>();
			 String path = "",json_str = "";
			 JSONObject json = new JSONObject();
			 boolean cont = true;
        	  Services.URL u = new URL(baseurl);
        	  String site = new URL(u.protocol, u.host, u.port, "", "").getURLString();
        	  headers.add(useAuthHeader(credentials));
        	  if(method.equals("OAuth2")){
        		  if(service_id!=R.string.dropbox){
        			  headers.add("Content-Type: application/json");
        		  }
        		  try {
			 switch(service_id){
			 	case R.string.google:
			 		HTTPVerb = "PATCH";
			 		path = "/drive/v2/files/"+id+"?"+"addParents="+dest_id+"&removeParents="+origin_id;
			 		json.put("parents", new JSONArray(Arrays.asList(new JSONObject().put("id",dest_id))));
					json_str=json.toString();
			 		break;
			 	case R.string.microsoft:
			 		HTTPVerb = "PATCH";
			 		path = "/v1.0/drive/items/"+id;
			 		if(dest_id.equals("root")){
			 			json.put("parentReference",new JSONObject().put("path", "/drive/root"));
			 		}else{
			 			json.put("parentReference",new JSONObject().put("id", dest_id));
			 		}
					
					json_str = json.toString();
					break;
			 	case R.string.box:
			 		HTTPVerb ="PUT";
			 		if(isFolder){
			 			path = "/2.0/folders/"+id;
			 		}else{
			 			path = "/2.0/files/"+id;
			 		}
			 		json.put("parent", new JSONObject().put("id", dest_id));
			 		json_str = json.toString();
			 		break;
			 	case R.string.dropbox:
         	  		path = "/1/fileops/move";
         	  		headers.add("Content-Type: application/x-www-form-urlencoded");
         	  		json_str= "root=auto&from_path="+id+"&to_path="+dest_id+"/"+name;
         	  		break;
			 }
        		  } catch (Exception e) {e.printStackTrace();}
        		  if(cont){
			 Services.HTTPRequest req = new HTTPRequest(HTTPVerb, site+path, headers, utils.strToBytes(json_str));
			 System.out.println(req.getStatus());
        		  }
        	  }

  	  if(method.equals("WebDAV")){
	         	 String origin = new URL(u.protocol, u.host, u.port, "", "").getURLString()+id;
				 String destination = new URL(u.protocol, u.host, u.port, "", "").getURLString()+dest_id+"/"+name;
         		 headers.add("Content-Type: application/xml");
         		 headers.add("Destination: "+ destination);
         		 Services.HTTPRequest req =  new HTTPRequest("MOVE",origin, headers,new byte[0]);
         		 System.out.println(req.getStatus());
  	  		}	
		}
		public File download(boolean isFolder, String id, final String name, File local_path, final int notify_id){
			File file;
			String state = Environment.getExternalStorageState();
			if (!Environment.MEDIA_MOUNTED.equals(state)) {
				return new File("");
			}else{
			 file = new File(local_path,name);
	    	 int c=0;
	    	 while(file.exists()){
	    		 c++;
	    		 if(name.contains(".")){
	    			 file = new File(local_path,name.replace(".", "("+String.valueOf(c)+")"+"."));
	    		 }else{
	    			 file = new File(local_path,name+"("+String.valueOf(c)+")");
	    		 }
	    	 }
			if(isFolder){
				file.mkdir();
				ArrayList<String> children = list_children(id);	
				for(String child:children){
					Item item = new Item(method,service_id, child);
					download(item.isFolder,item.id,item.name,file,notify_id);
				}
				return file;
				
			}else{
				 String HTTPVerb="GET";
				 ArrayList<String> headers = new ArrayList<String>();
				 String path = ""; byte[] bytes;
	        	  Services.URL u = new URL(baseurl);
	        	  String site = new URL(u.protocol, u.host, u.port, "", "").getURLString();
	        	  headers.add(useAuthHeader(credentials));
				if(method.equals("OAuth2")){
					switch(service_id){
					case R.string.google:
						path = "/drive/v2/files/"+id+"?alt=media"; break;
					case R.string.microsoft:
						path = "/v1.0/drive/items/"+id+"/content"; break;
					case R.string.box:
						path = "/files/"+id+"/content"; break;
					case R.string.dropbox:
						site = "https://content.dropboxapi.com:443";
						path = "/1/files/auto"+id;
						break;
					}
				}else if(method.equals("WebDAV")){
					path = id;
				}
					HTTPRequest req = new HTTPRequest(HTTPVerb, site+path, headers,new byte[0]);
					if(req.getStatus().contains("302")){
						req = new HTTPRequest(HTTPVerb, utils.getHeaderValue(req.getHeaders(),"Location"), headers,new byte[0]);
					}
					final int total = Integer.valueOf(utils.getHeaderValue(req.getHeaders(),"Content-Length"));
					runOnUiThread(new Runnable(){public void run(){
						ProgressActivity.progress.setMessage("Downloading "+name+".\r\n Size: "+utils.bytes_in_h_format((long)total));
						//FileAccessActivity.notification.contentView.setTextViewText(R.id.txtstatus,"Downloading "+name+". Size: "+bytes_in_h_format((long)total));
						//FileAccessActivity.mNotifyManager.notify(notify_id,FileAccessActivity.notification);
					}});

				    try{
				    	 FileOutputStream fos = new FileOutputStream(file);
				    	 int chunk_size = 16*1024*1024;
				    	 int remaining = total;
				    	 while(remaining>0){
				    		 bytes = req.getFileChunk(chunk_size,remaining,total - remaining,total,notify_id);
				    		 fos.write(bytes, 0, bytes.length);
				    		 remaining-=bytes.length;
				    	 }
				    	 return file;
				    	 }catch(Exception e){
				    		 e.printStackTrace();
				    	 }
				    }
					return new File("");
			}
		}
		public String upload(String parent_id,final File file, boolean overwrite, final int notify_id){
			if(!file.exists()){return "File or directory not found";}
			if(file.isDirectory()){
				File[] children = file.listFiles();	
				String new_folder_id = mkdir(parent_id, file.getName());
				for(File child:children){
					upload(new_folder_id,child,false, notify_id);
				}
				return "Directory uploaded";
			}else{
			String HTTPVerb="POST"; 
			HTTPRequest req; String upload_url, upload_id,path,site; JSONObject json;
			  Services.URL u = new URL(baseurl);
		  ArrayList<String>headers = new ArrayList<String>();
		  headers.add(useAuthHeader(credentials));
       	  site = new URL(u.protocol, u.host, u.port, "", "").getURLString();
       	String mimeType=MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getPath()));
       	if(mimeType==null){
 			mimeType = "application/octet-stream";
 		} 
       	try{
       		FileInputStream fos = new FileInputStream(file);
       		final int total;
			int uploadedBytes, remainingBytes, chunk_size = 16*1024*1024; byte[] chunk,remainder;
   			total = (int) file.length(); remainingBytes=total; uploadedBytes=0;
   			runOnUiThread(new Runnable(){public void run(){
				ProgressActivity.progress.setMessage("Uploading "+file.getName()+".\r\n Size: "+utils.bytes_in_h_format((long)total));
   			}});
       		  if(method.equals("OAuth2")){
       			json = new JSONObject(); 
			 switch(service_id){
			 	case R.string.google: 
			 		headers.add(utils.Header("X-Upload-Content-Type",mimeType));
			 		headers.add("Content-Type: application/json");
			 		json.put("title", file.getName()); json.put("parents", new JSONArray(Arrays.asList(new JSONObject().put("id",parent_id))));
			 		req = new HTTPRequest(HTTPVerb,site+"/upload/drive/v2/files?uploadType=resumable",headers,utils.strToBytes(json.toString()));
			 		upload_url =utils.getHeaderValue(req.getHeaders(),"Location");
			 		headers = new ArrayList<String>(Arrays.asList(useAuthHeader(credentials),utils.Header("Content-Type",mimeType), "Content-Length: "+total));
			 		HTTPRequest req2;
			 		if(total>chunk_size){
			 			req2 = new HTTPRequest(HTTPVerb,upload_url,headers);
			 			chunk = new byte[chunk_size];
			 			while(remainingBytes>chunk_size){
				 			fos.read(chunk);
				 			req2.putChunk(chunk);
				 			remainingBytes-=chunk_size; uploadedBytes+=chunk_size;
				 		}
				 		remainder = new byte[remainingBytes];
				 		fos.read(remainder);
				 		req2.putChunk(remainder);
				 		req2.send();
			 		}else{
			 			byte[] whole = new byte[total];
			 			fos.read(whole);
			 			headers.add("Content-Length: "+total);
			 			req2 = new HTTPRequest(HTTPVerb,upload_url,headers).putChunk(whole).send();
			 		}
			 		return req2.getStatus();
			 	case R.string.microsoft: 
			 		headers.add("Content-Type: application/json");
			 		req = new HTTPRequest(HTTPVerb,site+"/v1.0/drive/items/"+parent_id+":/"+URLEncoder.encode(file.getName())+":/upload.createSession",headers,new byte[0]);
			 		HTTPVerb="PUT";
			 		upload_url = utils.getProperty(method,req.getContent(),"uploadUrl");
			 		fos = new FileInputStream(file);
			 		if(total>chunk_size){
			 			chunk = new byte[chunk_size];
			 			while(remainingBytes>chunk_size){
			 				fos.read(chunk);
			 				headers = new ArrayList<String>();
			 				headers.add(useAuthHeader(credentials));
			 				headers.add("Content-Type: "+mimeType);
			 				headers.add(utils.Header("Content-Range","bytes "+String.valueOf(uploadedBytes)+"-"+String.valueOf(uploadedBytes+chunk_size-1)+"/"+String.valueOf(total)));
			 				headers.add("Content-Length: "+chunk_size);
			 				req = new HTTPRequest(HTTPVerb,upload_url,headers).putChunk(chunk).send();
			 				remainingBytes-=chunk_size; uploadedBytes+=chunk_size;
			 			}
			 		}
			 		remainder = new byte[remainingBytes];
			 		fos.read(remainder);
			 		headers = new ArrayList<String>();
			 		headers.add(useAuthHeader(credentials));
			 		headers.add(utils.Header("Content-Range","bytes "+String.valueOf(uploadedBytes)+"-"+String.valueOf(total-1)+"/"+String.valueOf(total)));
			 		headers.add("Content-Length: "+remainingBytes);
			 		req = new HTTPRequest(HTTPVerb,upload_url,headers).putChunk(remainder).send();
			 		fos.close();
			 		return req.getStatus();
			 	case R.string.box: 
			 		site = "https://upload.box.com:443"; path = "/api/2.0/files/content";
			 		String separator = "---------wqkjfhkwqhfkwqf";
			 		headers.add("Content-Type: multipart/form-data; boundary="+separator);
			 		json.put("name", file.getName()); json.put("parent", new JSONObject().put("id", parent_id));
			 		ArrayList<String> first = new ArrayList<String>(),second=new ArrayList<String>();
			 		first.add("Content-Disposition: form-data; name=\"attributes\"");
			 		req = new HTTPRequest(HTTPVerb,site+path,headers);
			 		req.putChunk(utils.newPart(separator,first,json.toString().getBytes()));
			 		second.add("Content-Disposition: form-data; name=\"file\"; filename=\""+file.getName()+"\"");
			 		second.add("Content-Type: "+mimeType);
			 		req.putChunk(("--"+separator+"\r\n").getBytes("UTF-8"));
					for(int i=0; i<second.size(); i++){req.putChunk((second.get(i)+"\r\n").getBytes("UTF-8"));}
					req.putChunk(("\r\n").getBytes("UTF-8"));
					
		 			while(remainingBytes>chunk_size){
		 				chunk = new byte[chunk_size];
			 			fos.read(chunk);
			 			req.putChunk(chunk);
			 			remainingBytes-=chunk_size;
			 		}
			 		remainder = new byte[remainingBytes];
			 		fos.read(remainder);
			 		req.putChunk(remainder);
			 		
					req.putChunk(("\r\n").getBytes("UTF-8"));
			 		req.putChunk(("\r\n"+"--"+separator+"--"+"\r\n").getBytes());
			 		req.send();
			 		return req.getStatus();
			 	case R.string.dropbox: 
			 		HTTPVerb="PUT";
			 		site = "https://content.dropboxapi.com:443"; 
			 		if(total>chunk_size){
				 		chunk = new byte[chunk_size];
				 		fos.read(chunk);
				 		headers.add("Content-Length: "+chunk_size);
			 			req = new HTTPRequest(HTTPVerb,site+"/1/chunked_upload",headers).putChunk(chunk).send();
			 			remainingBytes-=chunk_size; uploadedBytes+=chunk_size;
			 			String json_str = req.getContent();
			 			json_str= json_str.substring(json_str.indexOf("{"),json_str.lastIndexOf("}")+1);
				 		upload_id = utils.getProperty(method,json_str,"upload_id");
				 		while(remainingBytes>chunk_size){
				 			fos.read(chunk);
				 			req = new HTTPRequest(HTTPVerb,site+"/1/chunked_upload?upload_id="+upload_id+"&offset="+uploadedBytes,headers).putChunk(chunk).send();
				 			remainingBytes-=chunk_size; uploadedBytes+=chunk_size;
				 		}
				 		remainder = new byte[remainingBytes];
				 		fos.read(remainder);
				 		headers.set(headers.size()-1,"Content-Length: "+remainder);
				 		req = new HTTPRequest(HTTPVerb,site+"/1/chunked_upload?upload_id="+upload_id+"&offset="+uploadedBytes,headers).putChunk(remainder).send();
				 		headers.set(headers.size()-1,"Content-Type: application/x-www-form-urlencoded");
				 		req = new HTTPRequest("POST",site+"/1/commit_chunked_upload/auto/"+parent_id+"/"+file.getName(),headers,utils.strToBytes("upload_id="+upload_id));
				 		fos.close();
				 		return req.getStatus();
			 		}else{
			 			byte[] whole = new byte[total];
			 			path = "/1/files_put/auto/"+parent_id+"/"+file.getName();
			 			headers.add("Content-Length: "+total);
			 			return new HTTPRequest(HTTPVerb,site+path,headers).putChunk(whole).send().getStatus();
			 		}
			 }
       		  }else if(method.equals("WebDAV")){
       			HTTPVerb="PUT"; 
		 		path = parent_id+"/"+file.getName();
		 		if(total>chunk_size){
		 			req = new HTTPRequest(HTTPVerb,site+path,headers);
		 			chunk = new byte[chunk_size];
		 			while(remainingBytes>chunk_size){
			 			fos.read(chunk);
			 			req.putChunk(chunk);
			 			remainingBytes-=chunk_size; uploadedBytes+=chunk_size;
			 		}
			 		remainder = new byte[remainingBytes];
			 		fos.read(remainder);
			 		req.putChunk(remainder);
			 		req.send();
		 		}else{
		 			byte[] whole = new byte[total];
		 			fos.read(whole);
		 			headers.add("Content-Length: "+total);
		 			req = new HTTPRequest(HTTPVerb,site+path,headers).putChunk(whole).send();
		 		}
		 		fos.close();
		 		return req.getStatus();
       		  }
       	  }catch(Exception e){ e.printStackTrace();}
			return "";
		}
		}
		public int getTotalSize(boolean isFolder, String id, int file_size){
			if(isFolder){
				int cur = 0;
				ArrayList<String> children = list_children(id);
				for(String child:children){
					Item item = new Item(method,service_id, child);
					if(item.isFolder){
						cur+=getTotalSize(true,item.id,0);
					}else{
						ProgressActivity.progress.setMax(ProgressActivity.progress.getMax()+Integer.parseInt(item.size));
						cur+=Integer.parseInt(item.size);
					}
				}	
				return cur;
			}else{
				ProgressActivity.progress.setMax(ProgressActivity.progress.getMax()+file_size);
				return file_size;
			}
		}	
		public int getNumOfFiles(boolean isFolder, String id){
			if(isFolder){
				int num = 0;
				ArrayList<String> children = list_children(id);
				for(String child:children){
					Item item = new Item(method,service_id, child);
					if(item.isFolder){
						num+=getNumOfFiles(true,item.id);
					}else{
						ProgressActivity.progress.setMax(ProgressActivity.progress.getMax()+1);
						num+=1;
					}
				}
				return num;
			}else{
				ProgressActivity.progress.setMax(ProgressActivity.progress.getMax()+1);
				return 1;
			}
		}
	}
	public class Path{
		String drive, child, root;
		public Path(String drive, String child, String root){
			super();this.drive=drive; this.child=child; this.root=root;
		}
	}		
	public class ConfigureAccount{
		public String changeAccountName(String original, String changed){
			SQLiteDatabase myDB = ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);
			int duplicates = myDB.query("ACCOUNTS",null,"NAME='"+changed+"'",null,null,null,null).getCount();
			if(duplicates>0){
				myDB.close();
				return original;
			}
			myDB.close();
			changed = new AccountDatabase().changeAccountName(original, changed);
			MainActivity.Account a = MainActivity.getAccountByName(original, MainActivity.accounts);
			int position = MainActivity.accounts.indexOf(a);
			MainActivity main = new MainActivity();
			MainActivity.Account new_a = main.new Account(changed, a.id, a.quota, a.service_id);
			MainActivity.accounts.set(position,new_a);
			MainActivity.account_adapter.notifyDataSetChanged();
			return changed;
		}
		public void disconnectAccount(String name){
			new AccountDatabase().disconnectAccount(name);
			MainActivity.accounts.remove(MainActivity.accounts.lastIndexOf(MainActivity.getAccountByName(name, MainActivity.accounts)));
			MainActivity.account_adapter.notifyDataSetChanged();
		}
	}
	public class AccountDatabase{
	SQLiteDatabase myDB;
	public AccountDatabase(){super();this.myDB= ctx.openOrCreateDatabase("services.db", Context.MODE_PRIVATE, null);}
	public String changeAccountName(String original, String changed){
		this.myDB.execSQL("UPDATE ACCOUNTS SET NAME='"+changed+"' WHERE NAME='"+original+"'");
		this.closeDatabase();
		return changed;
}
	public void disconnectAccount(String name){
		      myDB.execSQL("DELETE FROM ACCOUNTS WHERE NAME='"+name+"'");
		      this.closeDatabase();
	}	
	public void closeDatabase(){
		this.myDB.close();
	}
}
	public class FileChooser{
		String action,id,method, baseurl;
		int service_id;
		String credentials;
		public FileChooser(String action){
			super();
			this.action = action;
		}
		public FileChooser(String action, String method, int service_id,String baseurl, String credentials,String id){
			super();
			this.action = action;
			this.method = method;
			this.service_id = service_id;
			this.baseurl = baseurl;
			this.credentials=credentials;
			this.id = id;
		}
	 public void chooseFile(final URI uri){
		 
		 AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		 if(action.equals("open")){
		    builder.setTitle("Choose file to open: "+"\r\n"+uri);
		 }else if(action.equals("upload")){
			 builder.setTitle("Choose file to upload: "+"\r\n"+uri);
		 }
		    final File f = new File(uri);
		    final File[] children = f.listFiles();
		    String[] names = new String[children.length+1];
		    names[0] = "Go Back (..)";
		    for(int i=0; i<children.length;i++){
		    	names[i+1] = children[i].getName();
		    }
		    
		    builder.setItems(names, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, final int item) {
		        	if(item==0){
		        		if(f.getParentFile()!=null){
		        			chooseFile(f.getParentFile().toURI());
		        		}else{
		        			 Toast.makeText(ctx, "This is the root folder", 2000).show();
		        			 chooseFile(uri);
		        		}
		        	}
		        	else if(children[item-1].isDirectory()){
		        		if(children[item-1].canRead()){
		        			chooseFile(children[item-1].toURI());
		        		}else{
		        			Toast.makeText(ctx, "Cannot go to protected folder", 2000).show();
		        			chooseFile(uri);
		        		}
		            }else{
		            	if(action.equals("open")){
		            	 utils.open(children[item-1], ctx);
		            	}else if(action.equals("upload")){
		            		Editor edit = ctx.getSharedPreferences("com.x.cloudhub.upload", Context.MODE_PRIVATE).edit();
		            		edit.clear();
		            		edit.putBoolean("isFolder",false);
		            		edit.putString("name", children[item-1].getName());
		            		edit.putString("path", Uri.fromFile(children[item-1].getParentFile()).toString()+"/");
		            		edit.apply();
		            		((FileAccessActivity)ctx).upload(null);
		            	}
		            }	
		        } 
		    });
		    final AlertDialog alert = builder.create(); //don't show dialog yet
		    alert.setOnShowListener(new OnShowListener() {       
		        @Override
		        public void onShow(DialogInterface dialog) {       
		        	if(action.equals("upload")){
		        		alert.getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
		        	@Override
		        	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long longid){
		            	AlertDialog.Builder builder2 = new AlertDialog.Builder(ctx);
		            	builder2.setMessage("Choose action:");
		            	builder2.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
		            		public void onClick(DialogInterface dialog, int which){
		            			Editor edit = ctx.getSharedPreferences("com.x.cloudhub.upload", Context.MODE_PRIVATE).edit();
			            		edit.clear();
			            		edit.putBoolean("isFolder",children[position-1].isDirectory());
			            		edit.putString("name", children[position-1].getName());
			            		edit.putString("path", Uri.fromFile(children[position-1].getParentFile()).toString()+"/");
			            		edit.apply();
			            		alert.dismiss();
			            		((FileAccessActivity)ctx).upload(null);
		    		        }
		            		}
		            	);
		            	builder2.show();
		            	return true;
		        	}           
		        		});     
		        	}
		        }
		    });
		    alert.show();
	 	}
	}
}