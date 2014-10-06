package PoiServices;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import Configuration.ControlPanelInformation;

public class ControlPanel extends JFrame implements WindowListener, Observer {
	private static final long serialVersionUID = -156889830016809349L;
	private final Application application;
	
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel panel1 = new JPanel();
	private final JPanel panel2 = new JPanel();
	private final JPanel panel1_north = new JPanel();
	private final JPanel panel2_north = new JPanel();
	
	private final JTextArea area1 = new JTextArea(10,10);
	private final JTextArea area2 = new JTextArea(10,10);
	
	private final JButton refreshTab1 = new JButton("User get/set/get+set");
	private final JButton searchLocation = new JButton("Search Location");
	private final JButton refreshTab2 = new JButton("Update");	
	private final JButton deleteUser = new JButton("Delete User");	
	
	private final JTextField textFieldX = new JTextField("0",5);
	private final JTextField textFieldY = new JTextField("0",5);
	private final JTextField textFieldR = new JTextField("3",5);
	private final int T;		// in ms
	
	private DefaultListModel<String> dlm = new DefaultListModel<>();
	private JList<String> userList = new JList<String>(dlm);
	
	public ControlPanel(ControlPanelInformation controlPanelInformation, Application a) {
		super("Control Panel");
		this.application = a;
		this.T = controlPanelInformation.T;
		
		setSize(500,500);
		setLayout(new BorderLayout());
		addWindowListener(this);
		add(tabs);		
		
		tabs.addTab("Tab 1", null, panel1, "Does nothing");
		tabs.setMnemonicAt(0, KeyEvent.VK_1);
		tabs.addTab("Tab 2", null, panel2, "Does twice as much nothing");
		tabs.setMnemonicAt(1, KeyEvent.VK_2);
		
		panel1.setLayout(new BorderLayout());
		panel2.setLayout(new BorderLayout());
				
		panel1.add(area1, BorderLayout.CENTER);		
		panel2.add(area2, BorderLayout.CENTER);		
		panel1.add(panel1_north, BorderLayout.NORTH);
		panel2.add(panel2_north, BorderLayout.NORTH);
		panel1_north.add(refreshTab1, BorderLayout.NORTH);
		panel1_north.add(new JLabel("X:"), BorderLayout.NORTH);
		panel1_north.add(textFieldX, BorderLayout.NORTH);		
		panel1_north.add(new JLabel("Y:"), BorderLayout.NORTH);
		panel1_north.add(textFieldY, BorderLayout.NORTH);
		panel1_north.add(new JLabel("R:"), BorderLayout.NORTH);
		panel1_north.add(textFieldR, BorderLayout.NORTH);
		panel1_north.add(searchLocation, BorderLayout.NORTH);
		panel2_north.add(refreshTab2, BorderLayout.NORTH);
		panel2_north.add(deleteUser, BorderLayout.NORTH);
		panel2.add(userList, BorderLayout.WEST);
		
		dlm.addElement("     ---- Users ----     ");
		userList.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (userList.getSelectedIndex() > 0) {
					String u = userList.getSelectedValue().toString();
					if (u != null) {
						area2.setText(application.database.GetLocationStatisticsByUser(u));
					} else {
						area2.setText("No data.");
					}
				}
			}
		});
		
		refreshTab1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				area1.setText(application.database.CalculateTab1Statistics(application.databaseInformation.T));
			
			}			
		});
		
		searchLocation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int x = Integer.parseInt(textFieldX.getText());
				int y = Integer.parseInt(textFieldY.getText());
				int R = Integer.parseInt(textFieldR.getText());
				area1.setText(application.database.GetLocationStatistics(x, y, R));				
			}
		});
		
		refreshTab2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<String> usernames = application.database.GetUserNames();
				dlm.clear();
				dlm.addElement("     ---- Users ----     ");
				for (String s : usernames) {
					dlm.addElement(s);
				}
				area2.setText(application.database.CalculateTab2Statistics());				
			}
		});
		
		deleteUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (userList.getSelectedIndex() > 0) {
					String u = userList.getSelectedValue().toString();
					if (u != null) {
						application.database.DeleteUser(u);
						int index = userList.getSelectedIndex();
						dlm.remove(index);
					} 
				}
			}
		});
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		new GetSetWorker(T).execute();
	}
	
	void updateGUI(String s) {
		area1.setText(s);	
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		application.stop();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		System.out.println("Instant Update GUI.");
		area1.setText(application.database.CalculateTab1Statistics(application.databaseInformation.T));
	}
	
	
	class GetSetWorker extends SwingWorker<Void, Integer>
	{
		private final int T;
		private String result;
		
	    public GetSetWorker(int t) {
			super();
			T = t;
			System.out.println("A new worker has been created with T = " + T);
		}

		protected Void doInBackground() throws Exception {
	        // Do a time-consuming task.
			Thread.sleep(T);
			result = application.database.CalculateTab1Statistics(application.databaseInformation.T);
			return null;
	    }

	    protected void done() {
	    	updateGUI(result);
	    	new GetSetWorker(T).execute();
	    }
	}
	
}
