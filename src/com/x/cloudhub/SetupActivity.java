package com.x.cloudhub;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SetupActivity extends Activity{
	Integer service_id; String url_string,method; boolean authenticated = false; EditText webdav_url;
	 final Services s=new Services(this);
	String redirect_uri="https://cloudhub.com";
	String grant_type="authorization_code";
	 public void onCreate(Bundle savedInstanceState) {
		 requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		 super.onCreate(savedInstanceState);
		   Bundle extras = this.getIntent().getExtras();
       	service_id = extras.getInt("service_id");
       	method = extras.getString("method");
       setTitle(getResources().getString(service_id));
       
		 if(method.equals("WebDAV")){
			 url_string = getResources().getString(extras.getInt("url_id"));
			 setContentView(utils.getProtoLayout(service_id));
			 if(!service_id.equals(R.string.microsoft)){
	        	webdav_url =(EditText) findViewById(R.id.url_input);
	   			webdav_url.setText(url_string); 
	   			if(!url_string.equals("")){webdav_url.setEnabled(false);webdav_url.clearFocus();}
	        }else{
	        	final Services.OAuth2 o  = s.new OAuth2(R.string.microsoft);
	        	final String baseUrl1 = getResources().getString(o.getUrl(1));
	    	    final WebView wv1 = (WebView) findViewById(R.id.webView1);
	    	    WebSettings ws = wv1.getSettings();ws.setSaveFormData(false);ws.setSavePassword(false);ws.setAppCacheEnabled(false);
	    	    final ProgressBar progressBar1=(ProgressBar) findViewById(R.id.progressBar1);
	    	    final String scopes = "wl.basic";   
	    	    ws.setSaveFormData(false);ws.setSavePassword(false); 
	    	    ws.setAppCacheEnabled(false);
	    	    setTitle("Getting CID via OAuth2");  
	    	    wv1.setWebViewClient(new WebViewClient() { 
		        	@Override
		        	public void onPageStarted(WebView view, String url, Bitmap favicon){
		        		wv1.setVisibility(View.GONE);
		        		if(Integer.valueOf(android.os.Build.VERSION.SDK)<=10){progressBar1.setVisibility(View.VISIBLE);}
		               if(url.contains(redirect_uri) && url.contains("access_token=")){
		            		authenticated=true; wv1.stopLoading();CookieManager.getInstance().removeAllCookie(); utils.clearWebViewDatabases(SetupActivity.this);wv1.clearCache(true);
		    				try{
		    				   JSONObject about = new JSONObject(new String(s.new HTTPRequest("GET",getResources().getString(o.getUrl(3))+s.new OAuth2(service_id).getUserInfoPath(), new ArrayList<String>(Arrays.asList("Authorization: Bearer "+url.split("access_token=")[1].split("&")[0].replace("#",""))),new byte[0]).getContent()));
		    				   setContentView(R.layout.protocol_webdav);
		    				   webdav_url = (EditText) findViewById(R.id.url_input);
		    				   webdav_url.setText(url_string+about.getString("id"));webdav_url.setEnabled(false);webdav_url.clearFocus();
		    				   setTitle(getResources().getString(service_id)+" WebDAV configuration");
		    				   }catch(Exception e){e.printStackTrace();}
		            	   }
		               }
		        	 @Override
						public void onPageFinished(WebView view, String url){
			        		 progressBar1.setVisibility(View.GONE);	
			        		 if(!authenticated){wv1.setVisibility(View.VISIBLE);}       		 
			        	 }
		        	
		        });
	    	    wv1.getSettings().setJavaScriptEnabled(true);
		        String args = "client_id="+o.getClientId()+o.getSetupParams()+"&response_type=token&scope="+scopes+"&redirect_uri=https%3A%2F%2Fcloudhub%2Ecom";
		        wv1.loadUrl(baseUrl1+"?"+args);
	        }
		 }else if(method.equals("OAuth2")){
			 	final Services.OAuth2 o = s.new OAuth2(service_id);
			    final String client_id=o.getClientId();  
			    setContentView(R.layout.webview);
			    setTitle(getResources().getString(service_id)+" Authorization");
			    final WebView wv1 = (WebView) findViewById(R.id.webView1);
			    WebSettings ws = wv1.getSettings();
			    ws.setSaveFormData(false);ws.setSavePassword(false);ws.setAppCacheEnabled(false);
			    final ProgressBar progressBar1=(ProgressBar) findViewById(R.id.progressBar1);
			    final String scopes=getResources().getString(o.getScope()).replaceAll(":", "%3A").replaceAll("/","%2F").replaceAll(" ","%20");
			        wv1.setWebViewClient(new WebViewClient() { 
			        	@Override
			        	public void onPageStarted(WebView view, String url, Bitmap favicon){
			        		wv1.setVisibility(View.GONE);
			        		if(Integer.valueOf(android.os.Build.VERSION.SDK)<=10){progressBar1.setVisibility(View.VISIBLE);}
			               if(url.contains(redirect_uri) && url.contains("code=")){
			            		   String code=url.split("code=")[1].split("&")[0].replace("#","");
			            		   wv1.stopLoading(); CookieManager.getInstance().removeAllCookie();
			            		   utils.clearWebViewDatabases(SetupActivity.this); wv1.clearCache(true); authenticated=true;
			             		   String client_secret=o.getClientSecret();
			             		   String baseurl = getResources().getString(o.getUrl(3));
			             		   System.out.println(baseurl);
			    					String args="code="+code+"&"+"client_id="+client_id+"&"+"client_secret="+client_secret+"&"+"redirect_uri="+URLEncoder.encode(redirect_uri)+"&"+"grant_type="+grant_type;
			    					try {	        
			    						
			    				        String strHent = s.new HTTPRequest("POST",getResources().getString(o.getUrl(2)),new ArrayList<String>(Arrays.asList("Content-Type: application/x-www-form-urlencoded")),utils.strToBytes(args)).getContent();
			    				        strHent = strHent.substring(strHent.indexOf("{"), strHent.lastIndexOf("}") + 1);
			    						JSONObject response=new JSONObject(strHent);
		    				            String access_token=response.getString("access_token");
		    				            String refresh_token=access_token;
		    				            Long expires=0L;
		    				            if(service_id!=R.string.dropbox){refresh_token = response.getString("refresh_token"); expires = System.currentTimeMillis()+3600*1000;}
		    				            FileAccessActivity.expires=expires;
		    				            Intent acc=new Intent(SetupActivity.this, UserInfoActivity.class);
		    							Bundle para=new Bundle();
		    							para.putInt("service_id", service_id);
		    							para.putString("method", method);
		    							para.putString("url", baseurl);
		    							Services.AccountDetails details = s.new AccountDetails(method,service_id,baseurl,access_token+"&&"+refresh_token);
		    							para.putString("credentials", access_token+"&&"+refresh_token);
		    							para.putString("email", details.id);
		    							para.putLong("expires", expires);
			    				        startActivity(acc.putExtras(para));  
			    				    } catch (Exception e) {e.printStackTrace();}
			            	   }  
			             }
			        	 @Override
						public void onPageFinished(WebView view, String url){
			        		 progressBar1.setVisibility(View.GONE);	
							if(!authenticated){wv1.setVisibility(View.VISIBLE);}        		 
			        	 }
			            });	       
			        wv1.getSettings().setJavaScriptEnabled(true);
			        String args = "client_id="+client_id+o.getSetupParams()+"&response_type=code&scope="+scopes+"&redirect_uri=https%3A%2F%2Fcloudhub%2Ecom";
			        wv1.loadUrl(getResources().getString(o.getUrl(1))+"?"+args);
		 }else if(method.equals("FTP")){
			 setContentView(R.layout.protocol_ftp);
		 }
	  }
	 public void connectWebDAV(View v){
		 webdav_url =(EditText) findViewById(R.id.url_input);
		 EditText editUsrName =(EditText) findViewById(R.id.UsrName);
		 EditText editPwd =(EditText) findViewById(R.id.pwd);
		 String url = webdav_url.getText().toString();
		 String username = editUsrName.getText().toString();
		 String password =editPwd.getText().toString();
		 Intent acc=new Intent(SetupActivity.this, UserInfoActivity.class);
			Bundle para=new Bundle();
			para.putInt("service_id", service_id);
			para.putString("method", method);
			para.putString("url", url);
			
			TextView authError =(TextView) findViewById(R.id.authErrorText);
			if(service_id==R.string.microsoft){
				 para.putString("email", username);	
			    	String passport = s.getNetPassport(webdav_url.getText().toString(),username,password);
			    	if(passport.length()==3){
			    		authError.setText("Error "+passport+": ");
			    		if(passport.equals("401")){
			    			authError.append("The username or password is incorrect. Please try again."); return;
			    		}
			    		if(passport.equals("503")){
			    			authError.append("The authentication server is currently unavailiable. Please try again later.");
			    			return;
			    		}
			    		authError.append("Unknown problems are occuring. Please notify the developer and give the code above");
			    	}
			    	if(passport.equals("No Internet")|| passport.equals("001")){return;}
			    	para.putString("credentials", passport+"&&"+username+":"+password);
			    	para.putLong("expires", System.currentTimeMillis()+86400*1000);
				}else{
			    	 para.putString("credentials", username+":"+password); 
			    	 para.putLong("expires", 0);
			     }
			
	        startActivity(acc.putExtras(para));   
	 }
	 public void connectFTP(View v){
		 EditText ftp_url =(EditText) findViewById(R.id.url_input);
		 EditText editUsrName =(EditText) findViewById(R.id.UsrName);
		 EditText editPwd =(EditText) findViewById(R.id.pwd);
		 String url = ftp_url.getText().toString();
		 String username = editUsrName.getText().toString();
		 String password =editPwd.getText().toString();
		 Intent acc=new Intent(SetupActivity.this, UserInfoActivity.class);
			Bundle para=new Bundle();
			para.putInt("service_id", R.string.ftp);
			para.putString("method", "FTP");
			para.putString("url", url);
			para.putString("credentials", username+":"+password);
			para.putString("email","");
			para.putLong("expires",0);
	        startActivity(acc.putExtras(para));   
	 }
}