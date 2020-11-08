package Client;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;

public class SubFrame1 extends JFrame {

	private JPanel contentPane;
	private JTextField textField;
	private JTextPane textPane;
	
	
	
	/**
	 * Launch the application.
	 */
	public  void NewWindow(Client client) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					 
					SubFrame1 frame = new SubFrame1(client);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * @param client 
	 */
	public SubFrame1(Client client) {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 289, 286);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		textPane = new JTextPane();
		textPane.setBounds(6, 44, 276, 155);
		contentPane.add(textPane);
		textField = new JTextField();
		textField.setBounds(6, 6, 276, 26);
		contentPane.add(textField);
		textField.setColumns(10);
		
		JButton btnSubmit = new JButton("Submit");
		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				if(textField.getText().trim().isEmpty()||textPane.getText().trim().isEmpty())
				{
					JOptionPane.showMessageDialog(null,"Can't leave textfield blank!");
					
				}
				else if(!textField.getText().chars().allMatch(Character::isLetter))
				{
					JOptionPane.showMessageDialog(null,"Word can't contain invalid characters!");
				}
				else
				{
					String text = textPane.getText().replaceAll("[\n,]", " ");
					String Message = client.Transfer( "Add,"+textField.getText().toLowerCase()+","+text);
					JOptionPane.showMessageDialog(null,Message);
					if(Message.equals("Operation Success!"))
				            dispose();
				}
			}
		});
		btnSubmit.setBounds(84, 211, 117, 29);
		contentPane.add(btnSubmit);
	}
}
