package com.github.projetp1.rs232;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.swing.Timer;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

import com.github.projetp1.Pic;
import com.github.projetp1.Pic.PicMode;
import com.github.projetp1.Settings;

/**
 * The class that manages the RS-232 connection and communications.
 * 
 * @author alexandr.perez
 * @author sebastie.vaucher
 */
public class RS232 implements SerialPortEventListener
{
	/** The Settings */
	protected Settings settings;
	/** The object maintaining the PIC status */
	protected Pic pic;
	/** The serial port object. */
	private SerialPort sp;
	/** The queue of the latest received commands. */
	private ConcurrentLinkedQueue<RS232Command> commandQueue = new ConcurrentLinkedQueue<RS232Command>();
	/** The buffer in which the received datas are temporarily put. */
	private StringBuffer buffer = new StringBuffer();
	private Timer pingTimer = new Timer(PINGDELAY, new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent _arg0)
		{
			sendPing();
		}
	});
	private Timer timeoutTimer = new Timer(PINGTIMEOUT, new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent _e)
		{
			disconnect();
		}
	});

	Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/** The delay between 2 pings */
	private static final int PINGDELAY = 3000;
	/** The delay after which a ping timeout occurs */
	private static final int PINGTIMEOUT = 2000;

	/**
	 * Represents the direction of the arrow that is displayed on the PIC screen.
	 */
	public enum PicArrowDirection
	{
		NORTH(0), NORTHWEST(1), WEST(2), SOUTHWEST(3), SOUTH(4), SOUTHEAST(5), EAST(6), NORTHEAST(7), ONTARGET(8);
		private int number;

		private PicArrowDirection(int _number)
		{
			number = _number;
		}
	}

	/**
	 * Instantiates a new RS232 object.
	 * 
	 * @param _settings
	 *            A Settings object
	 * @param _pic
	 *            A Pic object
	 * @throws Exception
	 *             A generic Exception. May occur if the PIC isn't found.
	 * @throws SerialPortException
	 *             A SerialPortException
	 */
	public RS232(Settings _settings, Pic _pic) throws Exception, SerialPortException
	{
		this.settings = _settings;
		this.pic = _pic;

		/*
		 * this.pic.addObservateur(new Observateur(){ public void update() { //QUE FAIRE DE CET
		 * ANGLE log.info("Mainview to RS232 : OK  " + pic); } });
		 */

		// Initialisation du sp
		String port = null;
		if (settings != null)
			port = settings.getPort();
		if (port == null || port.equals(""))
		{
			try
			{
				port = SerialPortList.getPortNames()[0];
				if (port == null || port.equals(""))
					throw new Exception();
				log.info("The default SerialPort has been selected : " + port);
				settings.setPort(port);
			}
			catch (Exception e)
			{
				log.warning("No SerialPort has been found !");
				throw new SerialPortException("NoPort", "RS232.RS232(MainView)",
						"No RS-232 port found on the computer !");
			}
		}

		this.sp = new SerialPort(port);

		try
		{
			this.sp.openPort();// Open serial port
			this.sp.setParams(settings.getSpeed(), settings.getDatabit(),
					settings.getStopbit(), settings.getParity());
			this.sp.setFlowControlMode(settings.getFlowControl());
			this.sp.setEventsMask(SerialPort.MASK_RXCHAR);// Set mask
			this.sp.addEventListener(this);// Add SerialPortEventListener
		}
		catch (SerialPortException ex)
		{
			log.warning("SerialPortException at port opening : " + ex.getMessage());
			throw ex;
		}

		timeoutTimer.setRepeats(false);
		timeoutTimer.setInitialDelay(PINGTIMEOUT);
		pingTimer.setRepeats(true);
		pingTimer.setInitialDelay(PINGDELAY);

		if (!this.sendPing())
		{
			Exception ex = new Exception("The initial ping could not be sent, aborting...");
			log.warning(ex.getMessage());
			throw ex;
		}

		log.info("Serial port opened");

		log.info("RS232 object created, waiting for the ping reply to finish");
	}

	/**
	 * Send an EMPTY command to the PIC and resets the timeout
	 * 
	 * @return True if it succeeds, false otherwise
	 */
	protected Boolean sendPing()
	{
		try
		{
			this.sendFrame(RS232CommandType.EMPTY, "");
			timeoutTimer.restart();
			log.info("Ping sent");
			return true;
		}
		catch (SerialPortException ex)
		{
			log.warning("Unable to send ping");
			disconnect();
			return false;
		}
	}

	/**
	 * Disconnect from the PIC (cancel ping scheduling)
	 */
	private void disconnect()
	{
		log.info("Disconnecting the PIC");

		pingTimer.stop();
		timeoutTimer.stop();

		pic.setMode(PicMode.SIMULATION);
	}

	/**
	 * Indicate to this object that the operation mode has changed
	 * 
	 * @param _mode
	 *            The new mode of operation
	 * @throws SerialPortException
	 *             If the PIC can not be reached
	 */
	public void modeHasChanged(PicMode _mode) throws SerialPortException
	{
		pic.setMode(_mode);
		
		switch (_mode)
		{
			case SIMULATION:
				disconnect();
				break;
			case GUIDING:
				sendArrowToPic(PicArrowDirection.ONTARGET);
				break;
			case POINTING:
				sendFrame(RS232CommandType.CHANGE_TO_POINT_MODE, "");
				break;
			default:
				log.warning("Mode not yet supported, the state may be inaccurate !");
				disconnect();
				break;
		}

		log.info("PIC mode switched to : " + _mode);
	}

	/**
	 * Send the direction of the arrow to the PIC.
	 * 
	 * @param _dir
	 *            The direction of the arrow
	 * @throws SerialPortException
	 */
	public void sendArrowToPic(PicArrowDirection _dir) throws SerialPortException
	{
		this.sendFrame(RS232CommandType.CHANGE_TO_ARROW_MODE, String.valueOf(_dir.number));
	}

	/**
	 * Send an NCK message to the PIC informing that a packet has been corrupted.
	 * 
	 * @param command
	 *            The command number
	 */
	public void sendNck(RS232CommandType command)
	{
		if (command == null || pic.getMode() == PicMode.GUIDING
				|| command.equals(RS232CommandType.PIC_STATUS))
			return;

		try
		{
			sendFrame(command, "");
		}
		catch (SerialPortException ex)
		{
			log.warning("Unable to send a NCK on port " + ex.getPortName());
		}
	}

	/**
	 * Send a frame to the PIC
	 * 
	 * @param cNum
	 *            The command to send
	 * @param datas
	 *            The datas (can be empty)
	 * @throws SerialPortException
	 *             If the SerialPort cannot be written
	 */
	public synchronized void sendFrame(RS232CommandType cNum, String datas)
			throws SerialPortException
	{
		String trame = cNum.toString() + "," + datas;
		trame = "$" + trame + "*" + hexToAscii(RS232.computeCrc(trame)) + "\r\n";
		sp.writeString(trame);
		log.info("Frame sent : " + trame);
	}

	/**
	 * @see jssc.SerialPortEventListener#serialEvent(jssc.SerialPortEvent)
	 */
	@Override
	public void serialEvent(SerialPortEvent e)
	{
		// Callback du SerialPort
		if (!e.isRXCHAR()) // If no data is available
			return;

		pingTimer.restart();
		String received;
		try
		{
			received = sp.readString();
			if (received == null)
				return;
			log.fine("Data received : " + received);
		}
		catch (SerialPortException ex)
		{
			log.warning("Port " + ex.getPortName()
					+ " unreadable. Please check that we are the only one listening to this port.");
			return;
		}

		buffer.append(received);

		int pos;
		boolean newComs = false;

		while ((pos = buffer.indexOf("\r\n")) != -1)
		{
			RS232Command com;
			String chain = buffer.substring(0, pos + 2);
			buffer.delete(0, pos + 2);
			log.info("Trame complète : '" + chain + "'");
			try
			{
				com = new RS232Command(chain);
				if (com.getCommandNumber() != RS232CommandType.EMPTY)
				{
					commandQueue.add(com);
					newComs = true;
				}
				else
				{
					timeoutTimer.stop();
					log.info("Ping received");
				}
			}
			catch (CrcException e1)
			{
				this.sendNck(RS232Command.extractCommand(chain));
				log.info("Frame corrupted");
			}
			catch (Exception e1)
			{
				log.warning("Exception while trying to decode frame : " + e1.getMessage());
			}
		}

		if (pic.getMode() == PicMode.SIMULATION)
			pic.setMode(PicMode.POINTING);
		
		if (newComs)
			pic.run();
	}

	/**
	 * Gets the latest command received from the PIC and removes it from the queue.
	 * 
	 * @return The latest command
	 */
	public RS232Command getLastCommand()
	{
		return commandQueue.poll();
	}

	/**
	 * Check the CRC against the raw string received via RS-232
	 * 
	 * @param datas
	 *            The data against which to check the CRC
	 * @param crc
	 *            The received CRC
	 * @return True if it matches or false otherwise
	 */
	public static boolean checkCrc(String datas, byte crc)
	{
		return crc == (computeCrc(datas));
	}

	/**
	 * Returns the computed CRC of the string following NMEA-0183 method
	 * 
	 * @param datas
	 *            The String of which to compute the CRC
	 * @return The computed CRC as a byte
	 */
	public static byte computeCrc(String datas)
	{
		byte crc = 0;

		for (byte b : datas.getBytes())
		{
			crc = (byte) (crc ^ b);
		}
		return crc;
	}

	/**
	 * Convert a byte to an ASCII hex string
	 * 
	 * @param b
	 *            The byte to be converted
	 * @return A String
	 */
	public static String hexToAscii(byte b)
	{
		return Integer.toString((b & 0xff) + 0x100, 16).substring(1).toUpperCase();
	}
}
