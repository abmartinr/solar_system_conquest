package clienteyservidor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class InterfazServidor extends JFrame implements ActionListener, WindowListener {
	
	private static final long serialVersionUID = 1L;
	private JButton stopStart;
	private JTextArea chat, event;
	private JTextField tPortNumber;
	private Servidor server;
	
	InterfazServidor(int port) {
		super("Servidor");
		server = null;
		
		JPanel north = new JPanel();
		north.add(new JLabel("Puerto: "));
		tPortNumber = new JTextField("  " + port);
		north.add(tPortNumber);
		
		stopStart = new JButton("Iniciar");
		stopStart.addActionListener(this);
		north.add(stopStart);
		add(north, BorderLayout.NORTH);
		
		
		JPanel center = new JPanel(new GridLayout(2,1));
		chat = new JTextArea(80,80);
		chat.setEditable(false);
		appendRoom("Sala de chat.\n");
		center.add(new JScrollPane(chat));
		event = new JTextArea(80,80);
		event.setEditable(false);
		appendEvent("Log de eventos.\n");
		center.add(new JScrollPane(event));	
		add(center);
		
		addWindowListener(this);
		setSize(400, 600);
		setVisible(true);
	}		

	void appendRoom(String str) {
		chat.append(str);
		chat.setCaretPosition(chat.getText().length() - 1);
	}
	void appendEvent(String str) {
		event.append(str);
		event.setCaretPosition(chat.getText().length() - 1);
		
	}

	public void actionPerformed(ActionEvent e) {

		if(server != null) {
			server.stop();
			server = null;
			tPortNumber.setEditable(true);
			stopStart.setText("Iniciar");
			return;
		}

		int port;
		try {
			port = Integer.parseInt(tPortNumber.getText().trim());
		}
		catch(Exception er) {
			appendEvent("Puerto inválido");
			return;
		}

		server = new Servidor(port, this);

		new ServerRunning().start();
		stopStart.setText("Parar");
		tPortNumber.setEditable(false);
	}
	

	public static void main(String[] arg) {

		new InterfazServidor(1500);
	}

	public void windowClosing(WindowEvent e) {
		
		if(server != null) {
			try {
				server.stop();			
			}
			catch(Exception eClose) {
			}
			server = null;
		}

		dispose();
		System.exit(0);
	}

	public void windowClosed(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}


	class ServerRunning extends Thread {
		public void run() {
			server.start();        

			stopStart.setText("Start");
			tPortNumber.setEditable(true);
			appendEvent("El servidor se ha roto, buena eh!\n");
			server = null;
		}
	}

}

