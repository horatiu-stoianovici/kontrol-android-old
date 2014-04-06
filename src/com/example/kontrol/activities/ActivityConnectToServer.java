package com.example.kontrol.activities;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.example.kontrol.AdapterServerList;
import com.example.kontrol.MyFonts;
import com.example.kontrol.R;
import com.example.kontrol.database.SavedServers;
import com.example.kontrol.tcpcommunications.ClientInfo;
import com.example.kontrol.tcpcommunications.TCPSecurity;
import com.example.kontrol.tcpcommunications.TCPServerInfo;
import com.haarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.todddavies.components.progressbar.ProgressWheel;

public class ActivityConnectToServer extends Activity {
	private AdapterServerList listAdapter;
	private ListView listView;
	private TextView availableServersTextView;
//	private RelativeLayout loadingPanel;
	private ProgressWheel progressWheel;
	private boolean isSpinning;
	private Context context;
	private HashSet<String> knownServers;
	private SavedServers dbHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_connect_to_server);
		
		context = this;
		
		MyFonts.Initialize(context);
		knownServers = new HashSet<String>();
		listAdapter = new AdapterServerList(this);
		listView = (ListView)findViewById(R.id.listViewServers);
		
		AnimationAdapter animAdapter = new SwingBottomInAnimationAdapter(listAdapter);
		animAdapter.setAbsListView(listView);
		listView.setAdapter(animAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			/**
			 * When clicking on an item from the list of servers, a dialog appears for connecting to that server
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				//try to connect to the selected server
				final TCPServerInfo server = listAdapter.getItem(position);
					tryConnectingToServer(server);
			}
		});
		
		availableServersTextView = (TextView)findViewById(R.id.textViewAvailableServers);
		availableServersTextView.setTypeface(MyFonts.normalFont);
		availableServersTextView.setPaintFlags(availableServersTextView.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
		
		progressWheel = (ProgressWheel) findViewById(R.id.pw_spinner);
		progressWheel.setSpinSpeed(9);
		
		progressWheel.setOnClickListener(new OnClickListener() {
			/**
			 * Refresh the list of servers when clicking on the spinner
			 */
			@Override
			public void onClick(View v) {
				if(!isSpinning){
					startSearchingForServers(context);
				}
			}
		});
		
		dbHandler = new SavedServers(this);
		
		startSearchingForServers(context);
	}
	
	/**
	 * Scans for servers in the background and updates the listViewAdapter whenever it finds a new server
	 * @param context
	 */
	private void startSearchingForServers(final Context context){
		new AsyncTask<Void, TCPServerInfo, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				boolean ok = true;
				long startTime = System.currentTimeMillis();
				knownServers.clear();
				
				while(ok)
				{
					long currentTime = System.currentTimeMillis();
					if((currentTime - startTime) > 20000)
					{
						ok = false;
					}
					
					try {
						WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
						MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
						multicastLock.setReferenceCounted(true);
						multicastLock.acquire();
						
						MulticastSocket socket = new MulticastSocket(ClientInfo.PortUdp);
						InetAddress group = InetAddress.getByName(ClientInfo.MulticastIp);
						socket.joinGroup(group);
		
						DatagramPacket packet;
						byte[] buf = new byte[256];
						packet = new DatagramPacket(buf, buf.length);
						socket.setSoTimeout(5000);
						socket.receive(packet);
		
						String received = new String(packet.getData()).trim();
						
						TCPServerInfo info = new TCPServerInfo(received);
						
						if(!knownServers.contains(info.getId())){
							publishProgress(info);
							knownServers.add(info.getId());
						}
						
						if (multicastLock != null) {
						    multicastLock.release();
						    multicastLock = null;
						}
					}
					catch (Exception e) {
					}
				}
				return null;
			}
			
			@Override
			protected  void onProgressUpdate(TCPServerInfo... values) {
				listAdapter.add(values[0]);
				listAdapter.notifyDataSetChanged();
				isSpinning = true;
			};
			
			@Override
			protected void onPostExecute(Void result) {
				//loadingPanel.setVisibility(View.GONE);
				progressWheel.stopSpinning();
				isSpinning = false;
			};
			
			@Override
			protected void onPreExecute() {
				//loadingPanel.setVisibility(View.VISIBLE);
				progressWheel.spin();
				isSpinning = true;
				listAdapter.clear();
				listAdapter.notifyDataSetChanged();
			};
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Tries to connect to the server with the specified password, updating the listView as well and opening a new activity if successful
	 * @param server
	 * @param password
	 */
	private void tryConnectingToServer(final TCPServerInfo server) {
		if(server.getId().contains("testserver")){
			//opening main
			Intent i = new Intent(context, ActivityMain.class);
			startActivity(i);
		}
		else {
			new AsyncTask<Void, Void, Void>(){
				
				@Override
				protected void onPreExecute() {
					server.setIsConnecting(true);
					listAdapter.notifyDataSetChanged();
				};
				
				@Override
				protected Void doInBackground(Void... params) {
					try{
						if(TCPSecurity.AttemptLogin(server, context)){
							server.setConnectedMessage("Connected");
							
							//opening main
							Intent i = new Intent(context, ActivityMain.class);
							startActivity(i);
						}
						else{
							server.setConnectedMessage("Wrong password");
							dbHandler.deleteServer(server.getId());
						}
					}
					catch(Exception e){
						server.setConnectedMessage("Could not connect");
						Log.e("Kontrol", e.getMessage());
					}
					return null;
				}
				
				@Override
				protected void onPostExecute(Void result) {
					server.setIsConnecting(false);
					listAdapter.notifyDataSetChanged();
				};
			}.execute();
		}
		
	}
}
