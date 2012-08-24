/**
 * 
 */
package com.github.projetp1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * @author alexandr.perez and issa.barbier
 * 
 */
@SuppressWarnings("serial")
public class MainView extends JFrame implements KeyListener
{

	private Settings settings;

	public Settings getSettings()
	{
		return settings;
	}

	private Pic pic;

	public Pic getPic()
	{
		return pic;
	}

	private DataBase db = null;

	public DataBase getDataBase()
	{
		return db;
	}

	SkyMap skymap;

	private Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private Compass compassPanel;
	private Inclinometer inclinometerPanel;
	private Buttons buttonsPanel;
	private SearchBar searchBarPanel;
	private ZoomBar zoomBarPanel;
	private Help helpPanel;
	private SettingsConfig settingsPanel;
	private JLabel coordinate;
	private JLabel leftPanel;
	private int zoom = 2;
	private double angInclinometer;
	private double degCompass;
	private double xOrigin = 0;
	private double yOrigin = 0;
	private double scalar = 0.1;
	private double scalar_old = scalar;

	/**
	 * return the width of the main window.
	 */
	private double width()
	{
		return this.getWidth();
	}

	/**
	 * return the height of the main window.
	 */
	private double height()
	{
		return this.getHeight();
	}

	/**
	 * This is the timer for update the values form the pic.
	 */
	private Timer createTimer()
	{
		// Création d'une instance de listener
		// associée au timer

		ActionListener action = new ActionListener()
		{
			// Méthode appelée à chaque tic du timer
			@Override
			public void actionPerformed(ActionEvent event)
			{
				double degree = 0.0;
				if (pic != null)
					degree = pic.getAzimuth();

				compassPanel.setGreenNeedle(degree);

				compassPanel.setRedNeedle(degCompass);
				inclinometerPanel.setRedNeedle(angInclinometer);

				compassPanel.update(scalar);
				inclinometerPanel.update(scalar);

				if (pic != null)
				{
					inclinometerPanel.setGreenNeedle(pic.getPitch());

					char hemNS = 'N', hemWE = 'E';
					double lat = pic.getLatitude(), lon = pic.getLongitude();

					if (lat < 0.0)
						hemNS = 'S';
					if (lon < 0.0)
						hemWE = 'W';
					coordinate.setText(Math.abs(lat) + "° " + hemNS + ", " + Math.abs(lon) + "° " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							+ hemWE);
				}

				compassPanel.setLocation((int) (width() - compassPanel.getWidth()) - 20, 50);
				inclinometerPanel.setLocation(
						(int) (width() - compassPanel.getWidth() + (scalar * 70)),
						(100 + inclinometerPanel.getHeight()));
				coordinate.setBounds((int) width() - 100, (int) height() - 70, 100, 20);

			}
		};
		return new Timer(50, action);
	}

	/**
	 * Constructor
	 */
	public MainView()
	{
		try
		{
			db = new DataBase("hyg.db", ";"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		this.addKeyListener(this);
		// TODO : mettre des valeurs non arbitraire.
		leftPanel = new JLabel(""); //$NON-NLS-1$
		leftPanel.setBounds(100, 100, 100, 200);
		leftPanel.setForeground(new Color(250, 250, 250));
		getLayeredPane().add(leftPanel);

		settings = new Settings();

		coordinate = new JLabel(0 + Messages.getString("MainView.DegreesNorth") + 0 + Messages.getString("MainView.DegreesSouth")); //$NON-NLS-1$ //$NON-NLS-2$
		coordinate.setBounds(this.getWidth() - 200, this.getHeight() - 20, 200, 20);
		coordinate.setForeground(Color.WHITE);
		getLayeredPane().add(coordinate);

		buttonsPanel = new Buttons(scalar);
		buttonsPanel.setLocation((int) (width() / 2 - buttonsPanel.getWidth() / 2), 5);

		helpPanel = new Help(scalar);
		helpPanel.setLocation((int) (width() / 2 - buttonsPanel.getWidth() / 2 - 10 * scalar),
				buttonsPanel.getHeight());

		settingsPanel = new SettingsConfig(scalar);
		settingsPanel.setLocation((int) (width() / 2 - 2 * buttonsPanel.getWidth()),
				buttonsPanel.getHeight());

		searchBarPanel = new SearchBar(scalar);
		searchBarPanel.setLocation(0, 5);

		zoomBarPanel = new ZoomBar(scalar);
		zoomBarPanel.setLocation(5, 5);

		compassPanel = new Compass(scalar);
		compassPanel.setLocation((int) (width() - 10 - compassPanel.getWidth()), 50);

		inclinometerPanel = new Inclinometer(scalar);
		inclinometerPanel.setLocation((int) (width() - 10 - inclinometerPanel.getWidth()),
				(100 + inclinometerPanel.getHeight()));

		skymap = new SkyMap(this);

		this.setFocusable(true);

		skymap.setSize(this.getWidth() - 200, this.getHeight() - 20);
		skymap.setLocation(200, 20);
		skymap.updateSkyMap();
		skymap.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt)
			{
				skymapMouseClicked(evt);
			}
		});

		getLayeredPane().add(buttonsPanel);
		getLayeredPane().add(searchBarPanel);
		getLayeredPane().add(helpPanel);
		getLayeredPane().add(settingsPanel);
		getLayeredPane().add(zoomBarPanel);
		getLayeredPane().add(compassPanel);
		getLayeredPane().add(inclinometerPanel);
		getLayeredPane().add(skymap);

		this.setMinimumSize(new java.awt.Dimension(800, 600));

		this.setExtendedState(Frame.MAXIMIZED_BOTH);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Color l_BackgroundColor = new Color(5, 30, 50);
		this.getContentPane().setBackground(l_BackgroundColor);

		Timer timer = createTimer();
		timer.start();

		this.addComponentListener(new java.awt.event.ComponentAdapter()
		{
			@Override
			public void componentResized(java.awt.event.ComponentEvent evt)
			{
				formComponentResized(evt);
			}
		});

