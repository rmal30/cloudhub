package com.x.cloudhub;
import java.awt.Color;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import javax.imageio.*;

public class genimg {
	static String path="C:/Documents and Settings/r/workspace/CloudHub/res/drawable-ldpi/";
	public static void main(String args[]){
		char[] ms_secret = "gFIsIktzB2dLdAcRisLX4-DjY0ft21-v".toCharArray();
		//char[] dropbox_secret="beootlvbomv3amm".toCharArray();
		//char[] box_secret="ibifd462eza4JrFiSzsF3eGCMFbnOZoN".toCharArray();
		//char[] amazon_secret = "4330dbeb2ec51f117ecb9e9bb63c290173292c52b405de60a5c080e09e4cbdfc".toCharArray();
		//char[] google_secret ="4bcO5Tkk3jEbGxijDKtaVsNg".toCharArray();
		
		
		BufferedImage Image;BufferedImage originalImage = null;
		File fnew=new File(path +"ic_launcher.png");
		try {
			originalImage = ImageIO.read(fnew);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Image = originalImage;
	
		for(int i=0; i<ms_secret.length;i++){
			//Integer.toString(Integer.parseInt(Integer.toString((int) ms_secret[i],10)),10);
			String char_code=Integer.toString((int) ms_secret[i]);
			Color color = new Color(208,182,0);
			char[]colors=Integer.toString(Integer.parseInt(char_code),6).toCharArray();
			Color new_color=new Color(color.getRed()+(colors[0]-'0'), color.getGreen()+(colors[1]-'0'),color.getBlue()+(colors[2]-'0'));
			Image.setRGB(i,0,new_color.getRGB());
			System.out.println(colors[0]+" "+colors[1]+" "+colors[2]);
		}
		try {
			ImageIO.write(Image, "png", new File(path,"ic_launcher.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
