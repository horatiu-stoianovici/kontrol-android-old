package com.example.kontrol;

import com.example.kontrol.tcpcommunications.NetworkDevice;
import com.example.kontrol.tcpcommunications.TCPServerInfo;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AdapterServerList extends ArrayAdapter<TCPServerInfo> {
	private Context theContext;
	
	public AdapterServerList(Context context) {
		super(context, 0);
		theContext = context;
	}
	
	private static class TitleViewHolder {
	    public TextView tvServerName, tvIpAddress;
	    public ImageView imageViewDevice;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TCPServerInfo server = this.getItem(position);
		View rowView;
		
		if(convertView == null){
			LayoutInflater inflater = (LayoutInflater) this.theContext
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.listview_server, parent, false);
			
			TextView tvServerName = (TextView)rowView.findViewById(R.id.textViewServerName);
			tvServerName.setPaintFlags(tvServerName.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			tvServerName.setTypeface(MyFonts.boldFont);
			TextView tvIpAddress = (TextView)rowView.findViewById(R.id.textViewIpAddress);
			tvIpAddress.setPaintFlags(tvIpAddress.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			tvIpAddress.setTypeface(MyFonts.normalFont);
			ImageView imageViewDevice = (ImageView)rowView.findViewById(R.id.imageView1);
			
			TitleViewHolder holder = new TitleViewHolder();
			holder.tvIpAddress = tvIpAddress;
			holder.tvServerName = tvServerName;
			holder.imageViewDevice = imageViewDevice;
			
			rowView.setTag(holder);
		}
		else {
			rowView = convertView;
		}
		
		TitleViewHolder holder = (TitleViewHolder)rowView.getTag();
		holder.tvServerName.setText(server.getName());
		
		if(server.isConnecting())
		{
			holder.tvIpAddress.setText("Connecting...");
		}
		else
		{
			holder.tvIpAddress.setText(server.getConnectedMessage() != null ? server.getConnectedMessage() : server.getIpAddressString());
		}
		if(server.getDevice() == NetworkDevice.Cable)
			holder.imageViewDevice.setImageResource(R.drawable.computer_network_icon);
		else
			holder.imageViewDevice.setImageResource(R.drawable.wireless_icon);
			
		return rowView;
	}
}


