package Client;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;

import java.awt.Color;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Frame extends JFrame {

	private JLayeredPane contentPane;
	private JTextField txtTypeToSearch;
	private JButton btnNewButton;
	private JButton btnNewButton_1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) 
	{	
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() 
			{
				try {
					Frame frame = new Frame(args);
					frame.setVisible(true);	
			     	} 
				catch (Exception e) 
				    {
					e.printStackTrace();
				    }
			}
		});
	}

	public Frame(String[] args) {
try {
		Client client = new Client(args[0],Integer.parseInt(args[1]));
		setBackground(Color.DARK_GRAY);
		setTitle("Dictionary");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 559, 425);
		contentPane = new JLayeredPane();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
         JLabel lblNewLabel = new JLabel("");
		
		contentPane.setLayer(lblNewLabel, 0);
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel.setBackground(Color.WHITE);
		lblNewLabel.setForeground(Color.BLACK);
		lblNewLabel.setBounds(6, 119, 547, 278);
		lblNewLabel.setOpaque(true);
		contentPane.add(lblNewLabel);
		
		txtTypeToSearch = new JTextField();
		txtTypeToSearch.setToolTipText("");
		txtTypeToSearch.setText("type to search");
		txtTypeToSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) 
			{
			//System.out.println(textField.getText());
             //System.out.println(client.Search(textField.getText().toLowerCase()));
             lblNewLabel.setText("<html>"+"<span style='font-size:20px'>"+txtTypeToSearch.getText()+"</span>"+"<br>"+client.Transfer("Search,"+txtTypeToSearch.getText().toLowerCase())+"</html>");
             
			}
		});
		
		
		txtTypeToSearch.setBounds(6, 70, 547, 35);
		contentPane.add(txtTypeToSearch);
		txtTypeToSearch.setColumns(10);
		
		btnNewButton = new JButton("Delete");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				String words = JOptionPane.showInputDialog("type the word you want to delete");
                if(words!=null)
                { if (!words.chars().allMatch(Character::isLetter)||words.isEmpty())
					    JOptionPane.showMessageDialog(null,"Word can't be empty or contain invalid characters!");

				    else 
					{
					words = words.toLowerCase();
					String Message = client.Transfer("Delete,"+words);
					JOptionPane.showMessageDialog(null,Message);
					}
                 }
			}
		});
		btnNewButton.setBounds(396, 40, 148, 29);
		contentPane.add(btnNewButton);
		
		btnNewButton_1 = new JButton("Add");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				SubFrame1 subframe = new SubFrame1(client);
				subframe.NewWindow(client);
				
			}
		});
		btnNewButton_1.setBounds(22, 40, 148, 29);
		contentPane.add(btnNewButton_1);
		
    }
   catch(Exception e)
    {
	   JOptionPane.showMessageDialog(null,"Illegal input arguments!");
       System.exit(NORMAL);
    }


	}
}
