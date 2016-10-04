package server;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.bluetooth.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.microedition.io.*;

/**
 * Class that implements an SPP Server which accepts single line of
 * message from an SPP client and sends a single line of response to the client.
 */
public class Server {
	private static final String PC_CMD_LEFT = "left";
	private static final String PC_CMD_RIGHT = "right";
	private static final String PC_CMD_PEBBLE_LEFT = "pleft";
	private static final String PC_CMD_PEBBLE_RIGHT = "pright";
	private static final String PC_CMD_POINT_CLOSE = "point close";
	private static final String PC_CMD_MONITOR = "monitor";
	private static final String PC_CMD_QUIT = "quit";

	private int monitor = 0;

	//start server
	private void runServer() throws IOException{
		StreamConnectionNotifier streamConnNotifier = connectToClient();

		//Wait for client connection
		print("\nServer Started. Waiting for clients to connect...");
		StreamConnection connection=streamConnNotifier.acceptAndOpen();

//		RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
		//print("Remote device address: "+dev.getBluetoothAddress());
		//print("Remote device name: "+dev.getFriendlyName(true));
		
		listening(connection);
	}

	private void listening(StreamConnection connection) throws IOException {
		try {
			//In out streams
			OutputStream outStream = connection.openOutputStream();
			PrintWriter pWriter = new PrintWriter(new OutputStreamWriter(outStream));
			InputStream inStream = connection.openInputStream();
			BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
			
			//Computer Control stuff
			Robot robot = new Robot();
			ArrayList<ShapedWindow> swList = new ArrayList<ShapedWindow>();
			for (int i = 0; i < 5; ++i) {
				swList.add(new ShapedWindow());
			}
			Pattern p = Pattern.compile("Points: ([0-9])");//[0-9\.]+
			ImageOutputStream ios = ImageIO.createImageOutputStream(outStream);
			sendImage(robot, pWriter, ios);
			print(System.getProperty("os.name"));
			
			while (true) {
				String lineRead = bReader.readLine();
				Matcher m = p.matcher(lineRead);
				print(lineRead);

				if (lineRead.equals(PC_CMD_QUIT)) {
					break;
				} else if (lineRead.equals(PC_CMD_LEFT) || 
						lineRead.equals(PC_CMD_PEBBLE_LEFT)) {
					robot.keyPress(KeyEvent.VK_LEFT);
					robot.keyRelease(KeyEvent.VK_LEFT);
				} else if (lineRead.equals(PC_CMD_RIGHT) || 
						lineRead.equals(PC_CMD_PEBBLE_RIGHT)) {
					robot.keyPress(KeyEvent.VK_RIGHT);
					robot.keyRelease(KeyEvent.VK_RIGHT);
					
				} else if (m.find()) { // creation and movement
					int pCount = Integer.parseInt(m.group(1));
					pointer(swList, bReader, pCount);
				} else if (lineRead.equals(PC_CMD_POINT_CLOSE)) { // end
					for (ShapedWindow sw : swList) {
						if (sw != null) {
							//sw.dispose();
							sw.setOpacity(0);
						}
					}
					//swList.clear();
				} else if (lineRead.equals(PC_CMD_MONITOR)) {
					++monitor;
					sendImage(robot, pWriter, ios);
				} else {
					continue;
				}
				if (lineRead.equals(PC_CMD_LEFT) || lineRead.equals(PC_CMD_RIGHT)) {
					sendImage(robot, pWriter, ios);
				}
			}
			inStream.close();

			pWriter.close();
			outStream.close();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	private void pointer(ArrayList<ShapedWindow> swList, BufferedReader br, int pCount)
			throws IOException {
		GraphicsDevice[] gd = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getScreenDevices();
		if (monitor >= gd.length) {
			monitor = 0;
		}
		Rectangle monRect = gd[monitor].getDefaultConfiguration().getBounds();
//		int width = gd[monitor].getDisplayMode().getWidth();
//		int height = gd[monitor].getDisplayMode().getHeight();
		
		Pattern pp = Pattern.compile("Point: ([0-9]): (.+),(.+)");
		for (int i = swList.size(); i <= pCount; ++i) {
			swList.add(null);
		}
		for (int i = 0; i < pCount; i++) {
			String lineRead = br.readLine();
			Matcher mm = pp.matcher(lineRead);
			mm.find();
			int pIndex = Integer.parseInt(mm.group(1));
			if (swList.get(pIndex) == null) {
				swList.set(pIndex,new ShapedWindow());
			}
			ShapedWindow sw = swList.get(pIndex);
			double x = monRect.x + Double.parseDouble(mm.group(2))*monRect.width
					-ShapedWindow.RADIUS;
			double y = monRect.y + Double.parseDouble(mm.group(3))*monRect.height
					-ShapedWindow.RADIUS;
			sw.setLocation((int)x,(int)y);
			sw.setOpacity(0.7f);
		}
	}
	
	private void sendImage(Robot robot, PrintWriter pWriter, final ImageOutputStream ios) 
			throws IOException, AWTException {
		if (!System.getProperty("os.name").contains("Windows")) {
			return;
		}
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();
		
		print("numScreens: " + screens.length);
		print("monitor: " + monitor);
		if (monitor >= screens.length) {
			monitor = 0;
		}
		
		Rectangle rectangle = new Rectangle(
				screens[monitor].getDefaultConfiguration().getBounds());
		final BufferedImage image = robot.createScreenCapture(rectangle);

		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
	    ImageIO.write(image, "jpg", tmp);
	    tmp.close();
	    int estLength = tmp.size();
	    
		pWriter.write("image\n");
		pWriter.write(estLength+"\n");
		print("fileSize: "+estLength);
		pWriter.flush();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ImageIO.write(image, "jpg", ios);
				} catch (IOException e) {
					e.printStackTrace();
				}
				print("Image sent");
			}
		});
	}
	
	private StreamConnectionNotifier connectToClient() throws IOException {
		//Create a UUID for SPP
		UUID uuid = new UUID("1101", true);
		//Create the servicve url
		String connectionString = "btspp://localhost:" + uuid +";name=Sample SPP Server";

		//open server url
		StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier)Connector.open( connectionString );

		return streamConnNotifier;
	}

	public static void main(String[] args) throws IOException {
		ShapedWindow.checkTransparency();
		//display local device address and name
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		print("Address: "+localDevice.getBluetoothAddress());
		print("Name: "+localDevice.getFriendlyName());

		Server server=new Server();
		server.runServer();
	}
	
	private static void print(String s) {
		System.out.println(s);
	}
	
}