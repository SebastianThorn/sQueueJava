import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class sQueueWindows extends JFrame implements ActionListener{

  // Variables
	JTextField searchField = new JTextField(20);
	JLabel searchText = new JLabel("Search here");
	DefaultListModel listModel = new DefaultListModel(); 
	JList trackList = new JList(listModel);
	ListSelectionModel lSModel;
	ArrayList<HashMap<String, String>> menuItems;
	String trackXML;
	String sAddress = "192.168.0.196:8001";
	String user = "Thorn";
	JLabel addressLabel = new JLabel("Server Address:");
	JLabel userLabel = new JLabel("User:");
	JTextField addressField = new JTextField(10);
	JTextField userField = new JTextField(10);

	static final String URL = "http://ws.spotify.com/search/1/track?q=";
	static final String SERVER_ADD = "/add/";

	private XMLParser parser;

	// XML node keys
	static final String KEY_TRACKS = "tracks";
	static final String KEY_TRACK = "track";
	static final String KEY_ARTIST = "artist";
	static final String KEY_ALBUM = "album";
	static final String KEY_NAME = "name";
	static final String KEY_URI = "href";
	static final String KEY_QITEM = "qItem";

	public static void main(String arg[]) {
		sQueueWindows gui = new sQueueWindows();
	}

	sQueueWindows(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 640);
		setVisible(true);
		setTitle("sQueueWindows");
		setLayout(new FlowLayout());
		
		parser = new XMLParser();

		searchField.addActionListener(this);
		trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lSModel = trackList.getSelectionModel();
		lSModel.addListSelectionListener(new ListSelectionChangeHandler());
		
		add(userLabel);
		add(userField);
		add(addressLabel);
		add(addressField);
		add(searchField);
		add(searchText);
		add(trackList);

		userField.setText(user);
		addressField.setText(sAddress);
	}
	
	class ListSelectionChangeHandler implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent arg0) {

			Object selectedValue = trackList.getSelectedValue();

			System.out.println("Number of items: " + listModel.size());
			System.out.println("Has selection? " + trackList.isSelectionEmpty());
			
			if (arg0.getValueIsAdjusting() && trackList.isSelectionEmpty() == false) {
				String uri = menuItems.get(trackList.getSelectedIndex()).get(KEY_URI).toString();
				System.out.println("Clicked qItem: " + selectedValue.toString());
				System.out.println("Clicked index: " + trackList.getSelectedIndex());
				System.out.println("Clicked URI: " + uri);
				
				postTrack(uri);
			}
			trackList.clearSelection();
		}
	}
	
	private void postTrack(final String uri) {
		System.out.println("postTrack(uri) " + uri);
		new Thread(new Runnable() {
			public void run() {
				try {
					DefaultHttpClient httpClient = new DefaultHttpClient();
					String command = "http://" + addressField.getText().toString() + SERVER_ADD + userField.getText().toString() + "/" + uri;
					System.out.println("command: " + command);
					HttpGet httpGet = new HttpGet(command);
					httpClient.execute(httpGet);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == searchField) {
			searchText.setText("Searching for: " + searchField.getText().toString());
			getTracks(URL + searchField.getText().replace(" ", "%20"));
			listModel.removeAllElements();
		} else if (arg0.getSource() == trackList) {
			System.out.println("Clicked on an item");
		}
	}

	private void getTracks(final String address) {
		System.out.println("getTracks(address) " + address);
		new Thread(new Runnable() {
			public void run() {
				try {
					DefaultHttpClient httpClient = new DefaultHttpClient();
					HttpGet httpGet = new HttpGet(address);
					HttpResponse httpResponse = httpClient.execute(httpGet);
					HttpEntity httpEntity = httpResponse.getEntity();
					trackXML = EntityUtils.toString(httpEntity);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Response below @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				//Log.i("Debug", trackXML);
				(new Thread(new getTracksUpdate())).start();
			}
		}).start();
	}

	public class getTracksUpdate implements Runnable {
		public void run() {
			System.out.println("getTracksUpdate()");

			System.out.println("Response type: " + trackXML.substring(0,5));
			if (trackXML.substring(0,5).toString().compareToIgnoreCase("<?xml") == 0) {
				try{
					Document doc = parser.getDomElement(trackXML); // getting DOM element
					menuItems = new ArrayList<HashMap<String, String>>();
					NodeList nl = doc.getElementsByTagName(KEY_TRACK);
					// looping through all item nodes <item>
					for (int i = 0; i < nl.getLength(); i++) {
						HashMap<String, String> map = new HashMap<String, String>();
						Element e = (Element) nl.item(i);

						// Variables
						String track;
						String artist = "";
						String album;
						String uri = "";

						// Trackname and track-URI
						track = parser.getValue(e, KEY_NAME);
						uri = e.getAttribute(KEY_URI);

						// Artist, may be more then one artist per track
						NodeList artists = e.getElementsByTagName(KEY_ARTIST);
						for (int j = 0; j < artists.getLength(); j++) {
							Element f = (Element) artists.item(j);

							if (j != 0) {
								artist += ", ";
							}
							artist += parser.getValue(f, KEY_NAME);
						}

						// Album
						NodeList nAlbum = e.getElementsByTagName(KEY_ALBUM);
						Element f = (Element) nAlbum.item(0);
						album = parser.getValue(f, KEY_NAME);

						System.out.println("-----------------------------------------");
						String out = (artist + " - " + track + ", " + album);
						System.out.println("OUT: " + artist + " - " + track + ", " + album);
						System.out.println("URI: " + uri);

						// adding each child node to HashMap key => value
						map.put(KEY_URI, uri);
						map.put(KEY_QITEM, out);
						map.put(KEY_NAME, track);
						map.put(KEY_ARTIST, artist);
						map.put(KEY_ALBUM, album);

						// adding HashList to ArrayList
						menuItems.add(map);
					}

					// Update the UI
					for (int j = 0; j <menuItems.size(); j++) {
						System.out.println("Item: " + menuItems.get(j).get(KEY_QITEM));
						listModel.addElement(menuItems.get(j).get(KEY_QITEM));
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				System.out.println("Spotify's söktjänst gav ett dåligt svar");
				searchText.setText("Spotify's söktjänst gav ett dåligt svar");
			}
		}
	};
}
