package clienteyservidor;

import javax.swing.*;


import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

public class InterfazCliente extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JLabel label;
	private JTextField tf;
	JTextField tCdadConstruir;
	private JTextField tCdadAtacar;
	private JTextField tfServer, tfPort, messageTextField;
	private JButton login, logout, whoIsIn, btnConstruir, btnAtacar;
	private JTextArea ta;
	private boolean connected;
	private Cliente client;
	private int defaultPort;
	private String defaultHost;
	public JComboBox cmbPlanetas, cmbTropas, cmbAtacarPlanetas;
	public JLabel lSoldado, lDefensor, lObrero, lRecRestantes, lRecDisponibles;

	InterfazCliente(String host, int port) {

		super("Conquista Sistema Solar");
		defaultPort = port;
		defaultHost = host;
		cmbPlanetas = new JComboBox<>();
		cmbTropas = new JComboBox<>();
		cmbAtacarPlanetas = new JComboBox<>();
		JPanel northPanel = new JPanel(new GridLayout(3, 1));

		JPanel info = new JPanel(new GridLayout(1, 5, 1, 3));

		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		// panel con la info para conectar
		info.add(new JLabel("Servidor:  "));
		info.add(tfServer);
		info.add(new JLabel("Puerto:  "));
		info.add(tfPort);
		info.add(new JLabel("Nick: "));
		tf = new JTextField("Anónimo");
		info.add(tf);
		northPanel.add(info);
		add(northPanel, BorderLayout.NORTH);
		// panel que contiene dos paneles, el del juego y el chat
		JPanel panelCentral = new JPanel(new GridLayout(2, 1));
		JPanel jpPlanetas1 = new JPanel(new GridLayout(2, 1));
		JPanel panelJuego = new JPanel(new GridLayout(5, 1));
		JLabel h1 = new JLabel("Mis Planetas");
		h1.setForeground(Color.BLUE);
		h1.setHorizontalAlignment(SwingConstants.CENTER);
		jpPlanetas1.add(h1);
		jpPlanetas1.add(cmbPlanetas);

		JPanel jpPlanetas2 = new JPanel(new GridLayout(3, 2));

		lSoldado = new JLabel();
		lDefensor = new JLabel();
		lObrero = new JLabel();
		lRecRestantes = new JLabel();
		lRecDisponibles = new JLabel();

		jpPlanetas2.add(new JLabel("Soldado (20 R):"));
		jpPlanetas2.add(lSoldado);
		jpPlanetas2.add(new JLabel("Recursos Restantes:"));
		jpPlanetas2.add(lRecRestantes);
		jpPlanetas2.add(new JLabel("Defensor(10 R):"));
		jpPlanetas2.add(lDefensor);
		jpPlanetas2.add(new JLabel("Recursos Disponibles:"));
		jpPlanetas2.add(lRecDisponibles);
		jpPlanetas2.add(new JLabel("Obrero(5 R):"));
		jpPlanetas2.add(lObrero);

		panelJuego.add(jpPlanetas1);
		panelJuego.add(jpPlanetas2);

		tCdadConstruir = new JTextField();
		tCdadAtacar = new JTextField();
		btnConstruir = new JButton("Construir");
		btnConstruir.addActionListener(this);
		btnAtacar = new JButton("Atacar");
		btnAtacar.addActionListener(this);

		JPanel jpAcciones = new JPanel(new GridLayout(1, 1));
		h1 = new JLabel("Acciones");
		h1.setForeground(Color.BLUE);
		h1.setHorizontalAlignment(SwingConstants.CENTER);
		jpAcciones.add(h1);

		JPanel jpAcciones2 = new JPanel(new GridLayout(2, 3));
		jpAcciones2.add(tCdadConstruir);
		jpAcciones2.add(cmbTropas);
		jpAcciones2.add(btnConstruir);

		// Segunda fila
		jpAcciones2.add(tCdadAtacar);
		jpAcciones2.add(cmbAtacarPlanetas);
		jpAcciones2.add(btnAtacar);

		panelJuego.add(jpAcciones);
		panelJuego.add(jpAcciones2);

		panelCentral.add(panelJuego);

		ta = new JTextArea("Bienvenido al chat\n", 80, 80);

		JPanel panelChat = new JPanel(new GridLayout(2, 1));

		JScrollPane js = new JScrollPane(ta);

		js.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		js.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		JPanel chat = new JPanel(new GridLayout(1, 1));
		chat.add(js);

		ta.setEditable(false);

		panelChat.add(chat);

		JPanel messagePanel = new JPanel();
		BoxLayout bl = new BoxLayout(messagePanel, BoxLayout.PAGE_AXIS);

		messageTextField = new JTextField("Introduce aquí tu mensaje");
		messageTextField.setColumns(50);

		messagePanel.add(messageTextField);
		panelChat.add(messagePanel);

		panelCentral.add(panelChat);

		add(panelCentral, BorderLayout.CENTER);

		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false);
		whoIsIn = new JButton("Who is in");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false);

		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		tf.setText("Anónimo");

		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);

		tfServer.setEditable(false);
		tfPort.setEditable(false);

		tf.removeActionListener(this);
		connected = false;
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == logout) {
			client.sendMessage(new Objeto(Objeto.LOGOUT, ""));

			return;
		}

		if (o == whoIsIn) {
			client.sendMessage(new Objeto(Objeto.WHOISIN, ""));
			return;
		}
		
		if (o == btnAtacar){
			
			if (!tCdadAtacar.getText().equals("")) {
				client.sendMessage(new Objeto(4, client.getPlanetaSeleccionado() + " " + tCdadAtacar.getText() + " " + (String)cmbAtacarPlanetas.getSelectedItem()));
			}else{
				client.display("Has de poner una cantidad de soldados a enviar");
			}
			return;	
		}
		

		if (o == btnConstruir) {

			if (!tCdadConstruir.getText().equals("")) {

				int tipoTropa = 0;
				switch ((String) cmbTropas.getSelectedItem()) {
				case "Soldado":
					tipoTropa = 1;
					break;
				case "Defensor":
					tipoTropa = 2;
					break;
				case "Obrero":
					tipoTropa = 3;
					break;
				}
				client.sendMessage(new Objeto(Objeto.ACTION, "CONSTRUIR "
						+ tCdadConstruir.getText() + " " + tipoTropa + " "
						+ client.getPlanetaSeleccionado()));
				client.actualizaDatosDePlaneta();
			} else {
				client.display("Has de poner una cantidad a construir");
			}
			return;
		}

		if (connected) {

			client.sendMessage(new Objeto(Objeto.MESSAGE, messageTextField
					.getText()));
			messageTextField.setText("");
			return;
		}

		if (o == login) {

			String username = tf.getText().trim();

			if (username.length() == 0)
				return;

			String server = tfServer.getText().trim();
			if (server.length() == 0)
				return;

			String portNumber = tfPort.getText().trim();
			if (portNumber.length() == 0)
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return;
			}

			client = new Cliente(server, port, username, this);

			if (!client.start())
				return;
			connected = true;

			login.setEnabled(false);
			tf.setEnabled(false);

			logout.setEnabled(true);
			whoIsIn.setEnabled(true);

			tfServer.setEditable(false);
			tfPort.setEditable(false);

			messageTextField.addActionListener(this);

		}

	}

	public static void main(String[] args) {
		new InterfazCliente("localhost", 1500);
	}

}