		this.setVisible(true);
		pic = new Pic(this);
		pic.addObservateur(new Observateur()
		{
			@Override
			public void update()
			{
				skymap.updateSkyMap();
			}
		});
	}

	/**
	 * When the user click on the skymap, the other window hide and the skymap will be selected.
	 */
	private void skymapMouseClicked(java.awt.event.MouseEvent evt)
	{
		settingsPanel.setVisible(false);
		helpPanel.setVisible(false);
		searchBarPanel.jScrollPane.setVisible(false);
		skymap.transferFocusBackward();
	}

	/**
	 * 
	 */
	@Override
	public void keyTyped(KeyEvent evt)
	{
	}

	/**
	 * 
	 */
	@Override
	public void keyReleased(KeyEvent evt)
	{
	}

	/**
	 * navigation on the skymap.
	 */
	@Override
	public void keyPressed(KeyEvent evt)
	{
		float l_fDelta = (float) (0.05 / zoom);
		if (evt.getKeyCode() == 37) // Left
		{
			if (xOrigin > -1)
				xOrigin -= l_fDelta;
		}
		else if (evt.getKeyCode() == 39) // Right
		{
			if (xOrigin < 1)
				xOrigin += l_fDelta;
		}
		else if (evt.getKeyCode() == 38) // Up
		{
			if (yOrigin < 1)
				yOrigin += l_fDelta;
		}
		else if (evt.getKeyCode() == 40) // Down
		{
			if (yOrigin > -1)
				yOrigin -= l_fDelta;
		}
		else if (evt.getKeyCode() == '.') // zoom +
		{
			zoom++;
		}
		else if (evt.getKeyCode() == '-') // zoom -
		{
			if (zoom > 1)
				zoom--;
		}

		zoomBarPanel.zoomSlider.setValue(zoom);

		skymap.setZoom(zoom);
		skymap.setXOrigin(xOrigin);
		skymap.setYOrigin(yOrigin);
		skymap.updateSkyMap();
	}

	/**
	 * Update the information of the star in the leftPanel.
	 */
	public void updateInfo(CelestialObject _object)
	{
		if (_object != null)
		{
			leftPanel.setText("<html>" + Messages.getString("MainView.StarName") + "<br />" + _object.getProperName() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "<br /><br />" + Messages.getString("MainView.Magnitude") + "<br />" + _object.getMag() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "<br /><br />" + Messages.getString("MainView.DistanceToEarth") + "<br />" + (int) (_object.getDistance() * 3.2616) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ Messages.getString("MainView.LY") + "<br /><br />" + Messages.getString("MainView.Colour") + "<br />" + _object.getColorIndex() + "</html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	/**
	 * calcul the beast scalar for resize the component
	 */
	private double calculateScale()
	{
		double w = width() * 0.15 / 345;
		double h = height() * 0.30 / 350;
		if (w > h)
			w = h;
		if (w < .1)
			w = .1;
		return w;
	}

	/**
	 * resize all the component
	 */
	private void formComponentResized(java.awt.event.ComponentEvent evt)
	{

		scalar = calculateScale();

		if (scalar - scalar_old > 0.001 || scalar - scalar_old < -0.001)
		{
			scalar_old = scalar;

			buttonsPanel.update(scalar / 3);
			compassPanel.update(scalar);
			inclinometerPanel.update(scalar);
			searchBarPanel.update(scalar);
			zoomBarPanel.update(scalar);
			helpPanel.update(scalar);
			settingsPanel.update(scalar);

			buttonsPanel.setLocation((int) (width() / 2 - buttonsPanel.getWidth() + (scalar * 70)),
					5);
			helpPanel.setLocation(
					(int) (width() / 2 - buttonsPanel.getWidth() + (scalar * 70) - 10 * scalar),
					buttonsPanel.getHeight() + (int) (20 * scalar));
			settingsPanel.setLocation((int) (width() / 2 - settingsPanel.getWidth() + 80 * scalar),
					buttonsPanel.getHeight() + (int) (20 * scalar));
			searchBarPanel.setLocation(
					(int) (width() / 2 + buttonsPanel.getWidth() - (scalar * 70)),
					(buttonsPanel.getHeight() / 2 - 10) + 5);
			zoomBarPanel.setLocation(5,
					(buttonsPanel.getHeight() / 2 - zoomBarPanel.getHeight() / 2) + 5);

			skymap.setBounds(0, 0, this.getWidth(), this.getHeight());
			skymap.setZoom(zoom);

			leftPanel.setBounds((int) (10 * scalar), (int) (10 * scalar), 150, this.getHeight());

			compassPanel.setLocation((int) (width() - compassPanel.getWidth()) - 20, 50);
			inclinometerPanel.setLocation(
					(int) (width() - compassPanel.getWidth() + (scalar * 70)),
					(100 + inclinometerPanel.getHeight()));
			coordinate.setBounds(this.getWidth() - 100, this.getHeight() - 70, 100, 20);

		}
	}

	/**
	 * resize an image by a scalar
	 */
	public static BufferedImage resizeImage(BufferedImage bImage, double _scale)
	{
		int destWidth = (int) (_scale * bImage.getWidth());
		int destHeight = (int) (_scale * bImage.getHeight());
		GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage bImageNew = configuration.createCompatibleImage(destWidth, destHeight, 2);
		Graphics2D graphics = bImageNew.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(bImage, 0, 0, destWidth, destHeight, 0, 0, bImage.getWidth(),
				bImage.getHeight(), null);
		graphics.dispose();

		return bImageNew;
	}

	/**
	 * resize the image by a arbitrary size
	 */
	public static BufferedImage resizeImage2(BufferedImage bImage, double w, double h)
	{
		int destWidth = (int) (w);// *bImage.getWidth());
		int destHeight = (int) (h);// *bImage.getHeight());
		GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage bImageNew = configuration.createCompatibleImage(destWidth, destHeight, 2);
		Graphics2D graphics = bImageNew.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(bImage, 0, 0, destWidth, destHeight, 0, 0, bImage.getWidth(),
				bImage.getHeight(), null);
		graphics.dispose();

		return bImageNew;
	}

	/**
	 * The Buttons class
	 */
	private class Buttons extends JLayeredPane
	{
		double scale;
		BufferedImage imgSettings;
		BufferedImage imgHelp;

		public Buttons(double _scale)
		{
			scale = _scale;
			try
			{
				imgSettings = resizeImage(ImageIO.read(new File("res/SettingsIcon.png")), scale); //$NON-NLS-1$
				imgHelp = resizeImage(ImageIO.read(new File("res/HelpIcon.png")), scale); //$NON-NLS-1$
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			this.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent evt)
				{
					MouseClicked(evt);
				}
			});
			this.setBounds(0, 0, (imgSettings.getWidth() * 2), (imgHelp.getHeight()));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.drawImage(imgSettings, 0, 0, null);
			g2.drawImage(imgHelp, (int) (imgSettings.getWidth() + 10 * scale), 0, null);
		}

		private void MouseClicked(java.awt.event.MouseEvent evt)
		{

			if (evt.getX() < buttonsPanel.getWidth() / 2)
			{
				if (settingsPanel.isVisible())
					settingsPanel.setVisible(false);
				else
				{
					if (helpPanel.isVisible())
						helpPanel.setVisible(false);
					settingsPanel.setVisible(true);
				}
			}
			else
			{
				if (helpPanel.isVisible())
					helpPanel.setVisible(false);
				else
				{
					if (settingsPanel.isVisible())
						settingsPanel.setVisible(false);
					helpPanel.setVisible(true);
				}
			}
		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;
			try
			{
				imgSettings = resizeImage(ImageIO.read(new File("res/SettingsIcon.png")), scale); //$NON-NLS-1$
				imgHelp = resizeImage(ImageIO.read(new File("res/HelpIcon.png")), scale); //$NON-NLS-1$
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			this.setBounds(0, 0, (imgSettings.getWidth() * 2), (imgHelp.getHeight()));
		}
	}

	/**
	 * The SettingsConfig class
	 */
	private class SettingsConfig extends JLayeredPane
	{
		double scale;
		int number = 10;
		BufferedImage backgroundTop;
		BufferedImage backgroundMid;
		BufferedImage backgroundBot;
		BufferedImage InternalTop;
		BufferedImage[] InternalMid = new BufferedImage[number];

		JLabel titre;
		JLabel[] settingList = {
				new JLabel(Messages.getString("MainView.Port")),
				new JLabel(Messages.getString("MainView.Speed")),
				new JLabel(Messages.getString("MainView.Databits")),
				new JLabel(Messages.getString("MainView.Stopbits")),
				new JLabel(Messages.getString("MainView.Parity")),
				new JLabel(Messages.getString("MainView.FlowControl")),
				new JLabel(Messages.getString("MainView.SamplingRate")),
				new JLabel(Messages.getString("MainView.DatabaseName")),
				new JLabel(Messages.getString("MainView.InputDelimiter")),
				new JLabel(Messages.getString("MainView.Simulation"))
			};
		JComboBox[] comboBoxList = new JComboBox[number];

		BufferedImage InternalBot;

		/**
		 * Constructor
		 * 
		 * @param _scale
		 */
		public SettingsConfig(double _scale)
		{
			scale = _scale;

			String port[] = jssc.SerialPortList.getPortNames();
			comboBoxList[0] = new JComboBox<String>(port);
			comboBoxList[0].setSelectedItem(settings.getPort());
			String speed[] = { "110", "300", "600", "1200", "4800", "9600", "14400", "19200", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
					"38400", "57600", "115200", "128000", "256000" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			comboBoxList[1] = new JComboBox<String>(speed);
			comboBoxList[1].setSelectedItem(String.valueOf(settings.getSpeed()));
			String databit[] = { "5", "6", "7", "8" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			comboBoxList[2] = new JComboBox<String>(databit);
			comboBoxList[2].setSelectedItem(String.valueOf(settings.getDatabit()));
			String stopbit[] = { "1", "2", "1_5" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			comboBoxList[3] = new JComboBox<String>(stopbit);
			comboBoxList[3].setSelectedItem(String.valueOf(settings.getStopbit()));
			String parity[] = { Messages.getString("MainView.None"), Messages.getString("MainView.Odd"), Messages.getString("MainView.Even"), Messages.getString("MainView.Mark"), Messages.getString("MainView.Space") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			comboBoxList[4] = new JComboBox<String>(parity);
			comboBoxList[4].setSelectedItem(settings.getParity());
			String flowControl[] = { Messages.getString("MainView.None"), Messages.getString("MainView.RTSCTS_IN"), Messages.getString("MainView.RTSCTS_OUT"), Messages.getString("MainView.XONXOFF_IN"), Messages.getString("MainView.XONXOFF_OUT") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			comboBoxList[5] = new JComboBox<String>(flowControl);
			comboBoxList[5].setSelectedItem(settings.getFlowControl());
			String samplingRate[] = { "25" }; //$NON-NLS-1$
			comboBoxList[6] = new JComboBox<String>(samplingRate);
			comboBoxList[6].setSelectedItem(String.valueOf(settings.getSamplingRate()));
			String databaseName[] = { "hyz.db" }; //$NON-NLS-1$
			comboBoxList[7] = new JComboBox<String>(databaseName);
			comboBoxList[7].setSelectedItem(settings.getDatabaseName());
			String imputDelimiter[] = { ";", ":" }; //$NON-NLS-1$ //$NON-NLS-2$
			comboBoxList[8] = new JComboBox<String>(imputDelimiter);
			comboBoxList[8].setSelectedItem(settings.getInputDelimiter());
			String simulation[] = { Messages.getString("MainView.On"), Messages.getString("MainView.Off") }; //$NON-NLS-1$ //$NON-NLS-2$
			comboBoxList[9] = new JComboBox<String>(simulation);
			comboBoxList[9].setSelectedItem((settings.getSimulation()) ? Messages.getString("MainView.On") : Messages.getString("MainView.Off")); //$NON-NLS-1$ //$NON-NLS-2$

			for (int i = 0; i < number; i++)
			{
				comboBoxList[i].addActionListener(new java.awt.event.ActionListener()
				{
					@Override
					public void actionPerformed(java.awt.event.ActionEvent evt)
					{
						jComboBox1ActionPerformed(evt);
					}
				});
			}

			try
			{
				backgroundTop = resizeImage(
						ImageIO.read(new File("res/settings-top-background.png")), scale / 2); //$NON-NLS-1$
				backgroundMid = resizeImage(
						ImageIO.read(new File("res/settings-mid-background.png")), scale / 2); //$NON-NLS-1$
				backgroundBot = resizeImage(
						ImageIO.read(new File("res/settings-bot-background.png")), scale / 2); //$NON-NLS-1$
				InternalTop = resizeImage(ImageIO.read(new File("res/settings-top-internal.png")), //$NON-NLS-1$
						scale / 2);
				for (int i = 0; i < number; i++)
				{
					InternalMid[i] = resizeImage(
							ImageIO.read(new File("res/settings-mid-internal.png")), scale / 2); //$NON-NLS-1$

					settingList[i].setBounds(
							(backgroundTop.getWidth() / 2 - InternalTop.getWidth() / 2),
							backgroundTop.getHeight() + InternalTop.getHeight() + i
									* InternalMid[0].getHeight() + 25, (int) (500 * scale), 30);
					settingList[i].setFont(new Font("Calibri", Font.BOLD, (int) (36 * scale))); //$NON-NLS-1$
					settingList[i].setForeground(Color.BLACK);
					this.add(settingList[i]);
					comboBoxList[i].setBounds((int) (backgroundTop.getWidth() / 2 - 100 * scale),
							backgroundTop.getHeight() + InternalTop.getHeight() + i
									* InternalMid[0].getHeight() + 25, (int) (100 * scale), 30);
					this.add(comboBoxList[i]);

				}
				InternalBot = resizeImage(ImageIO.read(new File("res/settings-bot-internal.png")), //$NON-NLS-1$
						scale / 2);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			titre = new JLabel(Messages.getString("MainView.Settings"), SwingConstants.CENTER); //$NON-NLS-1$
			titre.setFont(new Font("Calibri", Font.BOLD, (int) (36 * scale))); //$NON-NLS-1$
			titre.setBounds(0, backgroundTop.getHeight(), (int) (scale * 345), (int) (scale * 34));
			titre.setForeground(Color.WHITE);
			this.add(titre);

			this.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent evt)
				{
					MouseClicked(evt);
				}
			});
			this.setBounds(0, 0, (backgroundTop.getWidth() * 2), (int) (500 * scale));
			this.setVisible(false);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.drawImage(backgroundTop, 0, 0, null);
			g2.drawImage(backgroundMid, 0, backgroundTop.getHeight(), null);
			g2.drawImage(backgroundBot, 0, backgroundTop.getHeight() + backgroundMid.getHeight(),
					null);
			g2.drawImage(InternalTop, (backgroundTop.getWidth() / 2 - InternalTop.getWidth() / 2),
					backgroundTop.getHeight() + titre.getHeight(), null);
			for (int i = 0; i < number; i++)
			{
				g2.drawImage(
						InternalMid[i],
						(backgroundTop.getWidth() / 2 - InternalTop.getWidth() / 2),
						backgroundTop.getHeight() + InternalTop.getHeight() + i
								* InternalMid[0].getHeight() + titre.getHeight(), null);

			}
			g2.drawImage(
					InternalBot,
					(backgroundTop.getWidth() / 2 - InternalTop.getWidth() / 2),
					backgroundTop.getHeight() + InternalTop.getHeight() + number
							* InternalMid[0].getHeight() + titre.getHeight(), null);
		}

		/**
		 * this method was here only for avoid the user to click on the skymap behind the widows.
		 * 
		 * @param evt
		 */
		private void MouseClicked(java.awt.event.MouseEvent evt)
		{
			// nothing
		}

		private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt)
		{

			settings.setPort((comboBoxList[0].getSelectedItem() != null) ? comboBoxList[0]
					.getSelectedItem().toString() : Messages.getString("MainView.None")); //$NON-NLS-1$
			settings.setSpeed(Integer.parseInt(comboBoxList[1].getSelectedItem().toString()));
			settings.setDatabit(Integer.parseInt(comboBoxList[2].getSelectedItem().toString()));
			settings.setStopbit(Integer.parseInt(comboBoxList[3].getSelectedItem().toString()));
			settings.setParity(comboBoxList[4].getSelectedItem().toString());
			settings.setFlowControl(comboBoxList[5].getSelectedItem().toString());
			settings.setSamplingRate(Integer.parseInt(comboBoxList[6].getSelectedItem().toString()));
			settings.setDatabaseName(comboBoxList[6].getSelectedItem().toString());
			settings.setInputDelimiter(comboBoxList[7].getSelectedItem().toString());
			settings.setSimulation((comboBoxList[8].getSelectedItem().toString().equals(Messages.getString("MainView.On"))) ? true //$NON-NLS-1$
					: false);
			Serializer.serialize("settings.conf", settings); //$NON-NLS-1$

		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;

			try
			{
				backgroundTop = resizeImage(
						ImageIO.read(new File("res/settings-top-background.png")), scale / 2); //$NON-NLS-1$

				titre.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 36))); //$NON-NLS-1$
				titre.setBounds(0, backgroundTop.getHeight(), backgroundTop.getWidth(),
						(int) (scale * 35));

				backgroundBot = resizeImage(
						ImageIO.read(new File("res/settings-bot-background.png")), scale / 2); //$NON-NLS-1$
				InternalTop = resizeImage(ImageIO.read(new File("res/settings-top-internal.png")), //$NON-NLS-1$
						scale / 2);
				for (int i = 0; i < number; i++)
				{
					InternalMid[i] = resizeImage(
							ImageIO.read(new File("res/settings-mid-internal.png")), scale / 2); //$NON-NLS-1$
					settingList[i]
							.setBounds((int) (backgroundTop.getWidth() / 2 - InternalTop.getWidth()
									/ 2 + 30 * scale),
									backgroundTop.getHeight() + InternalTop.getHeight() + i
											* InternalMid[0].getHeight() + titre.getHeight()
											+ (int) (10 * scale), (int) (500 * scale),
									(int) (30 * scale));
					settingList[i].setFont(new Font("Calibri", Font.BOLD, (int) (36 * scale))); //$NON-NLS-1$
					comboBoxList[i].setBounds((int) (backgroundTop.getWidth() - 300 * scale),
							backgroundTop.getHeight() + InternalTop.getHeight() + i
									* InternalMid[0].getHeight() + titre.getHeight()
									+ (int) (4 * scale), (int) (250 * scale), (int) (40 * scale));
					comboBoxList[i].setFont(new Font("Calibri", Font.BOLD, (int) (25 * scale))); //$NON-NLS-1$
				}
				backgroundMid = resizeImage2(
						ImageIO.read(new File("res/settings-mid-background.png")), //$NON-NLS-1$
						backgroundTop.getWidth(),
						(number + 2) * InternalMid[0].getHeight() + titre.getHeight()
								+ (int) (15 * scale));
				InternalBot = resizeImage(ImageIO.read(new File("res/settings-bot-internal.png")), //$NON-NLS-1$
						scale / 2);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			this.setBounds(0, 0, (backgroundTop.getWidth()), backgroundTop.getHeight()
					+ backgroundMid.getHeight() + backgroundBot.getHeight());
		}
	}

	/**
	 * The Help class
	 */
	private class Help extends JLayeredPane
	{
		double scale;
		BufferedImage backgroundTop;
		BufferedImage backgroundMid;
		BufferedImage backgroundBot;
		BufferedImage internalTop;
		BufferedImage internalMid;
		BufferedImage internalBot;

		JLabel titre;
		JLabel text;

		/**
		 * Constructor
		 * 
		 * @param _scale
		 */
		public Help(double _scale)
		{
			scale = _scale;
			try
			{
				backgroundTop = resizeImage(ImageIO.read(new File("res/haut-fond.png")), scale / 2); //$NON-NLS-1$
				backgroundMid = resizeImage2(ImageIO.read(new File("res/mid-fond.png")), 1, //$NON-NLS-1$
						200 * scale / 2);
				backgroundBot = resizeImage(ImageIO.read(new File("res/bas-fond.png")), scale / 2); //$NON-NLS-1$
				internalTop = resizeImage(ImageIO.read(new File("res/haut-interieur.png")), //$NON-NLS-1$
						scale / 2);
				internalMid = resizeImage2(ImageIO.read(new File("res/mid-interne.png")), 1, //$NON-NLS-1$
						50 * scale / 2);
				internalBot = resizeImage(ImageIO.read(new File("res/bas-interieur.png")), //$NON-NLS-1$
						scale / 2);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			titre = new JLabel(Messages.getString("MainView.Help"), SwingConstants.CENTER); //$NON-NLS-1$
			titre.setFont(new Font("Calibri", Font.BOLD, 36)); //$NON-NLS-1$
			titre.setBounds(0, backgroundTop.getHeight(), (int) (scale * 345), (int) (scale * 34));
			titre.setForeground(Color.WHITE);
			this.add(titre);

			text = new JLabel(
					Messages.getString("MainView.HelpText")); //$NON-NLS-1$
			text.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 36))); //$NON-NLS-1$
			text.setBounds(0, internalTop.getHeight(), (int) (scale * 345), (int) (scale * 34 * 8));
			text.setForeground(Color.BLACK);
			this.add(text);

			this.setBounds(0, 0, (backgroundTop.getWidth() * 2), (int) (500 * scale));
			this.setVisible(false);

			this.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent evt)
				{
					MouseClicked(evt);
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			int posY = backgroundTop.getHeight();
			Graphics2D g2 = (Graphics2D) g;
			g2.drawImage(backgroundTop, 0, 0, null);
			g2.drawImage(backgroundMid, 0, posY, null);
			posY += backgroundMid.getHeight();
			g2.drawImage(backgroundBot, 0, posY, null);
			posY = backgroundTop.getHeight() + titre.getHeight();
			int posX = (backgroundTop.getWidth() / 2 - internalTop.getWidth() / 2);
			g2.drawImage(internalTop, posX, posY, null);
			posY += internalTop.getHeight();
			g2.drawImage(internalMid, posX, posY, null);
			posY += internalMid.getHeight();
			g2.drawImage(internalBot, posX, posY, null);
		}

		/**
		 * this method was here only for avoid the user to click on the skymap behind the widows.
		 * 
		 * @param evt
		 */
		private void MouseClicked(java.awt.event.MouseEvent evt)
		{
			// nothing
		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;

			try
			{
				backgroundTop = resizeImage(ImageIO.read(new File("res/haut-fond.png")), scale / 2); //$NON-NLS-1$
				backgroundBot = resizeImage(ImageIO.read(new File("res/bas-fond.png")), scale / 2); //$NON-NLS-1$
				internalTop = resizeImage(ImageIO.read(new File("res/haut-interieur.png")), //$NON-NLS-1$
						scale / 2);

				titre.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 36))); //$NON-NLS-1$
				titre.setBounds(0, backgroundTop.getHeight(), backgroundTop.getWidth(),
						(int) (scale * 35));

				text.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 24))); //$NON-NLS-1$
				text.setBounds((int) (40 * scale),
						backgroundTop.getHeight() + internalTop.getHeight(),
						(internalTop.getWidth()), (int) (scale * 26 * 9));

				internalMid = resizeImage2(ImageIO.read(new File("res/mid-interne.png")), //$NON-NLS-1$
						text.getWidth(), text.getHeight() - 52 * scale);
				backgroundMid = resizeImage2(ImageIO.read(new File("res/mid-fond.png")), //$NON-NLS-1$
						backgroundTop.getWidth(), text.getHeight() + titre.getHeight() * 2.7);
				internalBot = resizeImage(ImageIO.read(new File("res/bas-interieur.png")), //$NON-NLS-1$
						scale / 2);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			this.setBounds(0, 0, (backgroundTop.getWidth()), backgroundTop.getHeight()
					+ backgroundMid.getHeight() + backgroundBot.getHeight());
		}
	}

	/**
	 * The ZoomBar class
	 */
	private class ZoomBar extends JLayeredPane
	{
		double scale;
		int hig;
		JSlider zoomSlider;

		/**
		 * Constructor
		 * 
		 * @param _scale
		 */
		public ZoomBar(double _scale)
		{
			scale = _scale;
			hig = (int) (30 * scale);
			this.setBounds(0, 0, (int) (width() / 2), hig);
			zoomSlider = new JSlider();
			zoomSlider.setBounds(0, 0, (int) (width() / 2), hig);
			zoomSlider.setMinimum(1);
			zoomSlider.setMaximum(40);
			zoomSlider.setValue(zoom);
			zoomSlider.setOpaque(false);
			this.add(zoomSlider);
			this.setVisible(true);
			zoomSlider.addChangeListener(new javax.swing.event.ChangeListener()
			{
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent evt)
				{
					jSlider1StateChanged(evt);
				}
			});
		}

		/**
		 * Called then the value of the slider change This method update the skymap
		 * 
		 * @param evt
		 */
		private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt)
		{
			zoom = zoomSlider.getValue();
			skymap.setZoom(zoom);
			skymap.updateSkyMap();
		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;
			hig = 30;

			this.setBounds(0, 0, (int) (width() / 2 - buttonsPanel.getWidth() / 2 - 70 * scale),
					hig);
			zoomSlider.setBounds(0, 0,
					(int) (width() / 2 - buttonsPanel.getWidth() / 2 - 70 * scale), hig);
		}
	}

	/**
	 * The SearchBar class
	 */
	private class SearchBar extends JLayeredPane
	{
		double scale;
		int hig;
		String l_sSavedSearch = null;
		JTextField searchBarTextField;
		JList<String> listNameOrID;
		ListModel<String> listModelNameOrID;
		ArrayList<CelestialObject> listModelObjects;
		String[] keys = { "!id ", "!ProperName ", "!RA ", "!Dec ", "!Distance ", "!Mag ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				"!ColorIndex " }; //$NON-NLS-1$
		JScrollPane jScrollPane = new JScrollPane();
		ArrayList<CelestialObject> listCelestialObject = new ArrayList<CelestialObject>();

		/**
		 * Constructor
		 * 
		 * @param _scale
		 */
		public SearchBar(double _scale)
		{
			scale = _scale;
			hig = (int) (300 * scale);
			this.setBounds(0, 0, (int) (width() / 2), hig);
			searchBarTextField = new JTextField();
			searchBarTextField.setBounds(0, 0, (int) (width() / 2), hig);
			searchBarTextField.addKeyListener(new java.awt.event.KeyAdapter()
			{
				@Override
				public void keyReleased(java.awt.event.KeyEvent evt)
				{
					try
					{
						searchBarKeyReleased(evt);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			});

			searchBarTextField.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent evt)
				{
					if (l_sSavedSearch != null)
					{
						searchBarTextField.setText(l_sSavedSearch);
						l_sSavedSearch = null;
					}
				}
			});

			listModelNameOrID = new ListModel<String>();
			listModelObjects = new ArrayList<CelestialObject>();
			listNameOrID = new JList<String>();
			listNameOrID.setModel(listModelNameOrID);
			listNameOrID.setBounds(0, 0, 300, 400);
			jScrollPane.setFocusable(false);
			listNameOrID.setFocusable(false);
			jScrollPane.setViewportView(listNameOrID);

			listNameOrID.addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(java.awt.event.MouseEvent evt)
				{
					listNameOrIDMouseClicked(evt);
				}
			});

			this.add(searchBarTextField);
			this.add(jScrollPane);
		}

		/**
		 * used then the user select an element of the list.
		 * 
		 * @param evt
		 */
		private void listNameOrIDMouseClicked(java.awt.event.MouseEvent evt)
		{

			jScrollPane.setVisible(false);
			String[] searchBarText = searchBarTextField.getText().split("[; ]"); //$NON-NLS-1$
			String[] searchFeatures = searchBarTextField.getText().split(";"); //$NON-NLS-1$
			String searchFeature = searchFeatures[searchFeatures.length - 1];

			// log.info("\n>" + searchFeature + "<->" + searchFeature.split(" ").length + "\n");
			if (searchFeature.split(" ").length > 1) //$NON-NLS-1$
			{
				int index = listNameOrID.getSelectedIndex();
				CelestialObject celObjt = listModelObjects.get(index);
				updateInfo(celObjt);
				skymap.setCelestialObjectPointed(celObjt);

				degCompass = celObjt.getAzimuth();
				angInclinometer = celObjt.getHeight();

				skymap.updateSkyMap();
				l_sSavedSearch = searchBarTextField.getText();
				searchBarTextField.setText(listNameOrID.getSelectedValue().toString());
				// skymap.requestFocus();
				skymap.transferFocusBackward();
				return;
			}

			String regex = searchBarText[searchBarText.length - 1] + "$"; //$NON-NLS-1$
			searchBarTextField.setText(searchBarTextField.getText().replaceFirst(regex,
					listNameOrID.getSelectedValue().toString()));

		}

		/**
		 * search in the database the stars corresponding with the textField.
		 * 
		 * @param evt
		 */
		private void searchBarKeyReleased(java.awt.event.KeyEvent evt)
		{
			// listCelestialObject.clear();
			if (listModelNameOrID.getSize() > 0)
				listModelNameOrID.removeAll();
			listModelObjects.clear();

			boolean canQueryDB = true;
			String[] searchFeatures = searchBarTextField.getText().split(";"); //$NON-NLS-1$
			for (String searchFeature : searchFeatures)
			{
				for (String key : keys)
				{
					if (key.toLowerCase().startsWith(searchFeature.toLowerCase()))
					{
						listModelNameOrID.setElement(key);
					}
				}
				if (searchFeature.split(" ").length <= 1) //$NON-NLS-1$
					canQueryDB = false;
			}
			if (canQueryDB)
			{
				try
				{
					if (pic == null)
						listCelestialObject = db.starsForText(searchBarTextField.getText(),
								Calendar.getInstance(), 47.039448, 6.799734);
					else
						listCelestialObject = db.starsForText(searchBarTextField.getText(),
								Calendar.getInstance(), pic.getLatitude(), pic.getLongitude());

					if (listCelestialObject.size() != 0)
					{
						for (CelestialObject celestialObject : listCelestialObject)
						{
							if (celestialObject.getProperName() != null)
								listModelNameOrID.setElement(celestialObject.getProperName());
							else
								listModelNameOrID
										.setElement(String.valueOf(celestialObject.getId()));
							listModelObjects.add(celestialObject);
						}
					}
					else
						listModelNameOrID
								.setElement(Messages.getString("MainView.NoResult")); //$NON-NLS-1$

				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}

			if (listModelNameOrID.getSize() > 0)
			{
				int min = (listModelNameOrID.getSize() < 5) ? listModelNameOrID.getSize() * 21
						: (int) (200 * scale);
				jScrollPane.setBounds(0, 20, searchBarTextField.getWidth(), min);
				jScrollPane.setVisible(true);

			}
			else
				jScrollPane.setVisible(false);

		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;
			hig = 20;
			this.setBounds(0, 0,
					(int) (width() / 2 - buttonsPanel.getWidth() / 2 - 70 * scale - compassPanel
							.getWidth()), hig + (int) (400 * scale));
			searchBarTextField.setBounds(0, 0, (int) (width() / 2 - buttonsPanel.getWidth() / 2
					- 70 * scale - compassPanel.getWidth()), hig);
			int min = (listModelNameOrID.getSize() < 5) ? listModelNameOrID.getSize() * 21
					: (int) (200 * scale);
			jScrollPane.setBounds(0, 20, searchBarTextField.getWidth(), min);
		}
	}

	/**
	 * The Compass class
	 */
	private class Compass extends JLayeredPane
	{
		double scale = 1;
		double redAngle = 0;
		double greenAngle = 0;
		BufferedImage background;
		JLabel coordinate;
		Needle redNeedle;
		Needle greenNeedle;

		/**
		 * Constructor
		 * 
		 * @param _scale
		 *            : the scalar for resize the components.
		 */
		public Compass(double _scale)
		{
			scale = _scale;
			try
			{
				background = resizeImage(ImageIO.read(new File("res/backgroundCompass.png")), scale); //$NON-NLS-1$
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			redNeedle = new Needle("res/aiguille_rouge.png", scale); //$NON-NLS-1$
			greenNeedle = new Needle("res/aiguille_vert.png", scale); //$NON-NLS-1$

			redNeedle.setBackground(this.getBackground());
			redNeedle.setBounds(0, 0, (int) (scale * 345), (int) (scale * 304));
			redNeedle.setOpaque(false);
			this.add(redNeedle, new Integer(1));

			greenNeedle.setBackground(this.getBackground());
			greenNeedle.setBounds(0, 0, (int) (scale * 345), (int) (scale * 304));
			greenNeedle.setOpaque(false);
			this.add(greenNeedle, new Integer(2));

			this.setBounds(0, 0, (int) (scale * 345), (int) (scale * 350));
			coordinate = new JLabel("-10:2'13'' N", SwingConstants.CENTER); //$NON-NLS-1$
			coordinate.setFont(new Font("Calibri", Font.BOLD, 36)); //$NON-NLS-1$
			coordinate.setBounds(0, (int) (scale * 310), (int) (scale * 345), (int) (scale * 34));
			coordinate.setForeground(Color.WHITE);
			this.add(coordinate, new Integer(3));
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(background.getWidth(), background.getHeight());
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.drawImage(background, 0, 0, null);
		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;
			try
			{
				background = resizeImage(ImageIO.read(new File("res/backgroundCompass.png")), scale); //$NON-NLS-1$
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			redNeedle.scale(scale);
			redNeedle.rotate(redAngle);
			redNeedle.setBounds(0, 0, (int) (scale * 345), (int) (scale * 304));
			greenNeedle.scale(scale);
			greenNeedle.rotate(greenAngle);
			greenNeedle.setBounds(0, 0, (int) (scale * 345), (int) (scale * 304));
			try
			{
				coordinate.setText(String.valueOf(redAngle % 360));
			}
			catch (Exception e)
			{
				log.warning(e.toString());
			}
			this.setBounds(0, 0, (int) (scale * 345), (int) (scale * 350));
			coordinate.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 36))); //$NON-NLS-1$
			coordinate.setBounds(0, (int) (scale * 310), (int) (scale * 345), (int) (scale * 35));
		}

		/**
		 * Used for update the red needle angle.
		 * 
		 * @param _redAngle
		 *            : It's the new angle for the red needle
		 */
		public void setRedNeedle(double _angle)
		{
			_angle = Math.toRadians(_angle);
			redAngle = _angle;
		}

		/**
		 * Used for update the green needle angle.
		 * 
		 * @param _redAngle
		 *            : It's the new angle for the green needle
		 */
		public void setGreenNeedle(double _angle)
		{
			_angle = Math.toRadians(_angle);
			greenAngle = _angle;
		}

		private class Needle extends JPanel
		{
			BufferedImage needleImage;
			double angle = 0;
			double scale = 1;
			String adresseImage;

			public Needle(String _adresseImage, double _scale)
			{
				scale = _scale;
				adresseImage = _adresseImage;
				try
				{
					needleImage = resizeImage(ImageIO.read(new File(adresseImage)), scale);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			public void scale(double _scale)
			{
				if (scale != _scale)
				{
					scale = _scale;
					try
					{
						needleImage = resizeImage(ImageIO.read(new File(adresseImage)), scale);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}

			public void rotate(double _angle)
			{
				angle = _angle;
				repaint();
			}

			@Override
			public Dimension getPreferredSize()
			{
				return new Dimension(needleImage.getWidth(), needleImage.getHeight());
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.rotate(angle, needleImage.getWidth() / 2, needleImage.getHeight() / 2);
				g2.drawImage(needleImage, 0, 0, null);
			}
		}
	}

	/**
	 * The Inclinometer class
	 */
	private class Inclinometer extends JLayeredPane
	{
		double scale = 1;
		double redAngle = 0;
		double greenAngle = 0;
		BufferedImage background;
		JLabel coordinate;
		Needle redNeedle;
		Needle greenNeedle;

		/**
		 * Inclinometer Constructor
		 * 
		 * @param _scale
		 *            : It's the scale for resize the components
		 */
		public Inclinometer(double _scale)
		{
			scale = _scale;
			try
			{
				background = resizeImage(ImageIO.read(new File("res/backgroundInclinometer.png")), //$NON-NLS-1$
						scale);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			redNeedle = new Needle("res/aiguille_rouge_inclinometer.png", scale); //$NON-NLS-1$
			greenNeedle = new Needle("res/aiguille_vert_inclinometer.png", scale); //$NON-NLS-1$

			redNeedle.setBackground(this.getBackground());
			redNeedle.setBounds(0, 0, (int) (scale * 186), (int) (scale * 258));
			redNeedle.setOpaque(false);
			this.add(redNeedle, new Integer(1));

			greenNeedle.setBackground(this.getBackground());
			greenNeedle.setBounds(0, 0, (int) (scale * 186), (int) (scale * 258));
			greenNeedle.setOpaque(false);
			this.add(greenNeedle, new Integer(2));

			this.setBounds(0, 0, (int) (scale * 186), (int) (scale * 324));
			;
			coordinate = new JLabel("-10:2'13'' N", SwingConstants.CENTER); //$NON-NLS-1$
			coordinate.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 36))); //$NON-NLS-1$
			coordinate.setBounds(0, (int) (scale * 258), (int) (scale * 186), (int) (scale * 35));
			coordinate.setForeground(Color.WHITE);

			this.add(coordinate, new Integer(3));
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(background.getWidth(), background.getHeight());
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.drawImage(background, 0, 0, null);
		}

		/**
		 * update the scale variable and resize the components
		 * 
		 * @param _scale
		 *            : the scalar
		 */
		public void update(double _scale)
		{
			scale = _scale;
			try
			{
				background = resizeImage(ImageIO.read(new File("res/backgroundInclinometer.png")), //$NON-NLS-1$
						scale);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			redNeedle.scale(scale);
			redNeedle.rotate(redAngle);
			redNeedle.setBounds(0, 0, (int) (scale * 186), (int) (scale * 258));

			greenNeedle.scale(scale);
			greenNeedle.rotate(greenAngle);
			greenNeedle.setBounds(0, 0, (int) (scale * 186), (int) (scale * 258));
			try
			{
				coordinate.setText(String.valueOf(redAngle % 360));
			}
			catch (Exception e)
			{
				log.warning(e.toString());
			}
			this.setBounds(0, 0, (int) (scale * 186), (int) (scale * 324));
			coordinate.setFont(new Font("Calibri", Font.BOLD, (int) (scale * 36))); //$NON-NLS-1$
			coordinate.setBounds(0, (int) (scale * 258), (int) (scale * 186), (int) (scale * 35));
		}

		/**
		 * Used for update the red needle angle.
		 * 
		 * @param _redAngle
		 *            : It's the new angle for the red needle
		 */
		public void setRedNeedle(double _redAngle)
		{
			redAngle = _redAngle;
		}

		/**
		 * Used for update the green needle angle.
		 * 
		 * @param _redAngle
		 *            : It's the new angle for the green needle
		 */
		public void setGreenNeedle(double _greenAngle)
		{
			greenAngle = _greenAngle;
		}

		/**
		 * private class Needle
		 */
		private class Needle extends JPanel
		{
			BufferedImage needleImage;
			double angle = 0;
			double scale = 1;
			String adresseImage;

			/**
			 * private class Needle
			 * 
			 * @param _adressImage
			 *            : the path of the image
			 * @param _scale
			 *            : the scale for resize the image
			 */
			public Needle(String _adresseImage, double _scale)
			{
				scale = _scale;
				adresseImage = _adresseImage;
				try
				{
					needleImage = resizeImage(ImageIO.read(new File(adresseImage)), scale);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			/**
			 * used of resize the components
			 * 
			 * @param _scale
			 *            : the scalar with the components are resized
			 */
			public void scale(double _scale)
			{
				if (scale != _scale)
				{
					scale = _scale;
					try
					{
						needleImage = resizeImage(ImageIO.read(new File(adresseImage)), scale);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}

			/**
			 * used for rotate the needles
			 * 
			 * @param _angle
			 *            : the new angle of the needle.
			 */
			public void rotate(double _angle)
			{
				angle = Math.toRadians(_angle);

				if (Math.sin(angle) < 0 && Math.cos(angle) < 0)
					angle = angle + 2 * (3 * Math.PI / 2 - angle);
				if (Math.sin(angle) > 0 && Math.cos(angle) < 0)
					angle = angle - 2 * (angle - Math.PI / 2);

				repaint();
			}

			@Override
			public Dimension getPreferredSize()
			{
				return new Dimension(needleImage.getWidth(), needleImage.getHeight());
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.rotate(-angle, scale * 5, needleImage.getHeight() / 2);
				g2.drawImage(needleImage, 0, 0, null);
			}
		}
	}
}
